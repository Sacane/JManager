import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useBooklet from '../../composables/useBooklet'

const get = vi.fn()
const post = vi.fn()
const deleteQuery = vi.fn()

// useBooklet imports useQuery explicitly, so it must be mocked at module level rather than
// stubbed as a Nuxt auto-import.
vi.mock('../../composables/useQuery', () => ({
  default: () => ({ get, post, deleteQuery }),
}))

describe('composables/useBooklet', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    deleteQuery.mockReset()
    vi.stubGlobal('ref', ref)
  })

  describe('findRegenerableTransactions', () => {
    it('queries the regenerable endpoint with the month and year', async () => {
      get.mockResolvedValue([])

      await useBooklet().findRegenerableTransactions('booklet-1', 8, 2026)

      expect(get).toHaveBeenCalledWith('booklet/booklet-1/transactions/regenerable', { month: 8, year: 2026 })
    })

    it('returns the candidates as received', async () => {
      const candidates = [{
        regularTransactionId: 'rt-1',
        label: 'Loyer',
        value: '800.00',
        currency: '€',
        isIncome: false,
        date: '2026-08-05',
        tagDTO: null,
      }]
      get.mockResolvedValue(candidates)

      await expect(useBooklet().findRegenerableTransactions('booklet-1', 8, 2026)).resolves.toEqual(candidates)
    })
  })

  describe('findTransactionsByIdMonthAndYear', () => {
    it('forwards the requested sort direction to the transactions endpoint', async () => {
      get.mockResolvedValue({ transactions: [] })

      await useBooklet().findTransactionsByIdMonthAndYear('booklet-1', 8, 2026, {}, 1, 20, 'DESCENDING')

      expect(get).toHaveBeenCalledWith('booklet/booklet-1/transactions', {
        month: 8,
        year: 2026,
        page: 1,
        size: 20,
        sortDirection: 'DESCENDING',
      })
    })

    it('forwards the sort field alongside the direction', async () => {
      get.mockResolvedValue({ transactions: [] })

      await useBooklet().findTransactionsByIdMonthAndYear('booklet-1', 8, 2026, {}, 0, 10, 'ASCENDING', 'LABEL')

      expect(get).toHaveBeenCalledWith('booklet/booklet-1/transactions', {
        month: 8,
        year: 2026,
        page: 0,
        size: 10,
        sortDirection: 'ASCENDING',
        sortField: 'LABEL',
      })
    })

    it('omits the sort direction when none is requested', async () => {
      get.mockResolvedValue({ transactions: [] })

      await useBooklet().findTransactionsByIdMonthAndYear('booklet-1', 8, 2026)

      expect(get).toHaveBeenCalledWith('booklet/booklet-1/transactions', {
        month: 8,
        year: 2026,
        page: 0,
        size: 10,
      })
    })
  })

  describe('regenerateDeletedPrevisionalTransactions', () => {
    it('posts the selected identifiers in the request body', async () => {
      post.mockResolvedValue({ transactions: [], type: 'PREVISIONAL' })

      await useBooklet().regenerateDeletedPrevisionalTransactions('booklet-1', 8, 2026, ['rt-1', 'rt-2'])

      expect(post).toHaveBeenCalledWith(
        'booklet/booklet-1/transactions/regenerate?month=8&year=2026',
        { regularTransactionIds: ['rt-1', 'rt-2'] },
      )
    })
  })
})
