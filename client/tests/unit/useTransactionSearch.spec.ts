import { beforeEach, describe, expect, it, vi } from 'vitest'
import useTransactionSearch from '../../composables/useTransactionSearch'

const get = vi.fn()

describe('useTransactionSearch', () => {
  beforeEach(() => {
    get.mockReset()
    vi.stubGlobal('useQuery', () => ({ get }))
  })

  it('searches every booklet over the requested months', async () => {
    const answer = { from: '2025-11-01', to: '2026-10-31', regulars: [], months: [], hasOlderTransactions: false }
    get.mockResolvedValue(answer)

    await expect(useTransactionSearch().searchTransactions(' loyer ', 12)).resolves.toEqual(answer)
    expect(get).toHaveBeenCalledWith('transaction/search', { q: 'loyer', months: 12 })
  })

  it('narrows the search to one booklet when asked', async () => {
    get.mockResolvedValue({})

    await useTransactionSearch().searchTransactions('loyer', 24, 'booklet-1')

    expect(get).toHaveBeenCalledWith('transaction/search', { q: 'loyer', months: 24, bookletId: 'booklet-1' })
  })
})
