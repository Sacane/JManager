package fr.sacane.jmanager.domain.model

data class Category(
    private val label: String
){
    companion object{
        val DEFAULT_CATEGORY = Category("UNSPECIFIED_CATEGORY")
    }
}