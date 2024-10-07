package fr.sacane.jmanager.infrastructure.rest.preview.dto

import fr.sacane.jmanager.infrastructure.rest.tag.TagDTO
import java.time.LocalDate

data class PreviewTransactionDTO (
    val userId: Long,
    val accountId: Long,
    val label: String,
    val value: Double,
    val currency: String = "€",
    val date: LocalDate,
    val isIncome: Boolean,
    val tag: TagDTO?
)