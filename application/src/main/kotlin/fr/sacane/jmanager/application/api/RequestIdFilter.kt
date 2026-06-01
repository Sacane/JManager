package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.port.input.MdcKeys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// Registered at the servlet container level (outside the Spring Security filter chain).
// Do NOT add this filter via SecurityConfig.addFilterBefore — OncePerRequestFilter prevents
// duplicate execution within a single dispatch, but a second MDC.put() would overwrite the UUID
// generated here, producing a different requestId in the error response than what is in the logs.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        MDC.put(MdcKeys.REQUEST_ID, UUID.randomUUID().toString())
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MdcKeys.REQUEST_ID)
        }
    }
}
