# Sign-in reveals whether an email address has an account

## Observation

`LoginService.handle` answers differently for the two ways a sign-in can fail:

| Case | Result | Message | Error key |
|---|---|---|---|
| Unknown address | `NOT_FOUND` → **404** | `Aucun compte associé à l'adresse <address>` | `domain.user.login.user_not_found` |
| Known address, wrong password | `USER_UNAUTHORIZED` | `L'adresse e-mail ou le mot de passe est incorrect` | `domain.user.login.invalid_credentials` |

Reproduced against production on 21 September 2026 with an unregistered address: HTTP 404 and the first
message above, echoing the address back.

A second, quieter signal: an unknown address returns without hashing anything, while a known one runs
BCrypt at strength 12 (in the order of a few hundred milliseconds). The response time separates the two
cases even if the messages were made identical.

## Location

- `domain/.../port/input/user/LoginUseCase.kt` (`LoginService.handle`)
- `application/.../api/session/Controller.kt` (`login`, the 429 path also depends on the client address)

## Expected behaviour

One answer for both failures — same status, same message, same error key — and comparable cost: verify
the submitted password against a fixed dummy hash when the address is unknown.

## Impact

Any visitor can test whether a given address is registered, one request at a time. The only defence is
the sign-in rate limit, which may be keyed on the proxy's address (see
`login-rate-limit-keyed-on-proxy-ip.md`) and would then protect nothing.

It also undercuts two decisions made elsewhere in the UI/UX workstream:

- **UX-20** kept sign-in errors form-level "so as not to reveal whether an account exists". The client
  honours that; the API does not, so the choice bought nothing.
- **UX-21** requires the reset to answer identically for known and unknown addresses. That guarantee is
  worth little while the sign-in page next to it gives the answer away.

Fixing this belongs before or with UX-21, and it is small: one branch in `LoginService` plus tests.
