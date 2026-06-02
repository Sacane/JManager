# Application Module — Error Diagnostic Context in API Error Responses

**Context**
When an error occurs, users need to report it to support. Currently, error responses contain only a
numeric code and a message, providing no stable identifier that support can use to locate the
incident in logs or the database.

The project already has an MDC infrastructure (`MdcContextProvider`, `MdcKeys`, `LoggingCommandBus`,
`LoggingQueryBus`) that injects domain context keys (`bookletId`, `transactionId`) into the SLF4J
MDC during bus dispatch. However, those keys are **removed in the `finally` block** before the
exception propagates to `ProblemDetailHandler` — so they are not available at error-handling time.

The right strategy is to generate a `requestId` UUID at the HTTP boundary (servlet filter), store it
in MDC for the **full request lifetime** (including in exception handlers), and expose it in every
error response. Support can then grep logs by `requestId` and find all lines for that request —
including the ones where `bookletId` and `transactionId` were still in MDC during dispatch.

**Scope (application)**
- Add `REQUEST_ID` to `MdcKeys` in the domain (`domain/port/input/MdcContext.kt`).
- Create a servlet filter (e.g. `RequestIdFilter`) that generates a UUID v4 per request, stores it
  via `MDC.put(MdcKeys.REQUEST_ID, ...)`, and removes it in a `finally` block after the response
  is committed. This filter must run before Spring Security so the key is present even for 401/403
  responses.
- Update `ProblemDetailHandler.buildResponse()` to read `MDC.get(MdcKeys.REQUEST_ID)` and attach it
  as a custom property `"requestId"` on the `ProblemDetail` when non-null.
- Extract the authenticated `userId` from the Spring Security context and attach it as `"userId"` on
  the `ProblemDetail` when a principal is present.
- No domain use-case or infrastructure persistence changes are required.

**Acceptance Criteria**
Feature: Error diagnostic context in API error responses
  In order to locate the failing request quickly in logs and the database
  As a support engineer
  I want every error response to carry a stable requestId and the affected userId

Scenario: Authenticated request fails — both fields are present
  Given a servlet filter that set requestId in MDC before the request reached the controller
  And an authenticated user whose request triggers a domain failure
  When ProblemDetailHandler returns a 4xx or 5xx ProblemDetail
  Then the response JSON includes a non-null "requestId" UUID field
  And the response JSON includes a "userId" field matching the authenticated user's ID
  And the server log line from logException includes the requestId in the MDC output

Scenario: Unauthenticated request fails — requestId only
  Given a request without a valid authentication token that triggers a 401 response
  When ProblemDetailHandler returns the ProblemDetail
  Then the response JSON includes a non-null "requestId" UUID field
  And the response JSON does NOT include a "userId" field

Scenario: Every failing request gets a distinct requestId
  Given two concurrent requests that both fail
  When both ProblemDetail responses are returned
  Then each response carries a different "requestId" value

Scenario: Successful requests do not expose requestId
  Given an authenticated user whose request succeeds
  When the API returns a 2xx response
  Then the response body does not contain a "requestId" field

Scenario: requestId links error response to domain context in logs
  Given a command that implements MdcContextProvider exposing bookletId and transactionId
  And the command fails with a domain error
  When the ProblemDetail is returned with its requestId
  Then the server logs contain lines with requestId, bookletId, and transactionId
  So that support can grep by requestId to retrieve the full domain context
