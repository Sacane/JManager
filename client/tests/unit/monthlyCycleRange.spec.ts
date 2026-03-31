import { describe, expect, it } from 'vitest'
import {
  resolveMonthlyCycleRangeForTargetMonth,
  toIsoLocalDate,
} from '../../utils/monthlyCycleRange'

describe('utils/monthlyCycleRange', () => {
  it('keeps calendar month boundaries for default cycle on selected month', () => {
    const range = resolveMonthlyCycleRangeForTargetMonth(2026, 3, 1, null)

    expect(toIsoLocalDate(range.start)).toBe('2026-03-01')
    expect(toIsoLocalDate(range.end)).toBe('2026-03-31')
  })

  it('resolves selected month range as 28 M-1 to 27 M for cycle 28 with default end', () => {
    const range = resolveMonthlyCycleRangeForTargetMonth(2026, 3, 28, null)

    expect(toIsoLocalDate(range.start)).toBe('2026-02-28')
    expect(toIsoLocalDate(range.end)).toBe('2026-03-27')
  })

  it('keeps 28 M-1 to 27 M when end day is explicitly configured to 27', () => {
    const range = resolveMonthlyCycleRangeForTargetMonth(2026, 3, 28, 27)

    expect(toIsoLocalDate(range.start)).toBe('2026-02-28')
    expect(toIsoLocalDate(range.end)).toBe('2026-03-27')
  })
})
