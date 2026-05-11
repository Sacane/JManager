package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.given
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.transaction.ExcludeVirtualTransactionCommand
import fr.sacane.jmanager.domain.port.input.transaction.ExcludeVirtualTransactionUseCase
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class ExcludeVirtualTransactionFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val excludeVirtualTransactionUseCase: ExcludeVirtualTransactionUseCase =
        factory.excludeVirtualTransactionService
    private val trackerRepository: RegularTransactionTrackerRepository =
        factory.trackerRepository()

    @AfterEach
    fun clear() {
        factory.clearAll()
    }

    @Nested
    inner class ExcludeVirtualTransactionSuccessScenarios {

        @Test
        fun `shouldExcludeVirtualTransaction_whenValidBookletAndRegularTransaction`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val regularTransactionId = RegularTransactionId("regular-exclude-1")

            val result = act {
                excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.JUNE,
                        year = 2026
                    )
                )
            }

            then(result) {
                assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, ctx.booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.JUNE)))
            }
        }

        @Test
        fun `shouldExcludeVirtualTransaction_whenCurrentMonth`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val regularTransactionId = RegularTransactionId("regular-exclude-2")

            val result = act {
                excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.APRIL,
                        year = 2026
                    )
                )
            }

            then(result) {
                assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, ctx.booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.APRIL)))
            }
        }

        @Test
        fun `shouldBeIdempotent_whenMonthAlreadyExcluded`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val regularTransactionId = RegularTransactionId("regular-exclude-3")

            excludeVirtualTransactionUseCase.handle(
                ExcludeVirtualTransactionCommand(
                    userId = ctx.userId,
                    bookletId = ctx.booklet.id!!,
                    regularTransactionId = regularTransactionId,
                    month = Month.MARCH,
                    year = 2026
                )
            ).assertSuccess()

            val secondResult = act {
                excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.MARCH,
                        year = 2026
                    )
                )
            }

            then(secondResult) {
                assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, ctx.booklet.id!!)
                assertNotNull(tracker)
                assertEquals(1, tracker!!.excludedMonths.count { it == YearMonth.of(2026, Month.MARCH) })
            }
        }
    }

    @Nested
    inner class ExcludeVirtualTransactionFailureScenarios {

        @Test
        fun `shouldReturnBookletNotFound_whenBookletDoesNotExist`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = act {
                excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = UUID.randomUUID(),
                        regularTransactionId = RegularTransactionId("regular-exclude-4"),
                        month = Month.JUNE,
                        year = 2026
                    )
                )
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
        }
    }
}
