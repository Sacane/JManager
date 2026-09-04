# Client Module — Bound the centered card to the viewport width

**Context**
The card in `layouts/centercard.vue` is `w-100 sm:w-125 lg:w-170` with `p-10`. `w-100` is a fixed 25rem with no `max-width`, so below 400px of viewport the card is wider than the screen and the body scrolls sideways. Four screens use this layout: consent, verify-email, force-password-change and the not-found page.

**Acceptance Criteria**
Feature: Centered card bounded by the viewport
  In order to avoid a horizontal scrollbar on small phones
  As a developer
  I want the card width capped relative to the viewport

Scenario: The card declares an upper bound
  Given the centercard layout
  When I inspect the card element
  Then its width is capped relative to the viewport width

Scenario: The padding is accounted for
  Given a viewport narrower than the card base width
  When the card is rendered
  Then the card and its padding stay within the viewport

Scenario: The responsive widths are preserved
  Given the centercard layout
  When I inspect the card element
  Then the small and large breakpoint widths are unchanged

**Notes**
- Files: `layouts/centercard.vue`.
- `max-w-[calc(100vw-2rem)]` keeps a gutter on each side; the fixed widths stay for wider screens.
- Priority P2 - Effort XS - Frontend only.
