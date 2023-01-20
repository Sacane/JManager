package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.User

//User Adapter to Api
//Will contains all methods which returns users for the api
interface UserControl {
    fun createUser(user: User): User?
    fun checkUser(userId: String, pwd:String): Boolean
}