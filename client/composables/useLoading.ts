export default function useLoading() {
  const pendingScopes = ref<Record<string, number>>({})
  const defaultScope = 'global'
  const defaultMinDurationMs = 300

  const isLoading = computed(() => Object.values(pendingScopes.value).some(count => count > 0))

  function isScopeLoading(scope: string = defaultScope) {
    return (pendingScopes.value[scope] ?? 0) > 0
  }

  function clearScope(scope: string) {
    const nextScopes = { ...pendingScopes.value }
    delete nextScopes[scope]
    pendingScopes.value = nextScopes
  }

  function startLoading(scope: string = defaultScope) {
    const currentCount = pendingScopes.value[scope] ?? 0
    pendingScopes.value[scope] = currentCount + 1
  }

  function stopLoading(scope: string = defaultScope) {
    const currentCount = pendingScopes.value[scope] ?? 0
    if (currentCount <= 1) {
      clearScope(scope)
      return
    }
    pendingScopes.value[scope] = currentCount - 1
  }

  async function ensureMinDuration(startedAt: number, minDurationMs: number) {
    const elapsed = Date.now() - startedAt
    const remaining = minDurationMs - elapsed
    if (remaining > 0) {
      await new Promise(resolve => setTimeout(resolve, remaining))
    }
  }

  async function withLoading<T>(
    action: () => Promise<T>,
    scope: string = defaultScope,
    options?: { minDurationMs?: number },
  ): Promise<T> {
    const startedAt = Date.now()
    const minDurationMs = options?.minDurationMs ?? defaultMinDurationMs
    startLoading(scope)
    try {
      return await action()
    } finally {
      await ensureMinDuration(startedAt, minDurationMs)
      stopLoading(scope)
    }
  }

  return { isLoading, pendingScopes, isScopeLoading, startLoading, stopLoading, withLoading }
}
