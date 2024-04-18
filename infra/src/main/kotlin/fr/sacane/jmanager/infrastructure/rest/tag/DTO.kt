package fr.sacane.jmanager.infrastructure.rest.tag

data class TagDTO(
    val tagId: Long,
    val label: String,
    val colorDTO: ColorDTO = ColorDTO(0, 0, 0),
    val isDefault: Boolean = false
)

data class ColorDTO(
    val red: Int,
    val green: Int,
    val blue: Int
)

data class UserTagDTO(
    val userId: Long,
    val tagLabel: String,
    val colorDTO: ColorDTO
)