import axios from 'axios'

export default function useAppVersion() {
  const version = useState<string | null>('app:version', () => null)

  async function fetchVersion(): Promise<void> {
    try {
      const config = useRuntimeConfig()
      const origin = new URL(config.public.apiUrl).origin
      const response = await axios.get(`${origin}/actuator/info`)
      version.value = response.data?.build?.version ?? null
    } catch {
      version.value = null
    }
  }

  return { version: readonly(version), fetchVersion }
}
