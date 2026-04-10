package fr.sacane.jmanager.infrastructure.api.tag

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

@Serializable
data class TagDTO(
    val tagId: String?,
    @field:NotBlank
    @field:Size(max = 50)
    val label: String,
    @field:Valid
    val colorDTO: ColorDTO,
    val isDefault: Boolean = false
)
@Serializable
data class ColorDTO(
    @field:Min(0)
    @field:Max(255)
    val red: Int,
    @field:Min(0)
    @field:Max(255)
    val green: Int,
    @field:Min(0)
    @field:Max(255)
    val blue: Int
)

@Serializable
data class UserTagRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val tagLabel: String,
    @field:Valid
    val colorDTO: ColorDTO
)