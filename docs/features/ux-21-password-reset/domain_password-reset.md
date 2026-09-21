# Domain Module — Password reset lifecycle and credential revocation

**Context**
The domain owns the reset token lifecycle and the rule that a new password revokes older credentials.
Read against the code on 21 September 2026:

- **Reusable as is**: `SecureTokenGenerator` (256 random bits, URL-safe), the injected `Clock`, the
  `Hasher` (BCrypt 2b with a SHA-512 pre-hash, so the 72-byte BCrypt limit is not an issue for 100
  characters), `UserRepository.updatePassword(userId, hash, clearMustChange)`,
  `UserRepository.markEmailVerified`, and `UnitOfWorkTransactionProvider.executeInTransaction`, the
  pattern the JPA audit of 29 August asked every multi-write handler to follow.
- **Not reusable as is**: `EmailVerificationToken` is stored raw and looked up by its raw value. A reset
  token grants the account, so it is **stored as its SHA-256 digest only**; the raw value exists in
  memory and in the email, never in the database. SHA-256 rather than BCrypt is deliberate: the token
  carries 256 bits of entropy, so a slow hash adds nothing, and a deterministic digest can be looked up.
- **Missing**: any password rule (UX-27 adds `PasswordPolicy`), and any notion of when credentials last
  changed, which is what lets a session be revoked.

**Model**
- `PasswordResetToken(tokenHash, userId, expiresAt)` with `isExpired(now)`. One active token per user:
  issuing a new one deletes the previous, as `EmailVerificationIssuer` already does. A consumed token
  is deleted, so "already used" and "replaced" are both simply unknown.
- `PasswordResetIssuer` — mirrors `EmailVerificationIssuer`: delete the user's tokens, generate a raw
  token, persist its digest with `expiresAt = now(clock) + ttl`, return the raw value. TTL defaults to
  **30 minutes** and is injected.
- `User.credentialsChangedAt: LocalDateTime?` — set whenever a password is set, by any path.
- Output ports: `PasswordResetTokenRepository` (`save`, `findByTokenHash`, `deleteByUserId`,
  `deleteExpired(now)`), `UserRepository.findAllEnabledByEmailIgnoreCase(email)`,
  `UserRepository.updateCredentialsChangedAt(userId, at)`, `SessionManager.revokeAll(userId)`, and two
  `NotificationPort` methods: `sendPasswordResetEmail(email, rawToken)` and
  `sendPasswordChangedEmail(email)`.

**Acceptance Criteria**
Feature: Password reset lifecycle
  In order to recover access to my account safely
  As a registered user
  I want a single-use, short-lived reset token that revokes my older credentials

Scenario: 1. Requesting a reset issues a token and sends it
  Given an enabled user whose email address is "johan@example.com"
  When a password reset is requested for "johan@example.com"
  Then a token bound to that user is persisted with an expiry 30 minutes from now
  And the reset email is sent to "johan@example.com" with the raw token

Scenario: 2. Only the digest of the token is persisted
  Given a reset has been requested for an enabled user
  When the persisted token is read back
  Then it holds the SHA-256 digest of the raw token sent by email, never the raw token

Scenario: 3. The lookup ignores case
  Given an enabled user whose email address is "Johan@Example.com"
  When a password reset is requested for "johan@example.com"
  Then a token is issued for that user

Scenario: 4. An unknown address succeeds without effect
  Given no user has the address "nobody@example.com"
  When a password reset is requested for "nobody@example.com"
  Then the request succeeds, no token is persisted and no email is sent

Scenario: 5. A disabled account is treated as unknown
  Given a disabled user whose email address is "johan@example.com"
  When a password reset is requested for "johan@example.com"
  Then the request succeeds, no token is persisted and no email is sent

Scenario: 6. An ambiguous address is treated as unknown
  Given two users whose addresses differ only by case
  When a password reset is requested for that address
  Then the request succeeds, no token is persisted and no email is sent
  And the ambiguity is reported for an administrator

Scenario: 7. A new request replaces the previous token
  Given a user holding an unexpired reset token
  When a new reset is requested for that user
  Then the previous token no longer resets the password

Scenario: 8. A valid token sets the new password
  Given a reset token that is neither expired nor replaced
  When the reset is confirmed with a new password satisfying the password policy
  Then the password is replaced, the token is deleted and the forced change flag is cleared

Scenario: 9. A reset proves the address
  Given a user whose email address is not verified
  When that user confirms a reset with a valid token
  Then the email address is marked as verified

Scenario: 10. A reset revokes every older credential
  Given a user with active sessions and refresh tokens
  When that user confirms a reset with a valid token
  Then credentialsChangedAt is set to now and every refresh token of that user is revoked

Scenario: 11. A reset is confirmed by email
  Given a user confirms a reset with a valid token
  When the reset succeeds
  Then a password changed email is sent to that user

Scenario: 12. An expired token is rejected
  Given a reset token whose expiry has passed
  When the reset is confirmed
  Then it fails with PASSWORD_RESET_TOKEN_EXPIRED and the password is unchanged

Scenario: 13. An unknown token is rejected
  Given a token that was never issued, already consumed or replaced
  When the reset is confirmed
  Then it fails with PASSWORD_RESET_TOKEN_INVALID and the password is unchanged

Scenario: 14. The token is checked before the passwords
  Given an expired reset token
  When the reset is confirmed with two different passwords
  Then it fails with PASSWORD_RESET_TOKEN_EXPIRED, not with PASSWORD_NOT_MATCH

Scenario: 15. Mismatched passwords are rejected
  Given a valid reset token
  When the reset is confirmed with two different passwords
  Then it fails with PASSWORD_NOT_MATCH and the token remains usable

Scenario: 16. The password policy applies
  Given a valid reset token
  When the reset is confirmed with a password the policy refuses
  Then it fails with the policy error of UX-27 and the token remains usable

Scenario: 17. Changing the password while signed in also revokes older credentials
  Given a signed-in user
  When that user changes their password, or completes a forced change
  Then credentialsChangedAt is set to now and every refresh token of that user is revoked

Scenario: 18. A token can be checked without being consumed
  Given a valid reset token
  When its validity is queried
  Then it is reported valid and remains usable

**Notes**
- New `ResultState` values: `PASSWORD_RESET_TOKEN_INVALID`, `PASSWORD_RESET_TOKEN_EXPIRED`, in a new
  `70xx` range. Error keys `domain.user.password_reset.token_invalid` / `.token_expired`.
- `RequestPasswordResetUseCase` **always returns success**. Every non-sending branch (4, 5, 6) must be
  indistinguishable to the caller; only a log line differs, and it never contains the address.
- Confirmation (8 to 11) runs in **one** `executeInTransaction`: password, flag, verification,
  `credentialsChangedAt` and token deletion commit together. The two emails are sent after the commit.
- Token lookup, then expiry, then password match, then policy (14, 15, 16): a stale link must say so
  before asking for anything else. A password error leaves the token usable so the user can retry.
- 17 changes `ChangePasswordService` and `ForceChangePasswordService`. Revoking the refresh tokens of
  the device that made the change is expected; the application layer re-issues its cookies (see
  `application_password-reset.md`).
- `SessionManager` is in memory: revocation holds for a single instance, which is what the session
  design already assumes. A second instance would need a shared store.
- Inject `Clock`; `InMemorySessionManager` still calls `LocalDateTime.now()` — existing, not widened.
- Depends on UX-27 domain (`PasswordPolicy`).
- Priority P1 · Effort XL (full stack) · Part 1 of 4.
