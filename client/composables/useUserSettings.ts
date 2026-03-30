import useQuery from './useQuery'

export default function useUserSettings() {
  const { get, put } = useQuery()

  async function getSettings(): Promise<UserSettingsDTO> {
    return get('user/settings')
  }

  async function updateSettings(settings: UserSettingsUpdateDTO): Promise<UserSettingsDTO> {
    return put('user/settings', settings)
  }

  return {
    getSettings,
    updateSettings,
  }
}
