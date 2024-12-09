package fr.sacane.jmanager.infrastructure

interface State<T> {
    fun get(): Collection<T>
    fun init(initialState: Collection<T>)
    fun clear()
}