# Lot 4 — Manual validation walkthroughs

The test suite runs on happy-dom, which has no layout or rendering engine. Anything about
position, overlap, touch target size or visual state ships unverified until someone looks at it.
These walkthroughs cover what the suite cannot.

Mark each step ✅ or ❌. A ❌ is worth reporting with the viewport width and the theme in use.

**Delivered so far:** UX-15, UX-16, UX-19, UX-24.
**Still to come in this lot:** UX-20, UX-25, UX-26, UX-41, UX-50.

---

## 1. Regular transactions — row actions on desktop (UX-15)

Open **Mes transactions régulières** on a desktop-sized window, with at least two entries.

| # | Step | Expected |
|---|---|---|
| 1.1 | Look at the Actions column of any row | Four controls: edit (pencil), delete (bin), link, unlink |
| 1.2 | Hover the pencil | Tooltip "Modifier" |
| 1.3 | Hover the bin | Tooltip "Supprimer" |
| 1.4 | Check the column is not cut off | The frozen Actions column shows all four controls without horizontal clipping |
| 1.5 | Click the pencil | The edit dialog opens on that entry, with its values loaded |
| 1.6 | Close it, then double click the row | The same dialog opens — the shortcut still works |
| 1.7 | Click the bin on an entry named, say, "Loyer" | The confirmation names **« Loyer »**, not "cette transaction régulière" |
| 1.8 | Cancel | Nothing is deleted, the row is still there |
| 1.9 | Click the bin again, confirm | The row disappears and a success toast is shown |
| 1.10 | Delete an entry linked to at least one booklet | The confirmation names the entry **and** states how many booklets lose their link |

⚠️ **1.4 is the one to watch.** The Actions column was sized for two controls and now holds four.
If it clips or pushes the table into a horizontal scroll, say so.

---

## 2. Regular transactions — row actions on mobile (UX-15)

Same page, viewport at 375 px wide (or a real phone). Reload after resizing.

| # | Step | Expected |
|---|---|---|
| 2.1 | Look at a card | A row of buttons at the bottom: Modifier, Supprimer, Lier, and Délier when applicable |
| 2.2 | Check they fit | The buttons wrap onto a second line rather than overflowing the card |
| 2.3 | Tap each button | The target is comfortable to hit — no mis-taps on the neighbour |
| 2.4 | Tap Modifier | The edit dialog opens |
| 2.5 | Tap Supprimer | The confirmation names the entry |
| 2.6 | Confirm | The card disappears, success toast |

---

## 3. Regular transactions — selecting versus opening (UX-16)

Same page, mobile viewport. **This is the behaviour change most likely to surprise you.**

| # | Step | Expected |
|---|---|---|
| 3.1 | Tap the body of a card (not a button) | The card becomes visibly selected. The edit dialog does **not** open |
| 3.2 | Tap the same card body again | It is deselected |
| 3.3 | Select two cards | Both show the selected styling |
| 3.4 | Look at the top of the page | A red "Supprimer la sélection (2)" button has appeared |
| 3.5 | Deselect everything | That button disappears again |
| 3.6 | Select two, tap the bulk delete, confirm | Both are deleted |
| 3.7 | Tap Modifier on a selected card | The editor opens; the selection is not disturbed |

⚠️ **3.1 is a deliberate break with the previous behaviour.** Tapping a card used to open the
editor. It now selects. Judge whether that feels right — the alternative is that a phone can never
build a selection, which is what made the bulk delete unreachable there.

---

## 4. Regular transactions — desktop selection unchanged (UX-16 regression)

| # | Step | Expected |
|---|---|---|
| 4.1 | On desktop, tick two checkboxes | "Supprimer la sélection (2)" becomes enabled |
| 4.2 | Untick both | The button is still visible but disabled — unchanged from before |
| 4.3 | Double click a row | The editor opens |

---

## 5. Booklet detail — pinned behaviour, nothing changed (UX-16)

The issue claimed this page offered no obvious way to open a transaction on mobile. It was wrong:
the controls were already there. These steps confirm nothing regressed.

| # | Step | Expected |
|---|---|---|
| 5.1 | Open a booklet on mobile | Each row carries a pencil button |
| 5.2 | Tap a row body | The row is selected; no dialog opens |
| 5.3 | Tap the pencil | The edit dialog opens |
| 5.4 | On desktop, double click a row | The edit dialog opens |

