package fr.sacane.jmanager.domain.model

data class Category(
    val label: String
)
object CategoryFactory{
    val DEFAULT_CATEGORY = Category("Aucune")
    fun allDefaultCategories(): MutableList<Category> {
        return mutableListOf(
            DEFAULT_CATEGORY,
            Category("Loisir"),
            Category("Vetements"),
            Category("Courses"),
            Category("Sport"),
            Category("Bien-etre")
        )
    }
}