import type { LayoutKey } from '#build/types/layouts'

export default function useGlobalLayout() {
  const globalLayout = ref('default')
  const { isAuthenticated } = useAuth()
  watch(isAuthenticated, (val) => {
    globalLayout.value = (val ? 'sidebar-layout' : 'default') as LayoutKey
  }, { immediate: true })
  return globalLayout as Ref<LayoutKey>
}
