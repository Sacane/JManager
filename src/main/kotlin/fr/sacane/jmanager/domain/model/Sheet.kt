package fr.sacane.jmanager.domain.model

import java.time.LocalDate

data class Sheet(
        var id: SheetId,
        var label: String,
        var date: LocalDate,
        var value: Double,
        var labelAccount: String,
        var isEntry: Boolean
)
