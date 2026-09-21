# The sign-in rate limit is probably keyed on the proxy's address

## Observation

`LoginRateLimiter` blocks a key after 5 failed sign-ins in 15 minutes, and `SessionController.login`
uses `httpRequest.remoteAddr` as the key. Production runs behind a reverse proxy (confirmed by the
product owner on 21 September 2026), and no property sets `server.forward-headers-strategy`. Unless
Spring Boot detected a cloud platform and enabled it on its own, `remoteAddr` is the proxy's address
for every visitor.

**Confirmed on 21 September 2026** from the production log: `Recorded failed login attempt for …` shows an
address starting with `172`, a private (Docker) range — the reverse proxy's. Every visitor shares one key.

## How to confirm

The limiter logs the key it blocks: `Rate limit exceeded for IP …`. One such line in production logs
showing the proxy's address, or the same address for unrelated users, settles it.

## Location

- `application/.../api/session/LoginRateLimiter.kt`
- `application/.../api/session/Controller.kt` (`login`)
- `application/src/main/resources/application*.properties`

## Expected behaviour

The limiter keys on the client's address, taken from the proxy's `X-Forwarded-For` through
`server.forward-headers-strategy=native`. The production nginx appends the client address
(`$proxy_add_x_forwarded_for`), which is safe with `native`: Tomcat reads the header from the right and
skips trusted proxies, so a forged value on the left is ignored — as long as nginx's address, as Spring
sees it, is in a private range or in `server.tomcat.remoteip.internal-proxies`. The `framework`
strategy would read from the left and must not be used.

## Impact

Five wrong passwords from anyone lock sign-in **for everyone** for 15 minutes, so anyone can lock everyone
out with five requests per 15 minutes. It is not a per-client limit at all. Fixing the key also removes the
accidental global brake on guessing one account's password: once the key is the client address, an attacker
who changes address is no longer limited, and no per-account limit exists. UX-21 (application module) sets the strategy for its own
limits, which repairs this as a side effect; this note exists in case UX-21 is delayed.

**Also untested**: no test in `application/src/test` exercises the 429 path or `LoginRateLimiter` (checked
on 21 September 2026 — the words "rate" and "429" appear in none of them). UX-21 generalises this class,
so its refactor needs a characterisation test first.

**Reading the logs**: `Rate limit exceeded for IP …` is a `WARNING` emitted only on the request that
follows five recorded failures. Each failure logs at `INFO` (`Recorded failed login attempt for …`), and
an unknown address logs nothing at `WARN` at all — `LoginService` warns only for a known account. Looking
for a warning after one or two attempts finds nothing even when everything works.
