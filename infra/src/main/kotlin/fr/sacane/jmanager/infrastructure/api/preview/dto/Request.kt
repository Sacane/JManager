package fr.sacane.jmanager.infrastructure.api.preview.dto

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import java.time.LocalDate

data class PreviewTransactionDTO (
    val userId: Long,
    val accountId: Long,
    val label: String,
    val value: Double,
    val currency: String = "€",
    @JsonFormat(pattern = "dd-MM-yyyy")
    val date: LocalDate,
    val isIncome: Boolean,
    val tag: TagDTO? = null
)