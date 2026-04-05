package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction

data class TransactionResumeResult (
    val transaction: Transaction,
    val bookletAmount: Amount,
)