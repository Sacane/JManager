package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.utils.Result

/**
 * @deprecated Use [fr.sacane.jmanager.domain.port.input.admin.GetUsersUseCase] instead.
 * This interface is kept for backward compatibility with the application layer until migration is complete.
 */
@Deprecated("Use GetUsersUseCase from domain/port/input/admin instead")
interface AdminFeature {
    /**
     * Get users with pagination support.
     * @param token session token used to authenticate the caller
     * @param pageNumber zero-based page index
     * @param pageSize size of the page (must be >= 1)
     */
    fun getUsers(token: SessionToken, pageNumber: Int = 0, pageSize: Int = 20): Result<Page<User>>
}
