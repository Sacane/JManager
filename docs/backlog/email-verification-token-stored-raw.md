# Email verification tokens are stored in clear

## Observation

`email_verification_token.token` (`V27__add_email_verification.sql`) is the raw token, used as primary
key, and `VerifyEmailService` looks it up by that raw value. Anyone able to read that table — a backup,
a leaked dump, a read-only SQL access — holds working verification links for every pending account.

## Location

- `infrastructure/src/main/resources/db/migration/V27__add_email_verification.sql`
- `infrastructure/.../spi/entity/EmailVerificationTokenEntity.kt`
- `domain/.../usecase/EmailVerificationIssuer.kt`, `domain/.../port/input/user/VerifyEmailUseCase.kt`

## Expected behaviour

Store only a digest (SHA-256) of the token and look it up by digest, as UX-21 specifies for password
reset tokens. The raw value then exists only in memory and in the email.

## Impact

Low today: a verification token only marks an address as verified, it grants no access. Worth aligning
when UX-21 introduces the hashed pattern, so the codebase has one way of storing a secret token, not two.
Migrating means invalidating the pending tokens — users would request a new verification email.
