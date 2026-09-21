# Infrastructure Module — Reset token persistence, emails and token issue time

**Context**
Adapters for the ports of `domain_password-reset.md`. Read against the code on 21 September 2026:

- Migrations are Flyway, latest `V28__add_must_change_password.sql`. `email_verification_token`
  (`V27`) is the model to follow for the foreign key and cascade — but not for the key: it uses the raw
  token as primary key.
- `user_resource.email` is `VARCHAR(255) UNIQUE`, **case-sensitive**, and `UserPostgresRepository`
  only offers an exact `findByEmail`.
- `SpringMailNotificationAdapter` sends HTML built by `EmailTemplates`, `@Async`, and swallows SMTP
  failures into a log line. Asynchrony matters here: the request for a known address must not take
  measurably longer than for an unknown one because of SMTP.
- `JwtTokenGenerator` sets `subject`, `expiration` (1 h) and role claims, but **no `iat`**. The
  application layer needs the issue time to reject tokens older than a password change.
- `RetentionScheduler` already runs a nightly cron (`jmanager.retention.cron`, 02:00).

**Acceptance Criteria**
Feature: Reset token persistence and delivery
  In order to support the password reset flow
  As the system
  I want reset tokens stored safely and reset emails delivered

Scenario: 1. A token digest is persisted and found
  Given a reset token issued by the domain
  When it is saved and then looked up by its digest
  Then it is returned with its user and expiry

Scenario: 2. One token per user
  Given a user holding a stored reset token
  When the tokens of that user are deleted and a new one is saved
  Then only the new token exists for that user

Scenario: 3. Deleting a user deletes their tokens
  Given a user holding a stored reset token
  When the user is deleted
  Then the token is deleted with it

Scenario: 4. Expired tokens are purged
  Given stored reset tokens, some expired
  When the nightly purge runs
  Then only the expired tokens are deleted

Scenario: 5. Enabled users are found by address regardless of case
  Given an enabled user "Johan@Example.com" and a disabled user "old@example.com"
  When users are looked up by "johan@example.com" and by "OLD@example.com"
  Then the first lookup returns the enabled user and the second returns nothing

Scenario: 6. Every case variant of an address is returned
  Given two users "Johan@Example.com" and "johan@example.com"
  When users are looked up by "JOHAN@EXAMPLE.COM"
  Then both are returned, so the domain can refuse the ambiguity

Scenario: 7. The credential change time is persisted
  Given a user
  When their credentialsChangedAt is updated
  Then reading the user returns that time

Scenario: 8. The reset email carries the link in the fragment
  Given a raw reset token
  When the reset email is sent
  Then it is addressed to the user and links to "{app.url}/reset-password#token={raw token}"
  And it states that the link expires in 30 minutes and can be ignored if the user asked nothing

Scenario: 9. The password changed email invites action
  Given a password was just reset
  When the password changed email is sent
  Then it states when the change happened and what to do if the user did not make it

Scenario: 10. Access tokens carry their issue time
  Given a user signs in
  When the access token is generated and read back
  Then it carries an issued-at time

**Notes**
- `V29__add_password_reset.sql`: table `password_reset_token (token_hash CHAR(64) PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE REFERENCES user_resource(id_user) ON DELETE CASCADE,
  expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL)`; the `UNIQUE` on `user_id` enforces
  one token per user in the schema, not only in code. Plus
  `ALTER TABLE user_resource ADD COLUMN credentials_changed_at TIMESTAMP NULL` — null for every
  existing user, meaning "no revocation yet".
- Case-insensitive lookup: `lower(email) = lower(:email)` returning a **list**. No functional index
  needed at the current table size; revisit if it grows.
- The link uses the **fragment** (`#token=`) on purpose: a fragment is never sent to a server, so it
  cannot reach access logs or a `Referer` header. This differs from `/verify-email?token=`.
- Purge: add the reset table to the nightly `RetentionScheduler` run rather than a new schedule.
- Adding `iat` changes every token issued after deployment; tokens issued before carry none, and the
  application layer must treat a missing `iat` as older than any change (see its notes).
- Integration tests against a real database (Testcontainers), per the project testing strategy.
- Priority P1 · Effort XL (full stack) · Part 2 of 4.
