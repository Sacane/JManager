package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.port.spi.UserRepository
import kotlin.random.Random

class FakeUserRepository: UserRepository, State<User> {

    private val users = mutableListOf<UserWithPassword>()

    override fun findUserById(userId: UserId): User? {
        return users.find { it.user.id == userId }?.user
    }

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        return users.find { it.user.id == userId }?.user
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return users.find { it.user.username == pseudonym }?.user
    }

    override fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword? {
        return users.find { it.user.username == pseudonym }
    }

    override fun create(user: UserWithPassword): User? {
        if(!users.add(user)) return null
        return user.user
    }

    override fun register(username: String, email: String, password: Password): User? {
        val user = User(id = UserId(Random(15).nextLong()), username = username, email="")
        users.add(UserWithPassword(user, password))
        return user
    }

    override fun upsert(user: User): User? {
        if(findUserById(user.id) != null) {
            users.removeIf { it.user.id == user.id }
        }
        users.add(UserWithPassword(user, Password("")))
        return user
    }

    override fun getStates(): Collection<User> {
        return users.map { it.user }
    }

    override fun clear() {
        users.clear()
    }

    override fun init(initialState: Collection<User>) {
        users.addAll(initialState.map { UserWithPassword(it, Password("")) })
    }
}