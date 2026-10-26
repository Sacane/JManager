import { beforeEach, describe, expect, it, vi } from 'vitest'
import useUserSettings from '../../composables/useUserSettings'

const put = vi.fn()
const deleteQuery = vi.fn()

vi.mock('../../composables/useQuery', () => ({ default: () => ({ get: vi.fn(), put, deleteQuery }) }))

describe('useUserSettings budget targets', () => {
  beforeEach(() => vi.clearAllMocks())

  it('sets the target of one booklet, the amount as a two-decimal string', async () => {
    await useUserSettings().setBudgetTarget('b-1', 1500.5)

    expect(put).toHaveBeenCalledWith('user/settings/budget-targets/b-1', { amount: '1500.50' })
  })

  it('removes the target of one booklet', async () => {
    await useUserSettings().removeBudgetTarget('b-1')

    expect(deleteQuery).toHaveBeenCalledWith('user/settings/budget-targets/b-1', undefined)
  })
})
