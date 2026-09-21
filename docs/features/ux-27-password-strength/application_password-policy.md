# Application Module — Expose the password policy

**Context**
Added on 21 September 2026, when UX-27 became full-stack. The client must show the rules before the
user types and check them as they type, but must not hold its own copy of the numbers: a copy is how
the admin console came to announce a 6-character minimum that nothing enforced. The API publishes the
policy; the domain enforces it.

**Acceptance Criteria**
Feature: Password policy over HTTP
  In order to show the real rules and report every unmet one
  As the client
  I want the policy published and its violations detailed

Scenario: 1. The policy is published
  Given an anonymous visitor
  When "GET /api/password-policy" is called
  Then the answer is 200 with the minimum and maximum length and the list of rules

Scenario: 2. A violation lists every unmet rule
  Given a request setting a password that is too short and equal to the address
  When the domain refuses it
  Then the answer is 400 with the error key "domain.user.password.policy_violation" and both rules

Scenario: 3. Every password-setting endpoint reports violations the same way
  Given a non-compliant password
  When it is submitted to registration, password change, forced change, admin creation and reset
  Then every answer has the same shape

**Notes**
- `GET /api/password-policy` is public: the registration and reset screens are shown signed out.
  It returns data that is not secret and changes only with a release, so it can be cached.
- `@Size(max = 100)` stays on the DTOs as an input bound; the minimum moves out of `@Size(min = 1)` into
  the domain. Two sources of truth for one rule is the bug this item fixes.
- The unmet rules travel as a list of stable keys in the `ProblemDetail`, next to `code` and
  `errorKey`; French wording stays in the client.
- Priority P1 · Part 2 of 3.
