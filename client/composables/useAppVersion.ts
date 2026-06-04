import axios from 'axios'

export default function useAppVersion() {
  const version = useState<string | null>('app:version', () => null)

  /**
   * Derives the actuator base URL from the configured apiUrl.
   * When apiUrl is relative (production, e.g. "/api/"), the actuator lives
   * on the same origin as the page — use window.location.origin.
   * When apiUrl is absolute (local dev, e.g. "http://localhost:8080/api/"),
   * extract the origin from it.
   */
  function resolveActuatorBase(apiUrl: string): string {
    try {
      return new URL(apiUrl).origin
    } catch {
      // Relative URL — production mode: same origin as the SPA
      return window.location.origin
    }
  }

  async function fetchVersion(): Promise<void> {
    try {
      const config = useRuntimeConfig()
      const base = resolveActuatorBase(config.public.apiUrl as string)
      const response = await axios.get(`${base}/actuator/info`)
      version.value = response.data?.build?.version ?? null
    } catch {
      version.value = null
    }
  }

  return { version: readonly(version), fetchVersion }
}
