# Sign-in is limited per client address only, never per account

## Observation

`LoginRateLimiter` counts failed sign-ins per client address (`X-Forwarded-For` since the fix of
21 September 2026, the proxy's address before). Nothing limits attempts against **one account** across
addresses. An attacker who rotates addresses can try passwords against a single account without ever
reaching the threshold of any one address.

Until that fix the limit was accidentally global — every visitor shared the proxy's key — which throttled
this attack as a side effect, at the price of locking every legitimate user out for 15 minutes after five
failures from anyone. Keying on the real client address removed both the outage and the accidental brake.

## Location

- `application/.../api/session/LoginRateLimiter.kt`
- `application/.../api/session/Controller.kt` (`login`)

## Expected behaviour

A second, independent limit on failures per account (per email address), with a delay or a temporary
lock that cannot be used to deny service to the account's owner — for example a growing delay between
attempts rather than a hard lock, so an attacker cannot lock a victim out at will.

## Impact

Medium. Password guessing against a known address is limited only by BCrypt's cost (strength 12) and the
per-address limit. The gap is wider while the domain accepts weak passwords: there is no password policy
today (see UX-27), so a guessable password is realistic.

Sign-in also reveals whether an address has an account (`login-reveals-whether-an-account-exists.md`),
which tells an attacker which addresses are worth targeting.
