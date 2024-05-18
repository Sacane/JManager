package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class TagRepositoryAdapter(
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository,
    private val userPostgresRepository: UserPostgresRepository
): TagRepository {
    @Transactional
    override fun save(userId: UserId, tag: Tag): Tag? {
        val user = userPostgresRepository.findByIdWithTags(userId.id) ?: return null
        val tag1 = tag.toPersonalTag()
        val saved = tagPersonalPostgresRepository.save(tag1)
        user.addTag(tag1)
        return saved.toDomain()
    }
    @Transactional
    override fun getAll(userId: UserId)
    : List<Tag> {
        if(userId.id == null) return emptyList()
        return userId.id?.let {id ->
            tagPersonalPostgresRepository.findAllByOwnerId(id)
                .map { it.toDomain() }.plus(
                    defaultTagPostgresRepository.findAll().map { it.toDomain() }
                )
        } ?: emptyList()
    }
    @Transactional
    override fun deleteByLabel(label: String) {
        defaultTagPostgresRepository.deleteByName(label)
    }
    @Transactional
    override fun getAllDefault(userId: UserId): List<Tag> {
        val defaults = defaultTagPostgresRepository.findAll()
        val personal = userId.id?.let { tagPersonalPostgresRepository.findAllByOwnerId(it) } ?: emptyList()
        return defaults.map { it.toDomain() }.plus(personal.map { it.toDomain() })
    }
    @Transactional
    override fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean
    = userId.id?.let { tagPersonalPostgresRepository.existsTagByNameAndOwnerId(tag.label, it) } ?: false

    @Transactional
    override fun saveAll(defaultTags: List<Tag>) {
        defaultTagPostgresRepository.saveAll(defaultTags.map { it.asResource() as DefaultTagResource })
    }
    @Transactional
    override fun existsDefault(): Boolean {
        return defaultTagPostgresRepository.findAll().count() > 0
    }
    @Transactional
    override fun deleteById(tagId: Long) {
        defaultTagPostgresRepository.deleteById(tagId)
    }

}
