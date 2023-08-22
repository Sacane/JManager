import type { LayoutKey } from '../.nuxt/types/layouts'
export default function useGlobalLayout() {
  const globalLayout = ref('default')
  const { isAuthenticated } = useAuth()
  watch(isAuthenticated, () => {
    if (isAuthenticated.value)
      globalLayout.value = 'sidebar-layout'

    else
      globalLayout.value = 'default'
  })
  return globalLayout as Ref<LayoutKey>
}
