# Client Module — Show the linked booklets on each regular transaction

**Context**
On the regular transactions page nothing tells which booklets a recurring entry is linked to: the
user has to open the "Delier" dialog just to find out. The link and unlink buttons are also disabled
without explanation when there is nothing to link, and the explaining tooltip only exists on desktop.

**Acceptance Criteria**
Feature: Visible booklet links
  In order to know where a recurring entry applies
  As an authenticated user
  I want the linked booklets displayed on the row

Scenario: 1. Linked booklets are listed on the row
  Given a regular transaction linked to two booklets
  When the list is rendered
  Then the row displays the name of both booklets

Scenario: 2. An unlinked entry is marked as such
  Given a regular transaction linked to no booklet
  When the list is rendered
  Then the row states that it is not linked to any booklet

Scenario: 3. A disabled link action explains why
  Given a regular transaction already linked to every booklet
  When I focus the link action
  Then an explanation is available on desktop and on mobile

**Notes**
- Files: `pages/regular-transaction/index.vue`.
- Priority P1 - Effort S - Frontend only.
