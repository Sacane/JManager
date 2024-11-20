package fr.sacane.jmanager.domain.models

data class TransactionResumeResult (
    val transaction: Transaction,
    val accountAmount: Amount,
    val accountPreviewAmount: Amount
)