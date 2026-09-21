# The JWT filter reports any downstream exception as a 401

## Observation

`JwtCookieAuthenticationFilter.doFilterInternal` wraps **the whole rest of the chain** in its `try`:

```kotlin
try {
    ...authenticate...
    filterChain.doFilter(request, response)      // everything after this filter runs in here
} catch (ex: Exception) {
    LOGGER.warning("Authentication filter error: ${ex.javaClass.simpleName}")
    response.status = HttpServletResponse.SC_UNAUTHORIZED
    ...
}
```

Any exception that propagates out of a later filter or out of the servlet — not one handled by a
`@ExceptionHandler`, which never reaches the filter — is caught there and answered as **401
"Unauthorized"**, whatever its cause.

Observed on 21 September 2026 while fixing the SPA routes: a route pattern that Spring refused at match
time (`IllegalArgumentException: No capture groups allowed in the constraint regex`) made **every** request
answer 401, public pages and the home page included. A server-side defect read as "your session is bad".

## Location

`application/src/main/kotlin/fr/sacane/jmanager/application/api/session/Filter.kt`, the `try` / `catch`
around `filterChain.doFilter`.

## Expected behaviour

Only the authentication step is guarded. An exception thrown by the rest of the chain is not this filter's
to answer: it propagates, and ends as a 500 through the normal error handling. Only a token that cannot be
read — the part that genuinely means "unauthorized" — produces the 401.

## Impact

Medium, and it hides other things:

- server errors are answered and logged as authentication failures (a `WARNING`, not an `ERROR`), so they
  do not look like errors at all;
- the log line carries only the exception's class name, not the message, so the cause is hard to find;
- a client that receives 401 may sign the user out or redirect to the login page, turning a temporary
  server fault into a lost session.

The fix is small: move `filterChain.doFilter` out of the `try`, keeping the token handling inside it, with
a test that a downstream exception is not answered as 401.
