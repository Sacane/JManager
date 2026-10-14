import { describe, expect, it } from 'vitest'
import { formatSignedAmount } from '../../utils/money'

describe('utils/money', () => {
  it('signs an amount by its direction, in French format', () => {
    expect(formatSignedAmount(1234.5, true)).toBe('+1 234,50 €')
    expect(formatSignedAmount('60', false)).toBe('-60,00 €')
  })
})
