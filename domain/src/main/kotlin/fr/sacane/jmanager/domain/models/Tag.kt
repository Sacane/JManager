package fr.sacane.jmanager.domain.models

import java.awt.Color
import java.util.UUID

sealed class Tag(
    open val label: String,
    open val id: UUID? = null,
    open val color: Color = Color(0f, 0f, 0f, 0f)
) {
    val isDefault: Boolean get() = this is Default

    data class Default(
        override val label: String,
        override val id: UUID? = null,
        override val color: Color = Color(0f, 0f, 0f, 0f)
    ) : Tag(label, id, color)

    data class Personal(
        override val label: String,
        override val id: UUID? = null,
        override val color: Color = Color(0f, 0f, 0f, 0f)
    ) : Tag(label, id, color)

    companion object {
        fun noneTag(): Default = Default("Aucune", color = Color.WHITE)
    }

    override fun toString(): String =
        "name: $label\ncolor: (${color.red}, ${color.green}, ${color.blue}, ${color.alpha})"
}

val defaultTags: List<Tag.Default> = listOf(
    Tag.Default("Achat & Shopping", color = Color(1f, 0f, 0f, 1f)),
    Tag.Default("Alimentation & Restaurant", color = Color(1f, 0.5f, 0f, 1f)),
    Tag.Default("Logement & Charges", color = Color(0f, 1f, 0f, 1f)),
    Tag.Default("Santé", color = Color(0.4f, 0.2f, 0.8f, 1f)),
    Tag.Default("Transport", color = Color(1f, 0f, 1f, 1f)),
    Tag.Default("Epargne & Placement", color = Color(1f, 1f, 0f, 1f)),
    Tag.Default("Aucune", color = Color.WHITE),
)

fun String.asPersonalTag(color: Color = Color(0f, 0f, 0f, 0f)): Tag.Personal =
    Tag.Personal(this, color = color)