# Client Module — Attach each cycle to its account and preview the period it covers

**Context**
In the "Cycle mensuel par compte" section of `pages/settings/index.vue`, each account is a row with its
name on the left and two day selectors on the right. The name is truncated with an ellipsis, so two
accounts sharing a prefix become indistinguishable, and the rows are otherwise identical. Nothing shows
which dates a cycle covers: the user has to derive them from the help text.

That help text states that the start day "s'applique au mois précédent du mois affiché". It does not,
for start days 1 to 15. `resolveMonthlyCycleRangeForTargetMonth` anchors a month on its 15th: the
period of a month is the cycle that contains its 15th. For September, a start on the 25th gives
25/08 → 24/09, but a start on the 10th gives 10/09 → 09/10. The page therefore described a rule that
contradicts what the dashboard computes.

The rule itself is not changed here — the dashboard and every figure derived from it rely on it. It is
made visible instead.

**Acceptance Criteria**
Feature: Monthly cycle preview per account
  In order to know exactly which dates each account's cycle covers
  As an authenticated user
  I want each cycle named after its account and previewed as a date range

Scenario: 1. The account name is never truncated
  Given an account with a long name
  When the cycle settings are rendered
  Then the full name is displayed, wrapping onto several lines if needed

Scenario: 2. Each cycle shows the period of the current month
  Given an account whose cycle starts on the 25th with no custom end
  When the cycle settings are rendered in September 2026
  Then the preview reads the period from 25/08/2026 to 24/09/2026

Scenario: 3. A start day in the first half of the month is previewed correctly
  Given an account whose cycle starts on the 10th with no custom end
  When the cycle settings are rendered in September 2026
  Then the preview reads the period from 10/09/2026 to 09/10/2026

Scenario: 4. A custom end day is reflected in the preview
  Given an account whose cycle starts on the 25th and ends on the 20th
  When the cycle settings are rendered in September 2026
  Then the preview reads the period from 25/08/2026 to 20/09/2026

Scenario: 5. The preview updates as the start day changes
  Given the cycle settings are rendered
  When I select another start day for an account
  Then that account's preview updates without saving
  And the other accounts' previews are unchanged

Scenario: 6. The preview uses the dashboard's own computation
  Given any start day and end day
  When the preview is computed
  Then it equals what the dashboard computes for the same month and the same cycle

Scenario: 7. The help text no longer states a false rule
  Given the cycle settings are rendered
  When I read the explanation
  Then it does not claim that the start always falls in the previous month

**Notes**
- Merged with the Trello card "cycle mensuel du compte" (https://trello.com/c/b10ayHGe).
- Files: `pages/settings/index.vue`. Reuse `resolveMonthlyCycleRangeForTargetMonth` from
  `utils/monthlyCycleRange.ts` — a second implementation would let the preview and the dashboard
  disagree, which is the defect this item removes.
- Priority P2 - Effort M - Frontend only.
