export default function useLoading() {
  const pendingScopes = ref<Record<string, number>>({})
  const defaultScope = 'global'

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

  async function withLoading<T>(action: () => Promise<T>, scope: string = defaultScope): Promise<T> {
    startLoading(scope)
    try {
      return await action()
    } finally {
      stopLoading(scope)
    }
  }

  return { isLoading, pendingScopes, isScopeLoading, startLoading, stopLoading, withLoading }
}
