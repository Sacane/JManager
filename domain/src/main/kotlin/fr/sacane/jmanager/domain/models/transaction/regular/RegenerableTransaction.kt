package fr.sacane.jmanager.domain.models.transaction.regular

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import java.time.LocalDate

/**
 * A single occurrence of a regular transaction that is currently excluded for its month,
 * and could therefore be restored by the user.
 *
 * Exclusion is tracked per `(regularTransactionId, month)` pair — never per occurrence.
 * A recurrence producing several occurrences in the same month (weekly, daily) therefore yields
 * several [RegenerableTransaction] sharing the same [regularTransactionId], and restoring any one
 * of them restores them all.
 */
data class RegenerableTransaction(
    val regularTransactionId: RegularTransactionId,
    val label: String,
    val amount: Amount,
    val isIncome: Boolean,
    val tag: Tag?,
    val date: LocalDate
)