---

## 6. Next occurrence on every entry (UX-19)

Desktop, regular transactions page.

| # | Step | Expected |
|---|---|---|
| 6.1 | Look at the new **Prochaine** column | Each entry shows a date in dd/mm/yyyy |
| 6.2 | Check a monthly entry started on the 15th | The date is the 15th of this month if still ahead, otherwise of next month |
| 6.3 | Check a weekly entry | The date is within the next seven days |
| 6.4 | Create an entry with an end date already passed, reload | The row shows "Terminée" with a flag, no date |
| 6.5 | Create an entry limited to 2 repetitions, both past | Same: "Terminée" |
| 6.6 | Create a monthly entry starting the **31st**, look at it in February | The next occurrence is the 28th (or 29th), not the 1st of March |
| 6.7 | On mobile, look at a card | The calendar line reads "Prochaine : dd/mm/yyyy", or "Terminée" |

⚠️ **6.6 is the case worth creating data for.** Day clamping is where a date calculation usually
goes wrong, and it is the rule most likely to disagree with the backend.

---

## 7. Monthly commitment (UX-19)

| # | Step | Expected |
|---|---|---|
| 7.1 | Look at the page header | A red total "… € ce mois-ci" next to the transaction count |
| 7.2 | If you have recurring income | A green total appears as well; with none, only the red one shows |
| 7.3 | Hover the red total | A tooltip explains it covers the entries listed |
| 7.4 | Check the arithmetic on a small account | It equals the sum of the occurrences falling in the **current calendar month** |
| 7.5 | With a weekly 10 € charge in a 5-occurrence month | It contributes 50 €, not 43.33 € |
| 7.6 | Add an ended recurrence | The total does not change |

⚠️ **7.5 is a deliberate design decision.** The commitment counts occurrences that actually fall in
the month rather than normalising every entry into a monthly equivalent. If you would rather see a
smoothed "average per month", say so — it is a different figure and it would be an estimate.

❗️ **7.4 has a known limitation.** The totals cover the entries **on the loaded page**. With
pagination at 10 rows and more than 10 recurring entries, the figure describes the page, not the
account. The tooltip says so. Tell me if you want it to cover everything — that needs either
loading all entries or a backend total.

---

## 8. Linked booklets (UX-24)

| # | Step | Expected |
|---|---|---|
| 8.1 | Look at the new **Livrets** column | The names of the linked booklets, as chips |
| 8.2 | Find an entry linked to nothing | It reads "Aucun livret lié", it is not blank |
| 8.3 | Link a booklet from the row action | The chip appears without reloading the page |
| 8.4 | Unlink it | The chip disappears and the row falls back to "Aucun livret lié" |
| 8.5 | On an entry already linked to **every** booklet, hover the link button | A title explains it is already linked to all your booklets |
| 8.6 | On an entry linked to nothing, hover the unlink button | A title explains it is linked to no booklet |
| 8.7 | On mobile, look at a card | A wallet line lists the booklets or says "Aucun livret lié" |
| 8.8 | On mobile, long press the disabled Lier button | The same explanation is reachable |

---

## 9. The whole table, after four items (UX-15, 19, 24)

This page gained two columns and two row buttons in this batch.

| # | Step | Expected |
|---|---|---|
| 9.1 | Open the page on a 1280 px window | The table fits without a horizontal scrollbar, or scrolls cleanly if it does not |
| 9.2 | Look at the frozen Actions column | It does not overlap the Livrets column |
| 9.3 | Narrow the window to about 900 px | The layout degrades readably rather than breaking |
| 9.4 | Switch to dark theme | Chips, "Terminée" and the commitment totals stay legible |

⚠️ **This section is the most likely place to find a problem.** The suite proves the values are
right; it cannot prove nine columns fit on a screen.

---

## Priority cases

If time is short, do these five:

- **9.1 / 9.2** — nine columns and four row buttons. The most likely visual break in this batch.
- **2.2** — the same crowding question on a 375 px card.
- **3.1** — tapping a card now selects instead of opening. Your judgement decides whether it stays.
- **6.6** — the 31st clamped to February. Where date maths usually breaks.
- **7.5** — the commitment counts real occurrences instead of a smoothed average. A design decision
  to confirm or reject.
