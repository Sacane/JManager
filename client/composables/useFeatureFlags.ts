import type { FeatureKey } from '~/constants/featureKeys'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

/**
 * Global feature flag composable.
 *
 * - Any component/page may call `isEnabled(key)` — reads from the shared reactive state.
 * - `fetchFlags()` loads the public /feature-flags endpoint. Called once at app boot via plugin.
 * - `toggleFlag(key, enabled)` calls the admin-only PATCH endpoint. Must only be invoked
 *   from pages protected by the admin middleware — the composable itself does not enforce auth.
 */
export default function useFeatureFlags() {
  const { get, patch } = useQuery()
  const { withLoading, isScopeLoading } = useLoading()

  const flags = useState<FeatureFlagDTO[]>('feature-flags:flags', () => [])
  const isFetching = computed(() => isScopeLoading(LOADING_SCOPES.featureFlags.fetch))
  const isToggling = computed(() => isScopeLoading(LOADING_SCOPES.featureFlags.toggle))

  /** Returns true when the given flag is enabled. Falls back to false for unknown keys. */
  function isEnabled(key: FeatureKey): boolean {
    return flags.value.find(f => f.key === key)?.enabled ?? false
  }

  /** Loads all flags from the public API. Safe to call at boot — never throws. */
  async function fetchFlags(): Promise<void> {
    await withLoading(async () => {
      try {
        const result = await get('feature-flags') as FeatureFlagDTO[] | undefined
        if (result) {
          flags.value = result
        }
      } catch {
        // Backend unavailable — all flags default to false via empty state.
        flags.value = []
      }
    }, LOADING_SCOPES.featureFlags.fetch)
  }

  /**
   * Toggles a feature flag via the admin API.
   * Only call this from pages protected by the admin middleware.
   *
   * @returns The updated flag on success.
   * @throws Re-throws network/auth errors so the caller can show a toast.
   */
  async function toggleFlag(key: FeatureKey, enabled: boolean): Promise<FeatureFlagDTO> {
    let updated: FeatureFlagDTO | undefined
    await withLoading(async () => {
      updated = await patch(`admin/feature-flags/${key}`, { enabled }) as FeatureFlagDTO
      if (updated) {
        flags.value = flags.value.map(f => (f.key === key ? updated! : f))
      }
    }, LOADING_SCOPES.featureFlags.toggle)
    if (!updated) {
      throw new Error(`toggleFlag: no response for key '${key}'`)
    }
    return updated
  }

  return {
    flags: readonly(flags),
    isFetching,
    isToggling,
    isEnabled,
    fetchFlags,
    toggleFlag,
  }
}
