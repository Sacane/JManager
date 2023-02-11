package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginInventory
import fr.sacane.jmanager.domain.port.spi.UserTransaction

@LeftPort
class LoginManager(private val loginInventory: LoginInventory, private val userTransaction: UserTransaction): Administrator{


    override fun login(pseudonym: String, userPassword: Password): Response<Ticket> {
        val user = userTransaction.findByPseudonym(pseudonym) ?: return Response.notFound()
        val canLogin = user.password.matchWith(userPassword)
        if(canLogin) {
            val ticket = loginInventory.generateToken(user) ?: return Response.invalid()
            return Response.ok(Ticket(user, ticket))
        }
        return Response.invalid()
    }

    override fun logout(userId: UserId, userToken: Token): Response<Nothing> {
        loginInventory.logout(userId, userToken) ?: return Response.invalid()
        return Response.ok()
    }

    override fun register(user: User): Response<User> {
        val userResponse = userTransaction.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

}