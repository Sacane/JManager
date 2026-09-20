# UX-28 — A dashboard that follows the design system

**Context**
Functional acceptance for UX-28, independent of any layer. The dashboard sets most of its colours
and surfaces through `style` attributes written directly in the template — 64 static ones — instead
of the design system every other page now uses. Nothing is broken on screen today, but the
consequences are real for the user: a style attribute wins over any class, so a theme change or a
contrast fix applied to the design tokens cannot reach these blocks, and the same role has drifted
into several appearances on one page. Eight block titles are written five different ways, in three
sizes and two weights.

**Acceptance Criteria**
Feature: A dashboard that follows the design system

  In order to keep the dashboard consistent with the rest of the application
  As an authenticated user
  I want its blocks to share one appearance per role

Scenario: Blocks of the same kind look the same
  Given I am on the dashboard
  When I compare the titles of its blocks
  Then they share one size, one weight and one colour

Scenario: A theme change reaches the whole dashboard
  Given the application theme defines the text and surface colours
  When I switch between the light and dark themes
  Then every dashboard block follows the theme, with no block keeping the other theme's colours

Scenario: The dashboard keeps its current appearance otherwise
  Given I knew the dashboard before this change
  When I open it again
  Then the figures, their order and their layout are unchanged

**Notes**
- Layer-agnostic functional acceptance for UX-28, the last item of lot 5. It describes what the
  user gets, not how it is built.
- This is a coherence item, not a redesign: the only intended visible change is that the three
  oversized block titles come down to the size the other five already use.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
