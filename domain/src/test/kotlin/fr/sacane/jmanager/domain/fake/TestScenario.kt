package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction

/**
 * Composable test scenario DSL for setting up domain test preconditions.
 * Provides a fluent API to configure user, booklet, transactions and tags.
 */
class TestScenario(private val factory: FakeFactory) {

    fun withUser(
        user: UserWithPassword = UserFixture.aUserWithPassword()
    ): UserScenario {
        factory.fakeUserRepository().initWith(user)
        return UserScenario(factory, user.user)
    }
}

class UserScenario(
    private val factory: FakeFactory,
    val user: User
) {
    val userId: UserId get() = user.id

    fun withBooklet(
        booklet: Booklet = BookletFixture.aBooklet(owner = user)
    ): UserBookletScenario {
        factory.bookletState().initWith(BookletsByOwner(listOf(booklet), userId))
        return UserBookletScenario(factory, user, booklet)
    }

    fun withBooklets(vararg booklets: Booklet): UserBookletScenario {
        factory.bookletState().initWith(BookletsByOwner(booklets.toList(), userId))
        return UserBookletScenario(factory, user, booklets.first())
    }
}

class UserBookletScenario(
    private val factory: FakeFactory,
    val user: User,
    val booklet: Booklet
) {
    val userId: UserId get() = user.id

    fun withTransactions(vararg transactions: Transaction): UserBookletScenario {
        factory.fakeTransactionRepository().initWith(
            IdBookletByTransaction(
                IdUserBooklet(userId, booklet.id!!),
                transactions.toMutableList()
            )
        )
        return this
    }

    fun withRegularTransactions(vararg txs: RegularTransaction): UserBookletScenario {
        txs.forEach { tx ->
            factory.regularTransactionState.init(
                listOf(UserRegularTransaction(userId, tx))
            )
        }
        return this
    }

    fun withTags(vararg tags: UserTag): UserBookletScenario {
        tags.forEach { factory.tagTestState().init(it) }
        return this
    }
}
