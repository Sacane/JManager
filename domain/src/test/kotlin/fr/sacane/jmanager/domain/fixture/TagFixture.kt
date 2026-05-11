package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.Tag
import java.awt.Color
import java.util.UUID

object TagFixture {

    fun aDefaultTag(
        label: String = "Aucune",
        id: UUID? = UUID.randomUUID(),
        color: Color = Color.WHITE
    ): Tag.Default = Tag.Default(label, id, color)

    fun aPersonalTag(
        label: String = "Personal tag",
        id: UUID? = UUID.randomUUID(),
        color: Color = Color(0f, 0f, 0f, 0f),
        parentId: UUID? = null
    ): Tag.Personal = Tag.Personal(label, id, color, parentId)
}
