package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.UUID

class TagTest {

    @Test
    fun `Tag should be created with label and default values`() {
        val tag = Tag.Personal("Shopping")

        assertEquals("Shopping", tag.label)
        assertNull(tag.id)
        assertEquals(Color(0f, 0f, 0f, 0f), tag.color)
        assertFalse(tag.isDefault)
    }

    @Test
    fun `Tag should be created with all parameters`() {
        val tagId = UUID.randomUUID()
        val color = Color(255, 0, 0)
        val tag = Tag.Default("Alimentation", id = tagId, color = color)

        assertEquals(tagId, tag.id)
        assertEquals(color, tag.color)
        assertTrue(tag.isDefault)
    }

    @Test
    fun `Tag toString should display label and color components`() {
        val color = Color(255, 128, 64, 255)
        val tag = Tag.Personal("Transport", color = color)
        val stringRepresentation = tag.toString()

        assertTrue(stringRepresentation.contains("Transport"))
        assertTrue(stringRepresentation.contains("255"))
        assertTrue(stringRepresentation.contains("128"))
        assertTrue(stringRepresentation.contains("64"))
    }

    @Test
    fun `noneTag should create a default white tag`() {
        val noneTag = Tag.noneTag()

        assertEquals("Aucune", noneTag.label)
        assertEquals(Color.WHITE, noneTag.color)
        assertTrue(noneTag.isDefault)
    }

    @Test
    fun `asPersonalTag should create a non-default tag`() {
        val personalTag = "Ma catégorie".asPersonalTag()

        assertEquals("Ma catégorie", personalTag.label)
        assertFalse(personalTag.isDefault)
    }

    @Test
    fun `asPersonalTag with color should create a tag with specified color`() {
        val color = Color(0.5f, 0.5f, 0.5f, 1f)
        val personalTag = "Mes achats".asPersonalTag(color)

        assertEquals("Mes achats", personalTag.label)
        assertEquals(color, personalTag.color)
        assertFalse(personalTag.isDefault)
    }

    @Test
    fun `defaultTags should contain all predefined categories`() {
        assertEquals(7, defaultTags.size)

        val labels = defaultTags.map { it.label }
        assertTrue(labels.contains("Achat & Shopping"))
        assertTrue(labels.contains("Alimentation & Restaurant"))
        assertTrue(labels.contains("Logement & Charges"))
        assertTrue(labels.contains("Santé"))
        assertTrue(labels.contains("Transport"))
        assertTrue(labels.contains("Epargne & Placement"))
        assertTrue(labels.contains("Aucune"))
    }

    @Test
    fun `all defaultTags should be marked as default`() {
        assertTrue(defaultTags.all { it.isDefault })
    }

    @Test
    fun `defaultTags should have distinct colors`() {
        val colors = defaultTags.map { it.color }
        assertTrue(colors.toSet().size > 1)
    }
}

