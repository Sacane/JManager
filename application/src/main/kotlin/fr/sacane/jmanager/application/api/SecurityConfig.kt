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
import org.springframework.security.web.access.AccessDeniedHandler
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
                // Public SPA routes
                authorize("/", permitAll)
                authorize("/index.html", permitAll)
                authorize("/_nuxt/**", permitAll)
                authorize("/assets/**", permitAll)
                authorize("/favicon.ico", permitAll)
                authorize("/login", permitAll)
                authorize("/dashboard", permitAll)
                authorize("/dashboard/**", permitAll)
                authorize("/booklet", permitAll)
                authorize("/booklet/**", permitAll)
                authorize("/admin", permitAll)
                authorize("/admin/**", permitAll)
                authorize("/tag", permitAll)
                authorize("/tag/**", permitAll)
                authorize("/user", permitAll)
                authorize("/user/**", permitAll)
                authorize("/regular-transaction", permitAll)
                authorize("/regular-transaction/**", permitAll)
                // Legal pages (public, no auth)
                authorize("/privacy", permitAll)
                authorize("/terms", permitAll)
                authorize("/consent", permitAll)
                // Actuator
                authorize("/actuator/health", permitAll)
                authorize("/actuator/info", permitAll)
                // Public API
                authorize("/api/feature-flags", permitAll)
                authorize("/api/user/create", permitAll)
                authorize("/api/user/auth", permitAll)
                authorize("/api/user/auth/refresh/**", permitAll)
                // Protected API
                authorize("/api/user/me", authenticated)
                authorize("/api/admin/**", hasRole("ADMIN"))
                authorize("/api/**", authenticated)
                // Deny everything else
                authorize(anyRequest, denyAll)
            }
            httpBasic { disable() }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                accessDeniedHandler = AccessDeniedHandler { _, response, _ -> response.status = HttpStatus.FORBIDDEN.value() }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(JwtCookieAuthenticationFilter(tokenGenerator, userDetailsService))
        }
        return http.build()
    }
}