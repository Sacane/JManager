import { describe, expect, it } from 'vitest'
import { hasRecurrenceEnded, nextOccurrenceOnOrAfter, occurrencesInMonth } from '../../utils/recurrence'

function recurrence(overrides: Partial<Parameters<typeof nextOccurrenceOnOrAfter>[0]> = {}) {
  return {
    startDate: '2026-01-15',
    regularity: 'MONTHLY',
    frequencyProperty: { type: 'FOREVER' as const },
    ...overrides,
  }
}

/** Local calendar date. `toISOString` would shift a local midnight into the previous UTC day. */
function iso(date: Date | null): string | null {
  if (!date) return null
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

describe('utils/recurrence nextOccurrenceOnOrAfter', () => {
  it('returns the start date when the recurrence has not started yet', () => {
    const next = nextOccurrenceOnOrAfter(recurrence(), new Date(2026, 0, 1))

    expect(iso(next)).toBe('2026-01-15')
  })

  it('returns today when an occurrence falls today', () => {
    const next = nextOccurrenceOnOrAfter(recurrence(), new Date(2026, 0, 15))

    expect(iso(next)).toBe('2026-01-15')
  })

  it('steps monthly', () => {
    const next = nextOccurrenceOnOrAfter(recurrence(), new Date(2026, 2, 20))

    expect(iso(next)).toBe('2026-04-15')
  })

  it('steps weekly', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2026-01-05', regularity: 'WEEKLY' }),
      new Date(2026, 0, 20),
    )

    expect(iso(next)).toBe('2026-01-26')
  })

  it('steps daily', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2026-01-05', regularity: 'DAILY' }),
      new Date(2026, 0, 20),
    )

    expect(iso(next)).toBe('2026-01-20')
  })

  it('steps yearly', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2024-03-09', regularity: 'YEARLY' }),
      new Date(2026, 5, 1),
    )

    expect(iso(next)).toBe('2027-03-09')
  })

  // A charge set on the 31st must not silently drift to the 1st of the next month.
  it('clamps a monthly day that the target month does not have', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2026-01-31' }),
      new Date(2026, 1, 1),
    )

    expect(iso(next)).toBe('2026-02-28')
  })

  it('recovers the original day after a clamped month', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2026-01-31' }),
      new Date(2026, 2, 1),
    )

    expect(iso(next)).toBe('2026-03-31')
  })

  // A daily charge started years ago has more occurrences than any scan should walk through.
  it('answers for a long running daily recurrence', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '2005-01-01', regularity: 'DAILY' }),
      new Date(2026, 2, 10),
    )

    expect(iso(next)).toBe('2026-03-10')
  })

  it('answers for a long running monthly recurrence', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ startDate: '1995-06-08' }),
      new Date(2026, 2, 10),
    )

    expect(iso(next)).toBe('2026-04-08')
  })

  it('returns nothing once an until-date recurrence is over', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-03-15' } }),
      new Date(2026, 3, 1),
    )

    expect(next).toBeNull()
  })

  it('still returns the last occurrence of an until-date recurrence', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-03-15' } }),
      new Date(2026, 2, 1),
    )

    expect(iso(next)).toBe('2026-03-15')
  })

  it('returns nothing once a counted recurrence is exhausted', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ frequencyProperty: { type: 'TIMES', times: 3 } }),
      new Date(2026, 4, 1),
    )

    expect(next).toBeNull()
  })

  it('returns the last of a counted recurrence', () => {
    const next = nextOccurrenceOnOrAfter(
      recurrence({ frequencyProperty: { type: 'TIMES', times: 3 } }),
      new Date(2026, 2, 1),
    )

    expect(iso(next)).toBe('2026-03-15')
  })
})

describe('utils/recurrence hasRecurrenceEnded', () => {
  it('is false for a recurrence with no end', () => {
    expect(hasRecurrenceEnded(recurrence(), new Date(2030, 0, 1))).toBe(false)
  })

  it('is true once the until date has passed', () => {
    const ended = recurrence({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-03-15' } })

    expect(hasRecurrenceEnded(ended, new Date(2026, 3, 1))).toBe(true)
    expect(hasRecurrenceEnded(ended, new Date(2026, 1, 1))).toBe(false)
  })

  it('is true once the repetitions are exhausted', () => {
    const ended = recurrence({ frequencyProperty: { type: 'TIMES', times: 2 } })

    expect(hasRecurrenceEnded(ended, new Date(2026, 4, 1))).toBe(true)
    expect(hasRecurrenceEnded(ended, new Date(2026, 1, 1))).toBe(false)
  })
})

// The monthly commitment is the sum of the occurrences that actually fall in the month, never a
// normalised estimate: a weekly charge commits four or five times, not 4.33.
describe('utils/recurrence occurrencesInMonth', () => {
  it('counts a single monthly occurrence', () => {
    expect(occurrencesInMonth(recurrence(), 2026, 2)).toHaveLength(1)
  })

  it('counts every weekly occurrence of the month', () => {
    const weekly = recurrence({ startDate: '2026-03-02', regularity: 'WEEKLY' })

    expect(occurrencesInMonth(weekly, 2026, 2)).toHaveLength(5)
  })

  it('counts nothing before the recurrence starts', () => {
    expect(occurrencesInMonth(recurrence(), 2025, 11)).toHaveLength(0)
  })

  it('counts nothing after the recurrence ended', () => {
    const ended = recurrence({ frequencyProperty: { type: 'UNTIL_DATE', untilDate: '2026-02-20' } })

    expect(occurrencesInMonth(ended, 2026, 5)).toHaveLength(0)
  })

  it('stops a counted recurrence at its last repetition', () => {
    const counted = recurrence({ startDate: '2026-01-10', regularity: 'WEEKLY', frequencyProperty: { type: 'TIMES', times: 3 } })

    expect(occurrencesInMonth(counted, 2026, 0)).toHaveLength(3)
    expect(occurrencesInMonth(counted, 2026, 1)).toHaveLength(0)
  })

  it('counts a yearly occurrence only in its month', () => {
    const yearly = recurrence({ startDate: '2024-03-09', regularity: 'YEARLY' })

    expect(occurrencesInMonth(yearly, 2026, 2)).toHaveLength(1)
    expect(occurrencesInMonth(yearly, 2026, 3)).toHaveLength(0)
  })
})
