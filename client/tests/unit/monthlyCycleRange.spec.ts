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

  describe('non-regression — cycle start=27 end=26', () => {
    it('returns Feb 27 – Mar 26 for target month March 2026 regardless of a late anchor day', () => {
      // Bug: using resolveMonthlyCycleRangeFromAnchor with anchor=March 29 incorrectly returned Mar 27 – Apr 26
      const range = resolveMonthlyCycleRangeForTargetMonth(2026, 3, 27, 26)

      expect(toIsoLocalDate(range.start)).toBe('2026-02-27')
      expect(toIsoLocalDate(range.end)).toBe('2026-03-26')
    })

    it('returns Mar 27 – Apr 26 for target month April 2026', () => {
      const range = resolveMonthlyCycleRangeForTargetMonth(2026, 4, 27, 26)

      expect(toIsoLocalDate(range.start)).toBe('2026-03-27')
      expect(toIsoLocalDate(range.end)).toBe('2026-04-26')
    })

    it('returns Jan 27 – Feb 26 for target month February 2026', () => {
      const range = resolveMonthlyCycleRangeForTargetMonth(2026, 2, 27, 26)

      expect(toIsoLocalDate(range.start)).toBe('2026-01-27')
      expect(toIsoLocalDate(range.end)).toBe('2026-02-26')
    })
  })

  it('returns Mar 5 – Apr 4 for cycle start=5 end=4 on target month March 2026', () => {
    const range = resolveMonthlyCycleRangeForTargetMonth(2026, 3, 5, 4)

    expect(toIsoLocalDate(range.start)).toBe('2026-03-05')
    expect(toIsoLocalDate(range.end)).toBe('2026-04-04')
  })
})
