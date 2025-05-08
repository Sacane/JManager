package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.api.asAuthDetail
import fr.sacane.jmanager.infrastructure.spi.adapters.JwtTokenGenerator
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtCookieAuthenticationFilter(
    private val tokenGenerator: TokenGenerator,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = request.cookies?.firstOrNull { it.name == "token" }?.value

        if (jwt != null && SecurityContextHolder.getContext().authentication == null) {
            val token = tokenGenerator.readToken(jwt)

            if (token != null) {
                val user = userRepository.findUserById(token.userId)?.asAuthDetail(token.tokenValue, token.role)
                    ?: throw IllegalArgumentException("User not found")

                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    listOf(
                        GrantedAuthority { "ROLE_${token.role.name}" }
                    )
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
