# Lot 4 — Manual validation walkthroughs

The test suite runs on happy-dom, which has no layout or rendering engine. Anything about
position, overlap, touch target size or visual state ships unverified until someone looks at it.
These walkthroughs cover what the suite cannot.

Mark each step ✅ or ❌. A ❌ is worth reporting with the viewport width and the theme in use.

**Delivered so far:** UX-15, UX-16.
**Still to come in this lot:** UX-19, UX-20, UX-24, UX-25, UX-26, UX-41, UX-50.

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

## Priority cases

If time is short, do these four:

- **1.4** — four controls in a column sized for two. The most likely visual break.
- **2.2** — same question on a 375 px card.
- **3.1** — the deliberate behaviour change. Your judgement decides whether it stays.
- **3.4** — the bulk delete appearing on mobile is the point of UX-15 scenario 4; it was
  unreachable before UX-16 landed.
