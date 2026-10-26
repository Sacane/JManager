package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.input.user.GetUserSettingsQuery
import fr.sacane.jmanager.domain.port.input.user.SetBudgetTargetCommand
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

/** A budget target per booklet, kept with the user settings so it follows the user across devices. */
class BudgetTargetFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val current = BookletFixture.aBooklet(id = UUID.randomUUID(), label = "Compte courant", amount = 1000.toAmount())
    private val savings = BookletFixture.aBooklet(id = UUID.randomUUID(), label = "Livret A", amount = 1000.toAmount())

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldKeepTheTargetWithTheSettings_whenSetOnABookletOfTheUser`() {
        val userId = givenUser()

        val result = set(userId, current.id!!, "1500.00")

        then(result) { assertSuccess() }
        assertEquals(BigDecimal("1500.00"), targetOf(userId, current.id!!))
    }

    @Test
    fun `shouldKeepTwoDecimals`() {
        val userId = givenUser()

        set(userId, current.id!!, "1250.456")

        assertEquals(BigDecimal("1250.46"), targetOf(userId, current.id!!))
    }

    @Test
    fun `shouldLeaveTheOtherBookletsAlone`() {
        val userId = givenUser()
        set(userId, savings.id!!, "300")

        set(userId, current.id!!, "1500")

        assertEquals(BigDecimal("300.00"), targetOf(userId, savings.id!!))
    }

    @Test
    fun `shouldRefuseANegativeTarget_andKeepThePreviousOne`() {
        val userId = givenUser()
        set(userId, current.id!!, "1500")

        then(set(userId, current.id!!, "-10")) {
            assertFailure(ResultState.INVALID)
            assertEquals("domain.user.settings.negative_budget_target", errorInfo?.key)
        }
        assertEquals(BigDecimal("1500.00"), targetOf(userId, current.id!!))
    }

    @Test
    fun `shouldRefuseATargetBeyondWhatCanBeStored`() {
        val userId = givenUser()

        then(set(userId, current.id!!, "1000000000000")) {
            assertFailure(ResultState.INVALID)
            assertEquals("domain.user.settings.budget_target_too_large", errorInfo?.key)
        }
    }

    // As on the dashboard so far: emptying the field, or setting it to zero, removes the target.
    @Test
    fun `shouldRemoveTheTarget_whenClearedOrSetToZero`() {
        val userId = givenUser()
        set(userId, current.id!!, "1500")
        set(userId, savings.id!!, "300")

        set(userId, current.id!!, null)
        set(userId, savings.id!!, "0")

        assertNull(targetOf(userId, current.id!!))
        assertNull(targetOf(userId, savings.id!!))
    }

    @Test
    fun `shouldRefuse_whenTheBookletBelongsToSomeoneElse`() {
        val userId = givenUser()
        val theirs = BookletFixture.aBooklet(id = UUID.randomUUID(), label = "Leur compte", amount = 1000.toAmount())
        scenario.withUser(UserFixture.aUserWithPassword()).withBooklet(theirs)

        then(set(userId, theirs.id!!, "1500")) { assertFailure(ResultState.FORBIDDEN) }
        then(set(userId, UUID.randomUUID(), "1500")) { assertFailure(ResultState.FORBIDDEN) }
    }

    private fun givenUser(): UserId = scenario.withUser(UserFixture.aUserWithPassword()).withBooklets(current, savings).userId

    private fun set(userId: UserId, bookletId: UUID, amount: String?) =
        factory.setBudgetTargetService.handle(SetBudgetTargetCommand(userId, bookletId, amount?.let(::BigDecimal)))

    private fun targetOf(userId: UserId, bookletId: UUID): BigDecimal? =
        factory.getUserSettingsService.handle(GetUserSettingsQuery(userId)).mapNotNullOrFailure()!!
            .bookletCycles.single { it.bookletId == bookletId }.budgetTarget
}
