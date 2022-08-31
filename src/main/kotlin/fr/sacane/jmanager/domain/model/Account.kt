package fr.sacane.jmanager.domain.model

data class Account(
        var amount: Double,
        val labelAccount: String,
        var isMainAccount: Boolean?
)
