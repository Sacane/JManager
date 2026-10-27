import useUserSettings from './useUserSettings'

/** Where versions before UX-23 kept the targets, in the browser only. */
export const LEGACY_BUDGET_STORAGE_KEY = 'dashboard.budgetTargetsByBooklet.v1'

/**
 * The monthly budget target of each booklet, kept with the account so it follows the user across
 * devices (UX-23). A target is a positive amount; emptying it or setting it to zero removes it.
 */
export default function useBudgetTargets() {
  const { setBudgetTarget, removeBudgetTarget } = useUserSettings()
  const toast = useJToast()

  const targets = ref<Record<string, number>>({})

  function loadFromSettings(settings: UserSettingsDTO) {
    targets.value = Object.fromEntries(
      settings.bookletCycles
        .map(cycle => [cycle.bookletId, Number(cycle.budgetTarget)] as const)
        .filter(([, amount]) => Number.isFinite(amount) && amount > 0),
    )
  }

  function withTarget(bookletId: string, amount: number | null) {
    const { [bookletId]: _, ...others } = targets.value
    targets.value = amount === null ? others : { ...others, [bookletId]: amount }
  }

  /** Stores [amount] as the target of [bookletId]; reports a failure and keeps the previous target. */
  async function save(bookletId: string, amount: number | undefined): Promise<boolean> {
    const value = Number(amount ?? 0)
    const removes = !Number.isFinite(value) || value <= 0
    try {
      if (removes) {
        if (targets.value[bookletId] !== undefined) await removeBudgetTarget(bookletId)
        withTarget(bookletId, null)
      } else {
        const rounded = Number(value.toFixed(2))
        await setBudgetTarget(bookletId, rounded)
        withTarget(bookletId, rounded)
      }
      return true
    } catch {
      toast.error('Le budget n\'a pas pu être enregistré. Veuillez réessayer.')
      return false
    }
  }

  function readLegacyTargets(): Record<string, unknown> | null {
    try {
      const raw = window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)
      if (raw === null) return null
      const parsed: unknown = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : {}
    } catch {
      return {}
    }
  }

  function forgetLegacyTargets() {
    try {
      window.localStorage.removeItem(LEGACY_BUDGET_STORAGE_KEY)
    } catch {
      // Storage unavailable: nothing to forget.
    }
  }

  /**
   * Sends the targets an earlier version kept in this browser to the account, once. The account wins
   * where it already holds a target; targets of booklets the user no longer has are dropped. When the
   * account cannot take them, the browser copy is kept to try again on the next visit.
   */
  async function migrateLegacyTargets(ownedBookletIds: string[]): Promise<void> {
    const legacy = readLegacyTargets()
    if (legacy === null) return

    const owned = new Set(ownedBookletIds)
    const toSend = Object.entries(legacy)
      .map(([bookletId, amount]) => [bookletId, Number(amount)] as const)
      .filter(([bookletId, amount]) => owned.has(bookletId) && targets.value[bookletId] === undefined && Number.isFinite(amount) && amount > 0)

    try {
      for (const [bookletId, amount] of toSend) {
        const rounded = Number(amount.toFixed(2))
        await setBudgetTarget(bookletId, rounded)
        withTarget(bookletId, rounded)
      }
      forgetLegacyTargets()
    } catch (error) {
      console.warn('Budget targets kept in this browser could not be moved to the account yet', error)
    }
  }

  return { targets: readonly(targets), loadFromSettings, save, migrateLegacyTargets }
}
