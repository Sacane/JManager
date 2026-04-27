package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.port.output.TokenGenerator
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.application.api.session.JwtCookieAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val tokenGenerator: TokenGenerator,
    private val userDetailsService: UserRepository,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { }
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/api/user/create", permitAll)
                authorize("/api/user/auth", permitAll)
                authorize("/api/user/auth/refresh", permitAll)
                authorize("/api/user/auth/refresh/**", permitAll)
                authorize("/api/admin/**", hasRole("ADMIN"))
                authorize("/api/**", authenticated)
                authorize(anyRequest, permitAll)
            }
            httpBasic { disable() }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(JwtCookieAuthenticationFilter(tokenGenerator, userDetailsService))
        }
        return http.build()
    }
}