package fr.sacane.jmanager.application.api.session

import fr.sacane.jmanager.application.api.ForbiddenException
import fr.sacane.jmanager.application.api.JmanagerUserAuthDetail
import fr.sacane.jmanager.application.api.PublicApi
import fr.sacane.jmanager.domain.utils.ResultState
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Holds back an account with a pending step (consent, temporary password) from the rest of the API,
 * whatever client it uses. Registered on every API path except [ALLOWED_PATHS].
 */
@Component
class PendingAccountStepInterceptor : HandlerInterceptor {

    companion object {
        /** What an account can still do while a step is pending: complete it, read its status, leave or erase itself. */
        val ALLOWED_PATHS: List<String> = PublicApi.PATHS + listOf(
            "/api/user/me",
            "/api/user/consent",
            "/api/user/password/force",
            "/api/user/logout",
        )
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val account = SecurityContextHolder.getContext().authentication?.principal as? JmanagerUserAuthDetail
            ?: return true
        val step = account.pendingStep ?: return true
        throw ForbiddenException(ResultState.FORBIDDEN.code, step.detail, step.errorKey)
    }
}
