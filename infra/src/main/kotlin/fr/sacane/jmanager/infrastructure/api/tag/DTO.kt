package fr.sacane.jmanager.infrastructure.api.tag

import kotlinx.serialization.Serializable

@Serializable
data class TagDTO(
    val tagId: Long,
    val label: String,
    val colorDTO: ColorDTO,
    val isDefault: Boolean = false
)
@Serializable
data class ColorDTO(
    val red: Int,
    val green: Int,
    val blue: Int
)

@Serializable
data class UserTagRequest(
    val tagLabel: String,
    val colorDTO: ColorDTO
)