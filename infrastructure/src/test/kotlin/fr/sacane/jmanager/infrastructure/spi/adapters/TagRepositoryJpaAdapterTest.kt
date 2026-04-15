package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class TagRepositoryJpaAdapterTest(
    @Autowired private val tagRepositoryJpaAdapter: TagRepositoryJpaAdapter,
    @Autowired private val tagStateTestAdapter: TagStateTestAdapter,
    @Autowired private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    @Autowired private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear(){
        tagPersonalPostgresRepository.deleteAll()
        defaultTagPostgresRepository.deleteAll()
        tagStateTestAdapter.clear()
    }

    @Test
    fun `save should persist personal tag and attach to user`(){
        val t = Tag("mytag")
        val saved = tagRepositoryJpaAdapter.save(user!!.id, t)
        assertThat(saved).isNotNull
        val uid = user!!.id.value ?: throw AssertionError("user id is null")
        val persisted = tagPersonalPostgresRepository.findByNameAndOwnerId(t.label, uid)
        assertThat(persisted).isNotNull
    }

    @Test
    fun `getAll should return defaults plus personal`(){
        defaultTagPostgresRepository.saveAll(listOf(Tag("d1", isDefault = true).asResource() as fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource))
        val personal = Tag("p1")
        tagRepositoryJpaAdapter.save(user!!.id, personal)

        val all = tagRepositoryJpaAdapter.getAll(user!!.id)
        assertThat(all.map { it.label }).contains("d1", "p1")
    }
}
