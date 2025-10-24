package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.UserForAdmin
import fr.sacane.jmanager.domain.models.roleAdmin
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.domain.utils.*

interface AdminFeature {
    /**
     * Get all users in the database
     */
    fun getUsers(token: String): Result<Page<UserForAdmin>>
}

@DomainService
class AdminFeatureImpl(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
) : AdminFeature {

    override fun getUsers(
        token: String,

    ): Result<Page<UserForAdmin>> = sessionManager
        .authenticate(token, requiredRoles = roleAdmin) {
            val users = userRepository.findAll()
            return@authenticate success(Page(users.map { it.toModelForAdmin() }))
        }
}
