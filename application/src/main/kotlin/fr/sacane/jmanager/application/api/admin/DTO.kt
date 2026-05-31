package fr.sacane.jmanager.application.api.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

/** Request payload for `POST /api/admin/users`. */
@Serializable
data class AdminCreateUserRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val username: String,
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val password: String,
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val confirmPassword: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
)
