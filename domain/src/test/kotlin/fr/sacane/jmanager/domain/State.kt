package fr.sacane.jmanager.domain

interface State<T> {
    fun getStates(): Collection<T>
    fun init(initialState: Collection<T>)
    fun clear()
}