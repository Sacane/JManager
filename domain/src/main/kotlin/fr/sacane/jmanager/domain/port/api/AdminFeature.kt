package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.roleAdmin
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.domain.utils.*

interface AdminFeature {
    /**
     * Get users with pagination support
     * @param token session token used to authenticate the caller
     * @param pageNumber zero-based page index
     * @param pageSize size of the page (must be >= 1)
     */
    fun getUsers(token: String, pageNumber: Int = 0, pageSize: Int = 20): Result<Page<User>>
}

@DomainService
class AdminFeatureImpl(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val paginator: Paginator
) : AdminFeature {

    override fun getUsers(token: String, pageNumber: Int, pageSize: Int): Result<Page<User>> =
        sessionManager.authenticate(token, requiredRoles = roleAdmin) { userId ->
            val page = paginator.paginate(pageNumber, pageSize) {
                val allUsers = userRepository.findAll()
                allUsers.filter { it.id != userId }.sortedByDescending { it.creationDate }
            }

            return@authenticate success(
                Page(
                    content = page.content,
                    pageNumber = page.pageNumber,
                    pageSize = page.pageSize,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages
                )
            )
        }
}
