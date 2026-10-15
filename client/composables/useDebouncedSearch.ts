import { normalizedSearch } from '~/utils/labelSearch'

/**
 * A search typed by the user and the one to apply: [appliedSearch] follows [search] once typing pauses
 * for [delayMs], trimmed, so a request is sent per pause rather than per keystroke.
 */
export default function useDebouncedSearch(delayMs: number, initial = '') {
  const search = ref(initial)
  const appliedSearch = ref(normalizedSearch(initial))
  let timer: ReturnType<typeof setTimeout> | null = null

  watch(search, (typed) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      appliedSearch.value = normalizedSearch(typed)
    }, delayMs)
  })

  onUnmounted(() => {
    if (timer) clearTimeout(timer)
  })

  return { search, appliedSearch }
}
