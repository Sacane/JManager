package fr.sacane.jmanager.infrastructure.api.tag

data class TagDTO(
    val tagId: Long,
    val label: String,
    val colorDTO: ColorDTO,
    val isDefault: Boolean = false
)

data class ColorDTO(
    val red: Int,
    val green: Int,
    val blue: Int
)

data class UserTagRequest(
    val userId: Long,
    val tagLabel: String,
    val colorDTO: ColorDTO
)