package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
/**
 * Input port: GetUsersUseCase
 *
 * Retrieves a paginated list of all users, restricted to authenticated administrators.
 * Implementations must verify that the caller holds the ADMIN role before proceeding.
 */
interface GetUsersUseCase {
    /**
     * Get all users with pagination support.
     *
     * @param query encapsulates the caller's session token, the zero-based page index and the page size
     * @return Result containing a Page<User> on success, or an authentication failure when the caller
     *         is not authenticated or does not hold the ADMIN role.
     */
    fun handle(query: GetUsersQuery): Result<Page<User>>
}
