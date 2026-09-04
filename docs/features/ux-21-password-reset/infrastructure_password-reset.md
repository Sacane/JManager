# Infrastructure Module — Persist reset tokens and send the reset email

**Context**
The password reset flow needs a persistence adapter for the reset tokens and an email adapter to
deliver the reset link. The project already sends verification emails, so the reset email must reuse
the existing mail infrastructure and templating approach.

**Acceptance Criteria**
Feature: Reset token persistence and delivery
  In order to support the password reset flow
  As the system
  I want reset tokens stored and reset emails delivered

Scenario: 1. A token is persisted and retrievable
  Given a reset token issued by the domain
  When the token is saved through the persistence adapter
  Then it can be retrieved by its value with its user, expiry and used flag

Scenario: 2. Consuming a token is persisted atomically
  Given a stored reset token that is not used
  When the token is consumed together with the password update
  Then both changes are committed in the same transaction

Scenario: 3. The reset email is sent with the link
  Given a reset token was issued for a user
  When the email adapter sends the reset message
  Then the message is addressed to that user and contains the reset link with the token

Scenario: 4. Expired tokens can be purged
  Given stored reset tokens whose expiry date has passed
  When the purge routine runs
  Then those tokens are removed

**Notes**
- Add a Flyway migration for the reset token table with a unique index on the token value.
- Reuse the existing mail sender used by the email verification flow.
- Integration tests against a real database, per the project testing strategy.
- Priority P1 - Effort XL (full stack) - Part 2 of 4.
