package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.api.NotFoundException
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
        try {
        val jwt = request.cookies?.firstOrNull { it.name == "token" }?.value

        if (jwt != null && SecurityContextHolder.getContext().authentication == null) {
            val token = tokenGenerator.readToken(jwt)

            if (token != null) {

                val user = userRepository.findUserById(token.userId)?.asAuthDetail(token.tokenValue, token.role)
                    ?: throw NotFoundException(1050, "User not found")
                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    listOf(
                        SimpleGrantedAuthority ("ROLE_${token.role.name}")
                    )
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    } catch (ex: NotFoundException) {
            response.status = 404
            response.contentType = "application/json"
            response.writer.write("""{"code":${ex.errCode},"message":"${ex.message}"}""")
            SecurityContextHolder.clearContext()}
    }
}
