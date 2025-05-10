package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.api.session.JwtCookieAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
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
                authorize("/api/user/auth", permitAll)
                authorize(anyRequest, authenticated)
            }
            httpBasic { disable() }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(JwtCookieAuthenticationFilter(tokenGenerator, userDetailsService))
        }
        return http.build()
    }
}