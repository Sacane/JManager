# Lot 5 — Manual validation walkthroughs

The test suite runs on happy-dom, which has no layout or rendering engine. Anything about
position, overlap, reading order or visual weight ships unverified until someone looks at it.
This lot is almost entirely about those things.

Mark each step ✅ or ❌. A ❌ is worth reporting with the viewport width and the theme in use.

**Delivered:** UX-44, UX-18, UX-28 — the lot is complete.

---

## 1. The new layout (UX-18)

Open the dashboard with at least one booklet holding transactions.

| # | Step | Expected |
|---|---|---|
| 1.1 | Look at the top of the page | Greeting, then a subtitle "Vue septembre 2026 (dd/mm/yyyy → dd/mm/yyyy) • Livret A", then the account selector and the period controls |
| 1.2 | Look below the header | The quick actions, then a two-tab switch: **Vue d'ensemble** / **Analyse** |
| 1.3 | Read down the overview | Three headed zones in this order: **Où j'en suis**, **Ce qui arrive**, **Où part l'argent** |
| 1.4 | Zone 1 | Five figures — Solde, Dépenses, Revenus, Taux d'épargne, Projection fin de période — then the evolution chart beside the budget |
| 1.5 | Zone 2 | Prochaines transactions and Alertes de la période, side by side on a wide screen |
| 1.6 | Zone 3 | Dépenses par catégorie, with its sub-tag drill-down and the top tags |
| 1.7 | Click **Analyse** | The overview is replaced by the period comparison chart |
| 1.8 | Click **Vue d'ensemble** | The three zones come back |

⚠️ **1.3 is the point of the item.** Judge whether the page can now be read in a few seconds. If a
zone feels in the wrong place or a block in the wrong zone, say which — moving a block is cheap now.

---

## 2. What is gone, on purpose (UX-18)

| # | Step | Expected |
|---|---|---|
| 2.1 | Look at the header | The four pills (Période, À venir, Solde prévisionnel court terme, Projection) are gone |
| 2.2 | Look for "Tags populaires" | Gone. It listed the first six tags in list order, not the most used |
| 2.3 | Look for "Mes livrets" | Gone from the dashboard. Reordering still works on the Livrets page |
| 2.4 | Scroll to the bottom | The stats banner (Tags créés, Transactions prévisionnelles…) is gone |
| 2.5 | Look under the savings rate | "Objectif : 30 %" is replaced by "de vos revenus du mois" |

---

## 3. Every figure once (UX-18)

| # | Step | Expected |
|---|---|---|
| 3.1 | Find the end-of-period projection | Once, as the fifth figure of zone 1 |
| 3.2 | Find the upcoming count and net | Once, as the line under "Prochaines transactions": "N transaction(s) sur les 15 jours à venir · net X €" |
| 3.3 | Compare that net with the old pill value, if you remember it | Same figure, one place |
| 3.4 | Find the exact period dates | In the subtitle, in brackets |

---

## 4. Things only an eye can check (UX-18)

| # | Step | Expected |
|---|---|---|
| 4.1 | Scroll the overview | Zones 1 and 3 fade in as before; **nothing stays invisible** |
| 4.2 | Open **Analyse** | The comparison chart is fully visible straight away — not stuck faded out |
| 4.3 | At 1280 px | The evolution chart takes two thirds of the width, the budget one third |
| 4.4 | At 375 px | Zones stack, the five figures wrap, the tab switch fits |
| 4.5 | Dark theme | Zone titles, tabs and the upcoming summary stay legible |
| 4.6 | Keyboard: Tab to the switch | Focus lands on the **active** tab only — a single Tab stop for the pair |
| 4.7 | Press the right arrow, then the left arrow | The view switches each time and focus follows the selected tab |
| 4.8 | With a screen reader, reach the switch | It announces a tab list and "onglet 1 sur 2, sélectionné" or equivalent |

⚠️ **4.2 is the one to watch.** The fade-in starts content at zero opacity until it scrolls into
view. It was kept off the analysis tab precisely so the comparison could not stay invisible — confirm.

---

## 5. All accounts mode (UX-44)

With **at least two** booklets, and one of them set to a cycle starting on the 25th in Settings.

