package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.UserForAdmin
import fr.sacane.jmanager.domain.models.roleAdmin
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.domain.utils.*
import kotlin.math.min

interface AdminFeature {
    /**
     * Get users with pagination support
     * @param token session token used to authenticate the caller
     * @param pageNumber zero-based page index
     * @param pageSize size of the page (must be >= 1)
     */
    fun getUsers(token: String, pageNumber: Int = 0, pageSize: Int = 20): Result<Page<UserForAdmin>>
}

@DomainService
class AdminFeatureImpl(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
) : AdminFeature {

    override fun getUsers(
        token: String,
        pageNumber: Int,
        pageSize: Int,
    ): Result<Page<UserForAdmin>> = sessionManager.authenticate(token, requiredRoles = roleAdmin) {
            val normalizedPageSize = if (pageSize <= 0) 20 else pageSize
            val normalizedPageNumber = if (pageNumber < 0) 0 else pageNumber

            val allUsers = userRepository.findAll()
            val sorted = allUsers.sortedByDescending { it.creationDate }

            val totalElements = sorted.size.toLong()
            val fromIndex = normalizedPageNumber.toLong() * normalizedPageSize.toLong()
            val content = if (fromIndex >= totalElements) {
                emptyList()
            } else {
                val toIndexExclusive = min(totalElements, fromIndex + normalizedPageSize)
                sorted.subList(fromIndex.toInt(), toIndexExclusive.toInt()).map { it.toModelForAdmin() }
            }

            val totalPages = ((totalElements + normalizedPageSize.toLong() - 1L) / normalizedPageSize.toLong()).toInt()

            return@authenticate success(
                Page(
                    content = content,
                    pageNumber = normalizedPageNumber,
                    pageSize = normalizedPageSize,
                    totalElements = totalElements,
                    totalPages = totalPages
                )
            )
        }
}
