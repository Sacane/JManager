package fr.sacane.jmanager.domain

interface BiState<I, O> {
    fun getStates(): O
    fun init(initialState: I)
    fun clear()
}