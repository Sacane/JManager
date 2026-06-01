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
    val subscriptionPlan: String,
    val email: String? = null,
    val createdDate: String? = null,
    val roles: List<String> = emptyList(),
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
    @field:jakarta.validation.constraints.Email
    @field:Size(max = 255)
    val email: String,
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

/** GDPR — payload for the first-login consent gate (`POST /api/user/consent`). */
@Serializable
data class RecordConsentDTO(
    /** Whether the user explicitly accepted the Terms of Service. */
    val tosAccepted: Boolean = false,
    /** Version string of the TOS accepted (e.g. "1.0"). */
    @field:Size(max = 20)
    val tosVersion: String? = null,
    /** Whether the user explicitly accepted the Privacy Policy. */
    val privacyAccepted: Boolean = false,
)

/** Lightweight status payload returned by `GET /api/user/me`. */
@Serializable
data class UserStatusDTO(
    /** `true` when the account was created without explicit consent (e.g. admin-created) and the user must go through the consent gate. */
    val consentRequired: Boolean,
    /** `false` when the user has not yet clicked the verification link sent at registration. */
    val emailVerified: Boolean = true,
    /** The user's registered email address. */
    val email: String? = null,
)