package fr.sacane.jmanager.application

interface State<I, O> {
    fun get(): Collection<O>
    fun init(initialState: Collection<I>)
    fun clear()
}