# UX-45 — The booklet limit explains itself

**Context**
The booklets page caps creation at six. Once reached, the action turns into a disabled "Limite atteinte" button with no explanation and no way forward: the user is not told why the limit exists nor what to do about it.

**Acceptance Criteria**
Feature: The booklet limit explains itself
  In order to understand why I cannot create a booklet
  As a user at the limit
  I want the interface to tell me why and what I can do

Scenario: The limit is explained
  Given I already have the maximum number of booklets
  When I look at the creation action
  Then it explains that the limit is reached and why

Scenario: A way forward is offered
  Given I already have the maximum number of booklets
  When I read the explanation
  Then it tells me that deleting a booklet frees a slot

Scenario: Below the limit nothing changes
  Given I have fewer booklets than the maximum
  When I look at the creation action
  Then it is enabled and shows the remaining slots

**Notes**
- Layer-agnostic functional acceptance for UX-45. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
