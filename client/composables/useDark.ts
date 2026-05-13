export type ThemePreference = 'light' | 'dark' | 'system'

export default function useDark() {
  const colorMode = useColorMode()

  function toggle(dark?: boolean): void {
    if (colorMode.unknown) return

    if (dark !== undefined) {
      colorMode.preference = dark ? 'dark' : 'light'
    } else {
      colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
    }
  }

  function setPreference(pref: ThemePreference): void {
    if (colorMode.unknown) return
    colorMode.preference = pref
  }

  const value = computed(() => colorMode.value)
  const isDark = computed(() => value.value === 'dark')
  const preference = computed(() => colorMode.preference as ThemePreference)

  return { toggle, setPreference, value, isDark, preference }
}
