package fr.sacane.jmanager.domain.port.apiside

//User Adapter to Api
//Will contains all methods which returns users for the api
interface UserFlow {
    fun registerUser()
    fun userCheckLog()
}