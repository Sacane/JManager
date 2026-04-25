package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
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
     * @param token session token used to authenticate the caller (must have ADMIN role)
     * @param pageNumber zero-based page index (default: 0)
     * @param pageSize size of the page, must be >= 1 (default: 20)
     * @return Result containing a Page<User> on success, or an authentication failure when the caller
     *         is not authenticated or does not hold the ADMIN role.
     */
    fun getUsers(token: SessionToken, pageNumber: Int = 0, pageSize: Int = 20): Result<Page<User>>
}
