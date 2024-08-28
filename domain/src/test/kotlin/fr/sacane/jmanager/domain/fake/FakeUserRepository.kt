package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.UserRepository
import kotlin.random.Random

class FakeUserRepository: UserRepository, State<User> {

    private val users = mutableListOf<User>()

    override fun findUserById(userId: UserId): User? {
        return users.find { it.id == userId }
    }

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        return users.find { it.id == userId }
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return users.find { it.username == pseudonym }
    }

    override fun create(user: User): User? {
        if(!users.add(user)) return null
        return user
    }

    override fun register(username: String, email: String, password: Password): User? {
        val user = User(id = UserId(Random(15).nextLong()), username = username, email="", password=password)
        users.add(user)
        return user
    }

    override fun upsert(user: User): User? {
        if(findUserById(user.id) != null) {
            users.removeIf { it.id == user.id }
        }
        users.add(user)
        return user
    }

    override fun getStates(): Collection<User> {
        return users
    }

    override fun clear() {
        users.clear()
    }

    override fun init(initialState: Collection<User>) {
        users.addAll(initialState)
    }
}