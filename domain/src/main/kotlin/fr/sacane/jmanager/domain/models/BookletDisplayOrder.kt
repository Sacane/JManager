package fr.sacane.jmanager.domain.models

/**
 * The booklets in the order the user chose. A booklet they have not placed yet, such as one created
 * since, comes after the others; booklets on the same footing keep the order they came in.
 */
fun List<Booklet>.inDisplayOrder(): List<Booklet> = sortedWith(compareBy(nullsLast()) { it.displayPosition })
