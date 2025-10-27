package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class BookletJpaRepositoryAdapterTest(
    @Autowired private val bookletJpaRepositoryAdapter: BookletJpaRepositoryAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired private val bookletJpaRepository: BookletJpaRepository
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        bookletJpaRepository.deleteAll()
        accountStateTestAdapter.clear()
    }

    @Test
    fun `save should persist account and attach to user`() {
        val booklet = Booklet(labelAccount = "acct-test-save", amount = Amount(100L), owner = user)
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull
        val userId = user!!.id.value ?: throw AssertionError("user id is null")
        val persisted = bookletJpaRepository.findByOwnerAndLabelWithSheets(userId, booklet.label)
        assertThat(persisted).isNotNull
        assertThat(persisted!!.label).isEqualTo(booklet.label)
    }

    @Test
    fun `editFromAnother should return null when booklet id is null and update when present`() {
        val booklet = Booklet(labelAccount = "acct-edit", amount = Amount(50L), owner = user)
        // id null -> should return null
        val resNull = bookletJpaRepositoryAdapter.editFromAnother(booklet)
        assertThat(resNull).isNull()

        // now persist then edit
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull
        val toEdit = Booklet(amount = fr.sacane.jmanager.domain.models.Amount(999L), labelAccount = saved!!.label, owner = user, id = saved.id)
        val edited = bookletJpaRepositoryAdapter.editFromAnother(toEdit)
        assertThat(edited).isNotNull
        assertThat(edited!!.amount).isEqualTo(toEdit.amount)
    }

    @Test
    fun `upsert should persist transactions linked to account`() {
        val booklet = Booklet(labelAccount = "acct-upsert", amount = Amount(10L), owner = user)
        val up = bookletJpaRepositoryAdapter.upsert(booklet)
        assertThat(up).isNotNull
        val uid = user!!.id.value ?: throw AssertionError("user id is null")
        val persisted = bookletJpaRepository.findByOwnerAndLabelWithSheets(uid, booklet.label)
        assertThat(persisted).isNotNull
    }
}
