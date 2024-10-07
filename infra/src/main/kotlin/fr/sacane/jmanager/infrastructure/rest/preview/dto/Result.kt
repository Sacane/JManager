package fr.sacane.jmanager.infrastructure.rest.preview.dto

data class PreviewTransactionOutDTO(
    val id: Long,
    val label: String,
    val value: Double,
    val currency: String = "€",
    val isIncome: Boolean
)