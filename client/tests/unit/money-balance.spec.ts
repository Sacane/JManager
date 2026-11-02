import { describe, expect, it } from 'vitest'
import { formatBalance } from '../../utils/money'

// A balance is a level, not a movement: no "+", but a "-" when it goes below zero.
describe('utils/money formatBalance', () => {
  it('shows a balance without a sign, in French format', () => {
    expect(formatBalance('2940.00')).toBe('2 940,00 €')
  })

  it('shows an overdrawn balance with a minus sign', () => {
    expect(formatBalance(-12.5)).toBe('-12,50 €')
  })
})
