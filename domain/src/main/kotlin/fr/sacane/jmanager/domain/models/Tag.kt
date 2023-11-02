package fr.sacane.jmanager.domain.models

@JvmInline
value class Tag(val label: String)
fun tags(): List<Tag> {
    return listOf(
        Tag("Aucun"),
        Tag("Loisir"),
        Tag("Vetements"),
        Tag("Courses"),
        Tag("Sport"),
        Tag("Bien-etre")
    )
}