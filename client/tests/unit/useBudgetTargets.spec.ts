import { beforeEach, describe, expect, it, vi } from 'vitest'
import useBudgetTargets, { LEGACY_BUDGET_STORAGE_KEY } from '../../composables/useBudgetTargets'

const setBudgetTarget = vi.fn()
const removeBudgetTarget = vi.fn()
const toastError = vi.fn()

vi.mock('../../composables/useUserSettings', () => ({ default: () => ({ setBudgetTarget, removeBudgetTarget }) }))

function settings(...targets: Array<[string, string | null]>): UserSettingsDTO {
  return {
    projectionWindowDays: 15,
    bookletCycles: targets.map(([bookletId, budgetTarget]) => ({
      bookletId,
      label: bookletId,
      monthlyPeriodStartDay: 1,
      monthlyPeriodEndDay: null,
      budgetTarget,
    })),
  } as UserSettingsDTO
}

describe('useBudgetTargets', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    setBudgetTarget.mockResolvedValue(undefined)
    removeBudgetTarget.mockResolvedValue(undefined)
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), warn: vi.fn(), error: toastError, errorAxios: vi.fn() }))
  })

  it('reads the targets stored on the account', () => {
    const { targets, loadFromSettings } = useBudgetTargets()

    loadFromSettings(settings(['b-1', '1500.50'], ['b-2', null]))

    expect(targets.value).toEqual({ 'b-1': 1500.5 })
  })

  it('stores a target on the account', async () => {
    const { targets, save } = useBudgetTargets()

    await expect(save('b-1', 1200)).resolves.toBe(true)

    expect(setBudgetTarget).toHaveBeenCalledWith('b-1', 1200)
    expect(targets.value).toEqual({ 'b-1': 1200 })
  })

  // As before: emptying the field, or setting it to zero, removes the target.
  it('removes the target when the amount is empty or zero', async () => {
    const { targets, loadFromSettings, save } = useBudgetTargets()
    loadFromSettings(settings(['b-1', '1500'], ['b-2', '300']))

    await save('b-1', undefined)
    await save('b-2', 0)

    expect(removeBudgetTarget).toHaveBeenCalledWith('b-1')
    expect(removeBudgetTarget).toHaveBeenCalledWith('b-2')
    expect(targets.value).toEqual({})
  })

  it('does not call the account to remove a target there is not', async () => {
    const { save } = useBudgetTargets()

    await save('b-1', undefined)

    expect(removeBudgetTarget).not.toHaveBeenCalled()
  })

  it('reports a failed save and keeps the previous target', async () => {
    setBudgetTarget.mockRejectedValue(new Error('down'))
    const { targets, loadFromSettings, save } = useBudgetTargets()
    loadFromSettings(settings(['b-1', '1500']))

    await expect(save('b-1', 900)).resolves.toBe(false)

    expect(toastError).toHaveBeenCalled()
    expect(targets.value).toEqual({ 'b-1': 1500 })
  })

  describe('migration of the targets kept by a previous version in the browser', () => {
    it('sends them to the account once, then forgets the browser copy', async () => {
      window.localStorage.setItem(LEGACY_BUDGET_STORAGE_KEY, JSON.stringify({ 'b-1': 1500, 'b-2': 300 }))
      const { targets, migrateLegacyTargets } = useBudgetTargets()

      await migrateLegacyTargets(['b-1', 'b-2'])

      expect(setBudgetTarget).toHaveBeenCalledWith('b-1', 1500)
      expect(setBudgetTarget).toHaveBeenCalledWith('b-2', 300)
      expect(targets.value).toEqual({ 'b-1': 1500, 'b-2': 300 })
      expect(window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)).toBeNull()
    })

    // The account wins: another device may have set it since.
    it('never overwrites a target already on the account', async () => {
      window.localStorage.setItem(LEGACY_BUDGET_STORAGE_KEY, JSON.stringify({ 'b-1': 1500 }))
      const { targets, loadFromSettings, migrateLegacyTargets } = useBudgetTargets()
      loadFromSettings(settings(['b-1', '800']))

      await migrateLegacyTargets(['b-1'])

      expect(setBudgetTarget).not.toHaveBeenCalled()
      expect(targets.value).toEqual({ 'b-1': 800 })
      expect(window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)).toBeNull()
    })

    it('drops targets of booklets that are no longer the user\'s, and empty ones', async () => {
      window.localStorage.setItem(LEGACY_BUDGET_STORAGE_KEY, JSON.stringify({ 'b-gone': 1500, 'b-1': 0 }))
      const { migrateLegacyTargets } = useBudgetTargets()

      await migrateLegacyTargets(['b-1'])

      expect(setBudgetTarget).not.toHaveBeenCalled()
      expect(window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)).toBeNull()
    })

    it('keeps the browser copy to try again next time when the account cannot take it', async () => {
      setBudgetTarget.mockRejectedValue(new Error('down'))
      window.localStorage.setItem(LEGACY_BUDGET_STORAGE_KEY, JSON.stringify({ 'b-1': 1500 }))
      const { migrateLegacyTargets } = useBudgetTargets()

      await migrateLegacyTargets(['b-1'])

      expect(window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)).not.toBeNull()
      expect(toastError).not.toHaveBeenCalled()
    })

    it('forgets a browser copy it cannot read', async () => {
      window.localStorage.setItem(LEGACY_BUDGET_STORAGE_KEY, 'not json')
      const { migrateLegacyTargets } = useBudgetTargets()

      await migrateLegacyTargets(['b-1'])

      expect(window.localStorage.getItem(LEGACY_BUDGET_STORAGE_KEY)).toBeNull()
    })
  })
})
