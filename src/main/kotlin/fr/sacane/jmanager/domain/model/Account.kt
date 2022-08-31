package fr.sacane.jmanager.domain.model


data class Account(
        val amount: Double,
        val labelAccount: String,
        val sheets: List<Sheet>
)
