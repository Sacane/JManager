package fr.sacane.jmanager.domain.models

import java.awt.Color

class Tag(val label: String, val id: Long? = null, val color: Color = Color(0f, 0f, 0f, 0f), val isDefault: Boolean = false){
    override fun toString(): String {
        return "$color"
    }
}

val defaultTags: List<Tag> = listOf(
    Tag("Loisir", color = Color.BLUE, isDefault = true),
    Tag("Esthetique", color = Color.ORANGE, isDefault = true),
    Tag("Courses", color = Color.YELLOW, isDefault = true),
    Tag("Sport", color = Color.RED, isDefault = true),
    Tag("Bien-etre", color = Color.GREEN, isDefault = true)
)
fun String.asTag(color: Color = Color(0f, 0f, 0f, 0f), isDefault: Boolean = false): Tag = Tag(this, color = color, isDefault = isDefault)