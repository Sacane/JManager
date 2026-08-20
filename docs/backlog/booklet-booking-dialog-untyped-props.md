# `BookletBookingDialog` has no declared props and closes itself before the request resolves

## Observation

Two issues in the same component:

1. **No `defineProps`.** The parent passes `:visible="isAddBookletDialogOpen"`, but the
   component declares no props — the attribute only works through fallthrough onto the
   root `Dialog`, while the component's own `isVisibleData` ref stays out of sync. This
   breaks the project rule "Always type props with TypeScript interface + `withDefaults`".
2. **Self-closing before the outcome is known.** `createBooklet()` emits
   `createBooklet` and immediately calls `closeDialog()`. When the API call then fails,
   the dialog is already gone and the user loses everything they typed — they must
   re-enter the label and amount from scratch.

Emits are also untyped (`defineEmits(['visible', 'createBooklet', 'cancel'])`), and
`visible` is emitted but never listened to by the parent.

## Exact location

- `client/components/dialog/BookletBookingDialog.vue` — lines 2–24
- `client/pages/booklet/index.vue` — `handleBookletCreation`, whose `finally` block closing the dialog is dead code given the self-close above

## Expected behaviour

The dialog should be fully parent-controlled: a typed `visible` prop, typed emits, and
closing driven by the parent only once the creation succeeds. On failure it stays open
with the user's input intact next to the error toast.

## Impact

Any failed booklet creation (duplicate label, 6-booklet limit reached, network error)
silently discards the form. The error toast added alongside this note tells the user
*what* went wrong, but they still have to retype everything.

## Spotted during

Adding error/success toasts to the booklet index page.
