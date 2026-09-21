# The sign-in rate limit is probably keyed on the proxy's address

## Observation

`LoginRateLimiter` blocks a key after 5 failed sign-ins in 15 minutes, and `SessionController.login`
uses `httpRequest.remoteAddr` as the key. Production runs behind a reverse proxy (confirmed by the
product owner on 21 September 2026), and no property sets `server.forward-headers-strategy`. Unless
Spring Boot detected a cloud platform and enabled it on its own, `remoteAddr` is the proxy's address
for every visitor.

**Not confirmed** — this is read from the configuration, not observed.

## How to confirm

The limiter logs the key it blocks: `Rate limit exceeded for IP …`. One such line in production logs
showing the proxy's address, or the same address for unrelated users, settles it.

## Location

- `application/.../api/session/LoginRateLimiter.kt`
- `application/.../api/session/Controller.kt` (`login`)
- `application/src/main/resources/application*.properties`

## Expected behaviour

The limiter keys on the client's address, taken from the proxy's `X-Forwarded-For` through
`server.forward-headers-strategy=native` — with the proxy **overwriting** that header, otherwise a
client can forge it and escape the limit.

## Impact

If confirmed: five wrong passwords from anyone lock sign-in **for everyone** for 15 minutes, and the
limit does nothing against a single attacker. UX-21 (application module) sets the strategy for its own
limits, which repairs this as a side effect; this note exists in case UX-21 is delayed.
