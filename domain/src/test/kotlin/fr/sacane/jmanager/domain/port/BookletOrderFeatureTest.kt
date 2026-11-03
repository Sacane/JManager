package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.input.booklet.FindAllRegisteredBookletsQuery
import fr.sacane.jmanager.domain.port.input.booklet.ReorderBookletsCommand
import fr.sacane.jmanager.domain.port.input.booklet.SaveBookletCommand
import fr.sacane.jmanager.domain.port.input.user.GetUserSettingsQuery
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

/** The order the user gives their booklets, kept with the account so every screen and device shows the same one. */
class BookletOrderFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val current = aBooklet("Compte courant")
    private val savings = aBooklet("Livret A")
    private val joint = aBooklet("Compte joint")

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldKeepTheStoredOrder_untilTheUserChoosesOne`() {
        val userId = givenUser()

        assertEquals(listOf(current, savings, joint).ids(), listedIds(userId))
    }

    @Test
    fun `shouldListTheBookletsInTheChosenOrder`() {
        val userId = givenUser()

        val result = reorder(userId, joint, current, savings)

        then(result) { assertSuccess() }
        assertEquals(listOf(joint, current, savings).ids(), listedIds(userId))
    }

    @Test
    fun `shouldShowTheSameOrderInTheSettings`() {
        val userId = givenUser()

        reorder(userId, savings, joint, current)

        assertEquals(listOf(savings, joint, current).ids(), settingsIds(userId))
    }

    // A screen opened before a booklet was created sends an order without it: that booklet is not lost.
    @Test
    fun `shouldKeepTheBookletsLeftOutAfterTheOthers_inTheirPreviousOrder`() {
        val userId = givenUser()
        reorder(userId, joint, savings, current)

        reorder(userId, current)

        assertEquals(listOf(current, joint, savings).ids(), listedIds(userId))
    }

    @Test
    fun `shouldListANewBookletLast`() {
        val userId = givenUser()
        reorder(userId, joint, current, savings)
        val added = aBooklet("Épargne vacances")

        then(factory.saveBookletService.handle(SaveBookletCommand(userId, added))) { assertSuccess() }

        assertEquals(listOf(joint, current, savings, added).ids(), listedIds(userId))
    }

    @Test
    fun `shouldRefuseAnEmptyOrder`() {
        val userId = givenUser()

        then(factory.reorderBookletsService.handle(ReorderBookletsCommand(userId, emptyList()))) {
            assertFailure(ResultState.INVALID)
            assertEquals("domain.booklet.order.empty", errorInfo?.key)
        }
    }

    @Test
    fun `shouldRefuseABookletGivenTwice_andKeepThePreviousOrder`() {
        val userId = givenUser()
        reorder(userId, joint, current, savings)

        then(reorder(userId, savings, current, savings)) {
            assertFailure(ResultState.INVALID)
            assertEquals("domain.booklet.order.duplicate", errorInfo?.key)
        }
        assertEquals(listOf(joint, current, savings).ids(), listedIds(userId))
    }

    @Test
    fun `shouldRefuse_whenABookletIsNotTheUsers_andKeepThePreviousOrder`() {
        val userId = givenUser()
        val theirs = aBooklet("Leur compte")
        scenario.withUser(UserFixture.aUserWithPassword()).withBooklet(theirs)
        reorder(userId, joint, current, savings)

        then(reorder(userId, savings, theirs)) { assertFailure(ResultState.FORBIDDEN) }
        then(factory.reorderBookletsService.handle(ReorderBookletsCommand(userId, listOf(UUID.randomUUID())))) {
            assertFailure(ResultState.FORBIDDEN)
        }
        assertEquals(listOf(joint, current, savings).ids(), listedIds(userId))
    }

    private fun aBooklet(label: String): Booklet = BookletFixture.aBooklet(id = UUID.randomUUID(), label = label, amount = 1000.toAmount())

    private fun givenUser(): UserId = scenario.withUser(UserFixture.aUserWithPassword()).withBooklets(current, savings, joint).userId

    private fun reorder(userId: UserId, vararg booklets: Booklet) =
        factory.reorderBookletsService.handle(ReorderBookletsCommand(userId, booklets.toList().ids()))

    private fun listedIds(userId: UserId): List<UUID> =
        factory.findAllRegisteredBookletsService.handle(FindAllRegisteredBookletsQuery(userId)).mapNotNullOrFailure()!!.ids()

    private fun settingsIds(userId: UserId): List<UUID> =
        factory.getUserSettingsService.handle(GetUserSettingsQuery(userId)).mapNotNullOrFailure()!!
            .bookletCycles.map { it.bookletId }

    private fun List<Booklet>.ids(): List<UUID> = map { it.id!! }
}
