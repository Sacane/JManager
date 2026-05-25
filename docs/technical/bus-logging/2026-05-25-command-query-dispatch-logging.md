# Command/Query Dispatch Logging

> **Topic**: Observability — Bus-level tracing
> **Date**: 2026-05-25
> **Author**: Technical Backend

---

## Context

JManager uses a Command/Query bus pattern to dispatch write and read operations to their respective handlers.
Currently no individual dispatch is logged, making it impossible to trace which operations were executed, how long
they took, or whether they succeeded. Adding generic logging at the bus level provides a single cross-cutting
observation point without touching any handler or controller.

---

## Current State

### `SpringCommandBus` / `SpringQueryBus` — two problems

**Problem 1 — `java.util.logging.Logger` instead of SLF4J**

Both buses use the JDK logger:
```kotlin
private val LOGGER = Logger.getLogger(SpringCommandBus::class.java.name)
```

Spring Boot includes the `jul-to-slf4j` bridge, so logs do flow through Logback eventually — but:
- The SLF4J parameterized syntax (`{}`) is unavailable, so string concatenation is used instead.
- Log levels don't map cleanly (JUL `INFO` ≠ SLF4J `INFO` after bridging).
- The `logback-spring.xml` logger name filters work on SLF4J logger names, not JUL names.

**Problem 2 — No per-dispatch logging**

`dispatch()` in both buses contains zero logging. There is no way to know post-hoc:
- Which command/query was executed
- Whether it succeeded or failed (and with which `ResultState`)
- How long the handler took

---

## Analysis

### Option A — Modify `dispatch()` directly

Add `INFO` logging inside the existing `dispatch()` methods. Simple, zero new files.

**Rejected** — mixes the dispatch responsibility (routing) with the observability concern (logging).
Violates SRP. Also makes it harder to later add metrics or MDC enrichment without touching the router.

### Option B — Spring AOP `@Around` advice

Intercept `CommandBus.dispatch()` and `QueryBus.dispatch()` via a pointcut.

**Rejected** — AOP proxy-based magic is harder to trace during debugging and adds CGLIB complexity.
The bus classes are not Spring beans requiring transparent interception; they are owned infrastructure
where the Decorator is explicit and readable.

### Option C — Decorator pattern ✅ (recommended)

Create `LoggingCommandBus` and `LoggingQueryBus`, each implementing the respective bus interface and
delegating to the real implementation. Spring wires the decorator as `@Primary`.

**Chosen** because:
- Single Responsibility: the decorator owns logging, the real bus owns routing.
- Explicit: the delegation chain is visible in code.
- Extensible: adding MDC enrichment or Micrometer timing later means touching only the decorator.
- Zero domain contamination: purely in the `application` layer.

### What to log

Each dispatch emits one `INFO` log line:

```
COMMAND dispatched | SaveBookletCommand          | OK              |  12 ms
COMMAND dispatched | DeleteBookletByIdCommand    | BOOKLET_NOT_FOUND |   3 ms
QUERY  dispatched  | FindAllBookletsQuery         | OK              |   8 ms
QUERY  dispatched  | FindBookletByIdQuery         | NOT_FOUND       |   2 ms
```

- **Command/Query name**: `command::class.simpleName` — sufficient for tracing, no sensitive data.
- **Result state**: `result.status.name` — maps to `ResultState` enum, always meaningful.
- **Duration**: wall-clock ms via `System.nanoTime()` — measures handler execution only.

No payload logging: command/query fields may contain user data (amounts, labels). Logging them
would be a privacy concern and would bloat logs.

---

## Recommended Approach

### 1. Fix `SpringCommandBus` and `SpringQueryBus` — switch to SLF4J

Replace `java.util.logging.Logger` with `org.slf4j.LoggerFactory` in both buses.
No dependency change needed: `slf4j-api` is already on the classpath via `spring-boot-starter-web`.

### 2. Create `LoggingCommandBus` and `LoggingQueryBus`

Decorators in `application/src/main/kotlin/fr/sacane/jmanager/application/bus/`.
Annotated `@Primary` so Spring injects them wherever `CommandBus`/`QueryBus` is needed.
They inject the concrete `SpringCommandBus` / `SpringQueryBus` directly to avoid circular dependency.

### Why this approach

The buses are pure application infrastructure. The decorator keeps the routing logic untouched and
creates a named, testable seam for observability. If Micrometer metrics are added later, the decorator
absorbs them without touching `SpringCommandBus`.

---

## Implementation Notes

### Files to modify

| File | Change |
|---|---|
| `application/…/bus/CommandBus.kt` | Replace JUL logger with SLF4J |
| `application/…/bus/QueryBus.kt` | Replace JUL logger with SLF4J |

### Files to create

| File | Role |
|---|---|
| `application/…/bus/LoggingCommandBus.kt` | Decorator — logs command dispatches |
| `application/…/bus/LoggingQueryBus.kt` | Decorator — logs query dispatches |

### No new dependencies

`slf4j-api` is transitively provided by `spring-boot-starter-web`. No `build.gradle.kts` change needed.

### Code

**`LoggingCommandBus.kt`**
```kotlin
@Component
@Primary
class LoggingCommandBus(
    private val delegate: SpringCommandBus
) : CommandBus {

    private val log = LoggerFactory.getLogger(LoggingCommandBus::class.java)

    override fun <R> dispatch(command: Command<R>): Result<R> {
        val name = command::class.simpleName ?: "UnknownCommand"
        val start = System.nanoTime()
        val result = delegate.dispatch(command)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("COMMAND dispatched | {:<45} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }
}
```

**`LoggingQueryBus.kt`**
```kotlin
@Component
@Primary
class LoggingQueryBus(
    private val delegate: SpringQueryBus
) : QueryBus {

    private val log = LoggerFactory.getLogger(LoggingQueryBus::class.java)

    override fun <R> dispatch(query: Query<R>): Result<R> {
        val name = query::class.simpleName ?: "UnknownQuery"
        val start = System.nanoTime()
        val result = delegate.dispatch(query)
        val ms = (System.nanoTime() - start) / 1_000_000
        log.info("QUERY  dispatched | {:<45} | {:<30} | {} ms", name, result.status.name, ms)
        return result
    }
}
```

---

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Two beans of same type in Spring context | Low | `@Primary` on decorators ensures correct injection; `SpringCommandBus` is only injected explicitly in the decorator |
| `simpleName` is null for anonymous classes | Low | Fallback to `"UnknownCommand"` / `"UnknownQuery"` via Elvis operator |
| Logging every dispatch in prod (high-traffic) | Low | `INFO` level on `fr.sacane.jmanager` is already the configured minimum in `logback-spring.xml`; bus logs are one line per request, negligible volume for a small SaaS |
| No payload logging | Intentional | Privacy — command fields may contain user data |

---

## References

- [SLF4J Manual — Logger usage](https://www.slf4j.org/manual.html)
- [Decorator pattern — GoF](https://refactoring.guru/design-patterns/decorator)
- [Spring `@Primary` bean qualifier](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-primary)
