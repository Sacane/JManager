package fr.sacane.jmanager.domain.models

import java.awt.Color

class Tag(val label: String, val id: Long? = null, val color: Color = Color(0f, 0f, 0f, 0f), val isDefault: Boolean = false){
    override fun toString(): String {
        return """
            name: $label
            color: (${color.red}, ${color.green}, ${color.blue}, ${color.alpha})
        """.trimIndent()
    }
    companion object {
        fun noneTag(): Tag = Tag("Aucune", color = Color.WHITE, isDefault = true)
    }
}

val defaultTags: List<Tag> = listOf(
    Tag("Achat & Shopping", color = Color(1f, 0f, 0f, 1f), isDefault = true),
    Tag("Alimentation & Restaurant", color = Color(1f, 0.5f, 0f, 1f), isDefault = true),
    Tag("Logement & Charges", color = Color(0f, 1f, 0f, 1f), isDefault = true),
    Tag("Santé", color = Color(0.4f, 0.2f, 0.8f, 1f), isDefault = true),
    Tag("Transport", color = Color(1f, 0f, 1f, 1f), isDefault = true),
    Tag("Epargne & Placement", color = Color(1f, 1f, 0f, 1f), isDefault = true),
    Tag("Aucune", color = Color.WHITE, isDefault = true) // Blanc
)
fun String.asPersonalTag(color: Color = Color(0f, 0f, 0f, 0f)): Tag = Tag(this, color = color, isDefault = false)