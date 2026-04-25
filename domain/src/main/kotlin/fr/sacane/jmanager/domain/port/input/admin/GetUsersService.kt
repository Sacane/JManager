package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.roleAdmin
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

@DomainService
class GetUsersService(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val paginator: Paginator
) : GetUsersUseCase {

    override fun getUsers(token: SessionToken, pageNumber: Int, pageSize: Int): Result<Page<User>> =
        sessionManager.authenticate(token, requiredRoles = roleAdmin) { userId ->
            val page = paginator.paginate(pageNumber, pageSize) {
                val allUsers = userRepository.findAll()
                allUsers.filter { it.id != userId }.sortedByDescending { it.creationDate }
            }
            success(
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
