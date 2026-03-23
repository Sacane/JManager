import { afterEach, describe, expect, it, vi } from 'vitest'
import useLoading from '../../composables/useLoading'

afterEach(() => {
  vi.useRealTimers()
})

describe('composables/useLoading', () => {
  it('tracks global loading state with default scope', () => {
    const { isLoading, isScopeLoading, startLoading, stopLoading } = useLoading()

    expect(isLoading.value).toBe(false)
    expect(isScopeLoading()).toBe(false)

    startLoading()

    expect(isLoading.value).toBe(true)
    expect(isScopeLoading()).toBe(true)

    stopLoading()

    expect(isLoading.value).toBe(false)
    expect(isScopeLoading()).toBe(false)
  })

  it('tracks independent scoped loading counters', () => {
    const { isLoading, isScopeLoading, startLoading, stopLoading } = useLoading()

    startLoading('page')
    startLoading('submit')
    startLoading('submit')

    expect(isLoading.value).toBe(true)
    expect(isScopeLoading('page')).toBe(true)
    expect(isScopeLoading('submit')).toBe(true)

    stopLoading('submit')
    expect(isScopeLoading('submit')).toBe(true)

    stopLoading('submit')
    expect(isScopeLoading('submit')).toBe(false)
    expect(isScopeLoading('page')).toBe(true)

    stopLoading('page')
    expect(isLoading.value).toBe(false)
  })

  it('withLoading clears state on success and failure', async () => {
    const { isLoading, isScopeLoading, withLoading } = useLoading()

    await withLoading(async () => 'ok', 'refresh')
    expect(isScopeLoading('refresh')).toBe(false)

    await expect(withLoading(async () => {
      throw new Error('boom')
    }, 'refresh')).rejects.toThrow('boom')

    expect(isScopeLoading('refresh')).toBe(false)
    expect(isLoading.value).toBe(false)
  })

  it('withLoading keeps spinner for a minimum duration by default', async () => {
    vi.useFakeTimers()
    const { isScopeLoading, withLoading } = useLoading()

    const promise = withLoading(async () => 'ok', 'refresh')
    expect(isScopeLoading('refresh')).toBe(true)

    await vi.advanceTimersByTimeAsync(299)
    expect(isScopeLoading('refresh')).toBe(true)

    await vi.advanceTimersByTimeAsync(1)
    await promise
    expect(isScopeLoading('refresh')).toBe(false)
  })

  it('withLoading allows overriding minimum duration', async () => {
    vi.useFakeTimers()
    const { isScopeLoading, withLoading } = useLoading()

    const promise = withLoading(async () => 'ok', 'quick', { minDurationMs: 120 })
    expect(isScopeLoading('quick')).toBe(true)

    await vi.advanceTimersByTimeAsync(119)
    expect(isScopeLoading('quick')).toBe(true)

    await vi.advanceTimersByTimeAsync(1)
    await promise
    expect(isScopeLoading('quick')).toBe(false)
  })
})
