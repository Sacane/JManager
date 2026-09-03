# UX-24 — Knowing where a recurring entry applies

**Context**
Functional acceptance for UX-24, independent of any layer. Finding out which booklets a recurring entry is linked to currently requires opening the unlink dialog.

**Acceptance Criteria**
Feature: Knowing where a recurring entry applies
  In order to understand my recurring entries
  As an authenticated user
  I want the linked booklets shown on each entry

Scenario: Linked booklets are visible at a glance
  Given a regular transaction linked to one or more booklets
  When I look at the list
  Then the linked booklets are displayed on its row

Scenario: An unlinked entry is explicit
  Given a regular transaction linked to no booklet
  When I look at the list
  Then it states that it is not linked to any booklet

Scenario: A disabled action explains itself
  Given an action is unavailable on an entry
  When I try to use it
  Then I am told why, on desktop and on mobile alike

**Notes**
- Layer-agnostic functional acceptance for UX-24. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
