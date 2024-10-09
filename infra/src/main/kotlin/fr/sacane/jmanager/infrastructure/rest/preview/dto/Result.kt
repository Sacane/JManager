package fr.sacane.jmanager.infrastructure.rest.preview.dto

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.rest.tag.TagDTO
import java.time.LocalDate

data class PreviewTransactionOutDTO(
    val id: Long,
    val label: String,
    val value: Double,
    val currency: String = "€",
    val isIncome: Boolean,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val date: LocalDate,
    val tagDTO: TagDTO? = null
)