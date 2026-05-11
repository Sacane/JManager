package fr.sacane.jmanager.domain

/**
 * Initialize a [State] with vararg elements instead of a collection.
 */
fun <T> State<T>.initWith(vararg elements: T) {
    init(elements.toList())
}
