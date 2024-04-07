package fr.sacane.jmanager.domain.models

class Tag(val label: String, val id: Long? = null)

fun tags(): List<Tag> = listOf(
    Tag("Loisir"),
    Tag("Vetements"),
    Tag("Courses"),
    Tag("Sport"),
    Tag("Bien-etre")
)
fun String.asTag(): Tag = Tag(this)