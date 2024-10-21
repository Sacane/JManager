package fr.sacane.jmanager.domain.models

data class TransactionCreationResult (
    val transaction: Transaction,
    val accountAmount: Amount,
    val accountPreviewAmount: Amount
)