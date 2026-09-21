# Domain Module — One password policy for every way of setting a password

**Context**
Added on 21 September 2026, when UX-27 became full-stack. It was planned as frontend only, on the
assumption that the client would "mirror the backend policy". There is no backend policy: every request
DTO accepting a password declares `@Size(min = 1, max = 100)` — registration, admin creation, password
change, forced change — and no domain service checks anything but equality with the confirmation.
A password of one character is accepted today. Rules shown only by the client would be decoration over
an API that still accepts them.

Decision of the product owner, 21 September 2026: **the policy lives in the domain and applies to every
path that sets a password**, including the reset of UX-21, which consumes it.

**Policy** (proposed values — see Notes)
- at least **12** characters, at most **100** — counted in characters, not bytes;
- no composition rule (no forced digit, capital or symbol): length is what resists guessing, and
  composition rules push users toward predictable patterns;
- not equal to the account's email address, ignoring case.

The policy returns **every** unmet rule, not the first one, so the client can highlight them all.

**Acceptance Criteria**
Feature: Password policy
  In order to keep accounts from being guessed
  As the system
  I want one set of password rules enforced wherever a password is set

Scenario: 1. A long enough password is accepted
  Given a password of 12 characters that is not the account's address
  When it is checked against the policy
  Then it is accepted

Scenario: 2. A short password is refused
  Given a password of 11 characters
  When it is checked against the policy
  Then it is refused with the rule "too_short"

Scenario: 3. Length counts characters, not bytes
  Given a password of 12 characters including accented letters and an emoji
  When it is checked against the policy
  Then it is accepted

Scenario: 4. The address is refused as a password
  Given an account whose address is "Johan@Example.com"
  When the password "johan@example.com" is checked for that account
  Then it is refused with the rule "equals_email"

Scenario: 5. Every unmet rule is reported
  Given an account whose address is "a@b.fr" and the password "a@b.fr"
  When it is checked against the policy
  Then both "too_short" and "equals_email" are reported

Scenario: 6. Registration applies the policy
  Given a visitor registering with an 8-character password
  When the registration is submitted
  Then it fails with PASSWORD_POLICY_VIOLATION and no account is created

Scenario: 7. Changing a password applies the policy
  Given a signed-in user
  When they change their password to an 8-character one, in the settings or through a forced change
  Then it fails with PASSWORD_POLICY_VIOLATION and the password is unchanged

Scenario: 8. An administrator creating an account applies the policy
  Given an administrator creating an account with an 8-character temporary password
  When the creation is submitted
  Then it fails with PASSWORD_POLICY_VIOLATION

Scenario: 9. Existing passwords keep working
  Given a user whose current password is 6 characters long
  When that user signs in
  Then sign-in succeeds: the policy applies when a password is set, never when one is checked

Scenario: 10. The bootstrap administrator never blocks start-up
  Given the bootstrap administrator password from the environment is shorter than the policy
  When the application starts
  Then the account is created as before and a warning is logged

**Notes**
- New `ResultState.PASSWORD_POLICY_VIOLATION`, error key `domain.user.password.policy_violation`,
  carrying the list of unmet rules (`too_short`, `too_long`, `equals_email`) so the client can map each
  to its own line.
- Order in `ChangePasswordService` stays: current password, then confirmation match, then **policy**,
  then "different from the current one". A wrong current password must not reveal the policy result.
- Scenario 10: `CreateAdminIfNotExistsService` reads its password from the environment at start-up.
  Refusing it would stop the application from booting after a deployment; it warns instead.
- **12 is a proposal, not a decision.** For a password that is the only factor, NIST SP 800-63B (rev. 4)
  asks for at least 15 characters and accepts 8 only alongside a second factor; 12 is a common middle
  ground. JManager has no second factor. Changing the value later is one constant, but it changes what
  users are asked to type — confirm before implementing, and check the current guidance rather than
  this note.
- Pure Kotlin, no framework; a value object or a small domain service injected where needed.
- Priority P1 · Effort M → **L** (now full-stack) · Part 1 of 3.
