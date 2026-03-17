import { describe, expect, it } from 'vitest'
import useLoading from '../../composables/useLoading'

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
})
