package fr.sacane.jmanager.application.api.password

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordResetRequestDTO(
    @field:NotBlank
    @field:Size(max = 255)
    val email: String,
)

/** The token travels in the body, never in the path or the query string, so it never reaches access logs. */
data class PasswordResetTokenDTO(
    @field:NotBlank
    @field:Size(max = 64)
    val token: String,
)

// The minimum length is the password policy's job, in the domain: one rule, one place.
data class ConfirmPasswordResetDTO(
    @field:NotBlank
    @field:Size(max = 64)
    val token: String,
    @field:NotBlank
    @field:Size(max = 100)
    val newPassword: String,
    @field:NotBlank
    @field:Size(max = 100)
    val confirmPassword: String,
)
