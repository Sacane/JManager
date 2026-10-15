import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import useDebouncedSearch from '../../composables/useDebouncedSearch'

describe('useDebouncedSearch', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('applies what is typed once typing pauses, trimmed', async () => {
    const { search, appliedSearch } = useDebouncedSearch(300)

    search.value = 'lo'
    await nextTick()
    search.value = ' loyer '
    await nextTick()
    vi.advanceTimersByTime(299)
    expect(appliedSearch.value).toBe('')

    vi.advanceTimersByTime(1)
    expect(appliedSearch.value).toBe('loyer')
  })

  it('starts from a given search, applied at once', () => {
    const { search, appliedSearch } = useDebouncedSearch(300, 'loyer')

    expect(search.value).toBe('loyer')
    expect(appliedSearch.value).toBe('loyer')
  })
})
