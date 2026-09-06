# Client Module — Explain the booklet limit

**Context**
`pages/booklet/index.vue` swaps the create button for a disabled "Limite atteinte" one when `data.length >= 6`. The number 6 is repeated four times in the template as a literal, and no tooltip or hint explains the cap. The count badge shows `n/6` but nothing states what happens at the cap.

**Acceptance Criteria**
Feature: Explained booklet limit
  In order to avoid a dead end at the cap
  As a developer
  I want the limit surfaced with its reason and a way forward

Scenario: The disabled action carries an explanation
  Given the booklet count reached the maximum
  When the creation action renders
  Then an accessible explanation is attached to it

Scenario: The explanation names the way forward
  Given the booklet count reached the maximum
  When I read the explanation
  Then it mentions freeing a slot by deleting a booklet

Scenario: The maximum is declared once
  Given the booklets page source
  When I search for the maximum value
  Then it comes from a single named constant

**Notes**
- Files: `pages/booklet/index.vue`, `constants/`.
- Extract the literal 6 into a named constant; it currently appears four times in the template.
- Priority P2 - Effort XS - Frontend only.
