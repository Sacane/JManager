package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.roleAdmin
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class GetUsersQuery(
    val token: SessionToken,
    val pageNumber: Int = 0,
    val pageSize: Int = 20
) : Query<Page<User>>

@Port(Side.APPLICATION)
/**
 * Input port: GetUsersUseCase
 *
 * Retrieves a paginated list of all users, restricted to authenticated administrators.
 * Implementations must verify that the caller holds the ADMIN role before proceeding.
 */
interface GetUsersUseCase : QueryHandler<GetUsersQuery, Page<User>> {
    override val queryClass get() = GetUsersQuery::class
}

@DomainService
class GetUsersService(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val paginator: Paginator
) : GetUsersUseCase {

    override fun handle(query: GetUsersQuery): Result<Page<User>> =
        sessionManager.authenticate(query.token, requiredRoles = roleAdmin) { userId ->
            val page = paginator.paginate(query.pageNumber, query.pageSize) {
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