| # | Step | Expected |
|---|---|---|
| 5.1 | Open the account selector | **Tous les comptes** is the first option |
| 5.2 | Select it | The subtitle ends with "• Tous les comptes" |
| 5.3 | Look at the balance | The total of every booklet — add them up on the Livrets page to check |
| 5.4 | Look at the period dates in the subtitle | 1st to last day of the month (e.g. 01/09/2026 → 30/09/2026) |
| 5.5 | Switch back to the booklet with the cycle on the 25th | The dates become 25/08 → 24/09 |
| 5.6 | Check the upcoming list and the expenses | They include entries from every booklet |
| 5.7 | Look at zone 1 in "Tous les comptes" | The account budget block is absent |
| 5.8 | Look at the quick actions | Only "Créer une transaction régulière" remains |
| 5.9 | Reload the page | Still on "Tous les comptes" |
| 5.10 | With **a single** booklet | The option does not appear at all |

⚠️ **5.4 is the product decision of 19 September 2026.** Cycles are per booklet, so the aggregated
view uses the calendar month. If it reads oddly next to a booklet on a 25th cycle, that is expected;
tell me if it should behave otherwise.

⚠️ **5.6 depends on the backend.** The aggregated figures come from the stats endpoints called without
a booklet id — the path the JPA fix of 29 August made reliable. A total that looks too high is the
symptom that fix removed; report it if you see one.

---

## 6. Dashboard on the design system (UX-28)

Nothing here should be noticeable except one thing, so this walkthrough is mostly about confirming
that **nothing changed**. The dashboard's colours moved out of 64 inline `style` attributes and into
the design system; the values themselves are the same tokens as before.

| # | Step | Expected |
|---|---|---|
| 6.1 | Read the whole dashboard, light theme | Every figure, label, hint and panel has the colour it had yesterday — no grey text turned black, no panel lost its background |
| 6.2 | Switch to the dark theme | Same, in dark. Nothing keeps a light-theme colour |
| 6.3 | Look at the block titles | They are all the same size and weight. **Three of them are slightly smaller than before** — "Évolution des finances", "Dépenses par catégorie" and the comparison title in the Analyse tab |
| 6.4 | Look at the end-of-period projection figure | Still green or red according to the projection, and the same size as the four figures beside it |
| 6.5 | Look at the account selector, top right | Its border and background are unchanged |
| 6.6 | Look at the "Prochaines transactions" header | Its bottom rule is still a 2px line |
| 6.7 | Look at "Top tags de la période" | Each tag keeps its own colour — the dot and the tag name |

⚠️ **6.3 is the only intended change.** Eight titles were written five different ways for one role;
they are unified on the form five of them already used. If you would rather have the larger size
everywhere, say so — it is one value in `unocss.config.ts`.

❗️ **6.7 is the one worth a real look.** The tag colours are the only styles on this page still set
from JavaScript. If a tag chip shows the wrong colour, or none, that is this change.

---

## 7. Monthly cycle card, overflow fix (UX-41 follow-up)

Reported from the settings page on 20 September 2026: the **Fin** select was cut off by the right
edge of its card. The cycle box asked for a 240px minimum for the two selects plus the width of the
longest date beside them — about 18px more than the card offers when the settings grid shows three
columns.

Open **Paramètres**, with at least one account.

| # | Step | Expected |
|---|---|---|
| 7.1 | Look at the "Cycle mensuel par compte" card | Nothing is clipped: both selects are fully inside the rounded box, which is fully inside the card |
| 7.2 | Widen and narrow the window slowly | The Début/Fin pair drops below the date line when there is no room beside it, and comes back up when there is — never overlapping, never cut |
| 7.3 | Open the **Fin** select | The options open over the page, not clipped by the card |
| 7.4 | An account with a long name | The name wraps on its own line above the dates; the selects stay put |
| 7.5 | At 375 px | The card, the dates and both selects fit without a horizontal scrollbar |

⚠️ **The suite cannot see this one.** happy-dom has no layout engine, so no test can fail on an
overflow. The fix was measured on a standalone reproduction of the card at the reported width:
18px of overflow before, 0 after. Your eyes are the only check on the real page.

---

## Priority cases

If time is short, do these six:

- **1.3** — the reading order is the whole point of UX-18.
- **4.2** — the analysis tab must not show a chart stuck faded out.
- **3.2** — the upcoming figures appear once, under their own list.
- **5.3 / 5.4** — the aggregated balance and the calendar month.
- **5.6** — aggregated figures cover every booklet, without being inflated.
- **6.3** — the block titles, the one intended visual change.
- **7.1** — nothing clipped in the monthly cycle card.
