package fr.sacane.jmanager.domain.port.apiside.impl

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.apiside.UserRegisterFlow
import fr.sacane.jmanager.domain.port.serverside.UserTransaction

class UserRegisterFlowImpl(private val port: UserTransaction) : UserRegisterFlow{
    override fun registerUser(user: User): User {
        return port.save(user)
    }

    override fun findUserById(userId: UserId): User {
        return port.findById(userId)
    }

    override fun createUser(user: User): User? {
        return port.create(user)
    }

    override fun checkUser(userId: String, pwd: String): Boolean {
        return port.checkUser(userId, Password(pwd))
    }

    override fun findUserByPseudonym(pseudonym: String): User? {
        return port.findByPseudonym(pseudonym)
    }

}