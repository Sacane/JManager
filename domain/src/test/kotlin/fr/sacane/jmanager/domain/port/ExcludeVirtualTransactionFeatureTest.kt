package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.transaction.ExcludeVirtualTransactionCommand
import fr.sacane.jmanager.domain.port.input.transaction.ExcludeVirtualTransactionUseCase
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class ExcludeVirtualTransactionFeatureTest : FeatureTest() {

    companion object {
        private val excludeVirtualTransactionUseCase: ExcludeVirtualTransactionUseCase =
            FakeFactory.excludeVirtualTransactionService
        private val trackerRepository: RegularTransactionTrackerRepository =
            FakeFactory.trackerRepository()
    }

    @Nested
    inner class ExcludeVirtualTransactionSuccessScenarios {

        @Test
        fun `shouldExcludeVirtualTransaction_whenValidBookletAndRegularTransaction`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("regular-exclude-1")

                val result = excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        token = tokenValue,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.JUNE,
                        year = 2026
                    )
                )

                result.assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.JUNE)))
            }
        }

        @Test
        fun `shouldExcludeVirtualTransaction_whenCurrentMonth`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("regular-exclude-2")

                val result = excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        token = tokenValue,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.APRIL,
                        year = 2026
                    )
                )

                result.assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.APRIL)))
            }
        }

        @Test
        fun `shouldBeIdempotent_whenMonthAlreadyExcluded`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("regular-exclude-3")

                excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        token = tokenValue,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.MARCH,
                        year = 2026
                    )
                ).assertSuccess()

                val secondResult = excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        token = tokenValue,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        month = Month.MARCH,
                        year = 2026
                    )
                )

                secondResult.assertSuccess()
                val tracker = trackerRepository.findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertEquals(1, tracker!!.excludedMonths.count { it == YearMonth.of(2026, Month.MARCH) })
            }
        }
    }

    @Nested
    inner class ExcludeVirtualTransactionFailureScenarios {

        @Test
        fun `shouldReturnBookletNotFound_whenBookletDoesNotExist`() {
            launchWithConnectedUserInstance {
                val result = excludeVirtualTransactionUseCase.handle(
                    ExcludeVirtualTransactionCommand(
                        token = tokenValue,
                        bookletId = UUID.randomUUID(),
                        regularTransactionId = RegularTransactionId("regular-exclude-4"),
                        month = Month.JUNE,
                        year = 2026
                    )
                )

                result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }
        }
    }
}
