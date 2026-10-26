import useQuery from './useQuery'

export default function useUserSettings() {
  const { get, put, deleteQuery } = useQuery()

  async function getSettings(): Promise<UserSettingsDTO> {
    return get('user/settings')
  }

  async function updateSettings(settings: UserSettingsUpdateDTO): Promise<UserSettingsDTO> {
    return put('user/settings', settings)
  }

  /** The monthly budget target of one booklet (UX-23); amounts travel as two-decimal strings. */
  async function setBudgetTarget(bookletId: string, amount: number): Promise<void> {
    await put(`user/settings/budget-targets/${bookletId}`, { amount: amount.toFixed(2) })
  }

  async function removeBudgetTarget(bookletId: string): Promise<void> {
    await deleteQuery(`user/settings/budget-targets/${bookletId}`, undefined)
  }

  return {
    getSettings,
    updateSettings,
    setBudgetTarget,
    removeBudgetTarget,
  }
}
