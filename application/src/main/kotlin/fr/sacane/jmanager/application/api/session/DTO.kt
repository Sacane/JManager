package fr.sacane.jmanager.application.api.session

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: String,
    val username: String,
    val email: String? = null,
    val createdDate: String? = null,
    val roles: List<String> = emptyList(),
    val subscriptionPlan: String = "BETA_TESTER",
)

@Serializable
data class RegisteredUserDTO(
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
    @field:jakarta.validation.constraints.Email
    @field:Size(max = 255)
    val email: String,
    /** RGPD Art. 6 — L'utilisateur doit accepter les CGU avant de s'inscrire. */
    val tosAccepted: Boolean = false,
    /** Version des CGU acceptées (ex: "1.0"). */
    @field:Size(max = 20)
    val tosVersion: String? = null,
    /** RGPD Art. 6 — L'utilisateur doit accepter la politique de confidentialité. */
    val privacyAccepted: Boolean = false,
)

@Serializable
data class UserPasswordDTO(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val username: String,
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val password: String
)

@Serializable
data class UserStorageDTO(
    val id: String? = null,
    val username: String,
    val token: String,
    val refreshToken: String? = null,
)

@Serializable
data class UserSettingsDTO(
    val projectionWindowDays: Int,
    val bookletCycles: List<BookletMonthlyCycleDTO>,
)

@Serializable
data class BookletMonthlyCycleDTO(
    val bookletId: String,
    val label: String,
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int? = null,
)

@Serializable
data class UserSettingsUpdateDTO(
    @field:Min(7)
    @field:Max(60)
    val projectionWindowDays: Int,
    val bookletCycles: List<BookletMonthlyCycleUpdateDTO>,
)

@Serializable
data class BookletMonthlyCycleUpdateDTO(
    @field:NotBlank
    val bookletId: String,
    @field:Min(1)
    @field:Max(31)
    val monthlyPeriodStartDay: Int,
    @field:Min(1)
    @field:Max(31)
    val monthlyPeriodEndDay: Int? = null,
)