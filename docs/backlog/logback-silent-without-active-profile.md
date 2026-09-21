# Logback logs nothing at all when no Spring profile is active

## Observation

`application/src/main/resources/logback-spring.xml` declares its appenders, levels and root logger
**only inside `<springProfile>` blocks** (`local,test` and `prod`). When none of those profiles is active,
the file is read ("End of configuration") but defines no appender and no root configuration, so the
application logs **nothing**: no startup line, no request log, no warning, and no message saying that
logging is unconfigured.

Reproduced on 21 September 2026 by running `SessionControllerTest` twice, once as is and once with
`SPRING_PROFILES_ACTIVE=test`:

| Run | Application log lines |
|---|---|
| No profile (how the tests run today) | none — `Start authenticate user`, `Recorded failed login attempt` and the `WARN` from `LoginService` are all absent |
| `SPRING_PROFILES_ACTIVE=test` | all present, with level and `[req=…]`: the JUL loggers of `SessionController` and `LoginRateLimiter` are correctly bridged to Logback |

So the bridging and the patterns are fine; the only condition is that a profile is active.

Nothing in the repository activates one: `application.properties` has no `spring.profiles.active`, the
Gradle `bootRun` task passes no argument, and there is no IDE run configuration or script. The README
tells developers to run `:infrastructure:bootRun` (the other Gradle documents say `:application:bootRun`)
and does not mention a profile. A developer who does not set `SPRING_PROFILES_ACTIVE=local` gets a silent
application, and — since `application-local.properties` is the profile file — no local configuration either.

## Location

- `application/src/main/resources/logback-spring.xml`
- `application/build.gradle.kts` (`bootRun`)
- `README.md` (run instructions)

## Expected behaviour

Running the application without a profile still logs to the console at INFO, and the run instructions
name the profile to use.

## Impact

Medium for anyone diagnosing something locally: a missing log line looks like a missing feature. It also
hides test-run output, so a failing controller test has no application logs to read.

## Fix candidates

- Add a fallback `<springProfile name="!local &amp; !test &amp; !prod">` with a console appender at INFO
  (`&` must be escaped in the XML).
- Or default the profile: `spring.profiles.default=local` for `bootRun`, leaving `prod` to be set
  explicitly in the deployment.
- Fix the README run command and state the profile.
