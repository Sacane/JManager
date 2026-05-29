/**
 * Loads all feature flags once at app startup.
 * The composable silently swallows errors — unknown flags resolve to false.
 */
export default defineNuxtPlugin(async () => {
  const { fetchFlags } = useFeatureFlags()
  await fetchFlags()
})
