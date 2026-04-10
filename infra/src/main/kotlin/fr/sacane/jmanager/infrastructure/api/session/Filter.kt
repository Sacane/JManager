package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.api.asAuthDetail
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.logging.Logger

@Component
class JwtCookieAuthenticationFilter(
    private val tokenGenerator: TokenGenerator,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    companion object {
        private val LOGGER = Logger.getLogger(JwtCookieAuthenticationFilter::class.java.name)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
        val jwt = request.cookies?.firstOrNull { it.name == "token" }?.value

        if (jwt != null && SecurityContextHolder.getContext().authentication == null) {
            val token = tokenGenerator.readToken(jwt)

            if (token != null) {

                val user = userRepository.findUserById(token.userId)?.asAuthDetail(token.tokenValue, token.roles)
                if (user == null) {
                    LOGGER.warning("Authenticated token references a non-existent user")
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json"
                    response.writer.write("""{"code":1050,"message":"Unauthorized"}""")
                    SecurityContextHolder.clearContext()
                    return
                }
                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    token.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    } catch (ex: Exception) {
            LOGGER.warning("Authentication filter error: ${ex.javaClass.simpleName}")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"code":1050,"message":"Unauthorized"}""")
            SecurityContextHolder.clearContext()}
    }
}
