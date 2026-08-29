package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
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
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val bookletJpaRepository: BookletJpaRepository,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        bookletJpaRepository.deleteAll()
        bookletStateTestAdapter.clear()
    }

    @Test
    fun `save should persist booklet and attach to user`() {
        val booklet = Booklet(label = "acct-test-save", amount = Amount(100L), owner = user)
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull
        val userId = user!!.id.value ?: throw AssertionError("user id is null")
        val persisted = bookletJpaRepository.findByOwnerAndLabelWithTransactions(userId, booklet.label)
        assertThat(persisted).isNotNull
        assertThat(persisted!!.label).isEqualTo(booklet.label)
    }

    @Test
    fun `editFromAnother should return null when booklet id is null and update when present`() {
        val booklet = Booklet(label = "acct-edit", amount = Amount(50L), owner = user)
        // id null -> should return null
        val resNull = bookletJpaRepositoryAdapter.editFromAnother(booklet)
        assertThat(resNull).isNull()

        // now persist then edit
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull
        val toEdit = Booklet(amount = fr.sacane.jmanager.domain.models.Amount(999L), label = saved!!.label, owner = user, id = saved.id)
        val edited = bookletJpaRepositoryAdapter.editFromAnother(toEdit)
        assertThat(edited).isNotNull
        assertThat(edited!!.amount).isEqualTo(toEdit.amount)
    }

    @Test
    fun `upsert should persist transactions linked to booklet`() {
        val booklet = Booklet(label = "acct-upsert", amount = Amount(10L), owner = user)
        val up = bookletJpaRepositoryAdapter.upsert(booklet)
        assertThat(up).isNotNull
        val uid = user!!.id.value ?: throw AssertionError("user id is null")
        val persisted = bookletJpaRepository.findByOwnerAndLabelWithTransactions(uid, booklet.label)
        assertThat(persisted).isNotNull
    }

    @Test
    fun `updateMonthlyPeriodStartDay should persist cycle settings`() {
        val booklet = Booklet(label = "acct-cycle", amount = Amount(10L), owner = user)
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull
        val savedId = saved!!.id ?: throw AssertionError("booklet id is null")

        val updated = bookletJpaRepositoryAdapter.updateMonthlyPeriodStartDay(savedId, 28, 27)
        assertThat(updated).isTrue()

        val refreshed = bookletJpaRepositoryAdapter.findBookletByIdWithTransactions(savedId)
        assertThat(refreshed).isNotNull
        assertThat(refreshed!!.monthlyPeriodStartDay).isEqualTo(28)
        assertThat(refreshed.monthlyPeriodEndDay).isEqualTo(27)
    }

    @Test
    fun `existsBookletForUser should return true when the booklet belongs to the user`() {
        val booklet = Booklet(label = "acct-exists-owned", amount = Amount(10L), owner = user)
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        val savedId = saved!!.id ?: throw AssertionError("booklet id is null")

        assertThat(bookletJpaRepositoryAdapter.existsBookletForUser(user!!.id, savedId)).isTrue()
    }

    @Test
    fun `existsBookletForUser should return false for a booklet id that does not exist`() {
        assertThat(bookletJpaRepositoryAdapter.existsBookletForUser(user!!.id, java.util.UUID.randomUUID())).isFalse()
    }

    @Test
    fun `update should evict allBooklets cache so that findUserByIdWithBooklets returns fresh amount`() {
        // Arrange: save a booklet and warm up the allBooklets cache
        val booklet = Booklet(label = "acct-cache-evict", amount = Amount(100L), owner = user)
        val saved = bookletJpaRepositoryAdapter.save(user!!.id, booklet)
        assertThat(saved).isNotNull

        // Warm up the cache — this populates the "allBooklets" entry for this user
        val cachedUser = userRepositoryJpaAdapter.findUserByIdWithBooklets(user!!.id)
        val cachedAmount = cachedUser!!.booklets.first { it.label == "acct-cache-evict" }.amount
        assertThat(cachedAmount).isEqualTo(Amount(100L))

        // Act: update the booklet amount (simulates what happens after a transaction)
        val updated = Booklet(label = saved!!.label, amount = Amount(250L), owner = user, id = saved.id)
        bookletJpaRepositoryAdapter.update(updated)

        // Assert: the cache must be evicted — fresh amount should be returned
        val freshUser = userRepositoryJpaAdapter.findUserByIdWithBooklets(user!!.id)
        val freshAmount = freshUser!!.booklets.first { it.label == "acct-cache-evict" }.amount
        assertThat(freshAmount).isEqualTo(Amount(250L))
    }
}
