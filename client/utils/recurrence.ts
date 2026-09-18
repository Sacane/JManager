/**
 * Occurrence dates of a recurring entry, derived client side.
 *
 * The backend owns the authoritative computation when it materialises provisional transactions;
 * this answers the lighter question the regular transactions page asks — when is the next one due,
 * and which ones fall in a given month — without a round trip. Both must agree on the same rules,
 * the day clamping in particular.
 */

export interface RecurrenceLike {
  startDate: Date | string
  regularity: string
  frequencyProperty?: {
    type?: string
    untilDate?: Date | string
    times?: number
  }
}

/**
 * Steps taken after jumping straight to the candidate index. The jump lands on the right
 * occurrence or just before it, so a handful of steps is always enough; scanning from the first
 * occurrence instead would walk thousands of steps for a daily charge started years ago.
 */
const MAX_STEPS_AFTER_JUMP = 8

/** Guard for the month walk, which is bounded by the length of a month anyway. */
const MAX_OCCURRENCES_PER_MONTH = 40

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000

function asDate(value: Date | string): Date {
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  date.setHours(0, 0, 0, 0)
  return date
}

function atMidnight(value: Date): Date {
  const date = new Date(value.getTime())
  date.setHours(0, 0, 0, 0)
  return date
}

function lastDayOfMonth(year: number, monthIndex: number): number {
  return new Date(year, monthIndex + 1, 0).getDate()
}

/**
 * Nth occurrence of the recurrence, index 0 being the start date.
 *
 * Monthly and yearly steps keep the day of the start date and clamp it to the target month, so a
 * charge set on the 31st falls on the 28th in February and returns to the 31st in March rather
 * than drifting forward a day at a time.
 */
function occurrenceAt(start: Date, regularity: string, index: number): Date {
  switch (regularity) {
    case 'DAILY': {
      const date = new Date(start.getTime())
      date.setDate(date.getDate() + index)
      return date
    }
    case 'WEEKLY': {
      const date = new Date(start.getTime())
      date.setDate(date.getDate() + index * 7)
      return date
    }
    case 'YEARLY':
      return clampedToMonth(start.getFullYear() + index, start.getMonth(), start.getDate())
    case 'MONTHLY':
    default: {
      const absoluteMonth = start.getFullYear() * 12 + start.getMonth() + index
      return clampedToMonth(Math.floor(absoluteMonth / 12), absoluteMonth % 12, start.getDate())
    }
  }
}

function clampedToMonth(year: number, monthIndex: number, day: number): Date {
  return new Date(year, monthIndex, Math.min(day, lastDayOfMonth(year, monthIndex)))
}

/** Highest occurrence index the recurrence is allowed to reach, or null when it is unbounded. */
function lastAllowedIndex(recurrence: RecurrenceLike): number | null {
  const property = recurrence.frequencyProperty
  if (property?.type === 'TIMES' && typeof property.times === 'number') {
    return Math.max(property.times - 1, -1)
  }
  return null
}

function endDateOf(recurrence: RecurrenceLike): Date | null {
  const property = recurrence.frequencyProperty
  if (property?.type === 'UNTIL_DATE' && property.untilDate) return asDate(property.untilDate)
  return null
}

/** Whole calendar days between two dates, immune to daylight saving shifts. */
function daysBetween(from: Date, to: Date): number {
  const fromUtc = Date.UTC(from.getFullYear(), from.getMonth(), from.getDate())
  const toUtc = Date.UTC(to.getFullYear(), to.getMonth(), to.getDate())
  return Math.round((toUtc - fromUtc) / MILLISECONDS_PER_DAY)
}

/**
 * Occurrence index to start looking from — the exact one for day based frequencies, and one step
 * early for month based ones, where the day clamping makes the arithmetic approximate.
 */
function candidateIndexFor(start: Date, regularity: string, from: Date): number {
  if (from.getTime() <= start.getTime()) return 0

  switch (regularity) {
    case 'DAILY':
      return Math.max(daysBetween(start, from), 0)
    case 'WEEKLY':
      return Math.max(Math.ceil(daysBetween(start, from) / 7), 0)
    case 'YEARLY':
      return Math.max(from.getFullYear() - start.getFullYear() - 1, 0)
    case 'MONTHLY':
    default: {
      const months = (from.getFullYear() * 12 + from.getMonth()) - (start.getFullYear() * 12 + start.getMonth())
      return Math.max(months - 1, 0)
    }
  }
}

/**
 * First occurrence falling on or after `reference`, or null when the recurrence is over by then.
 */
export function nextOccurrenceOnOrAfter(recurrence: RecurrenceLike, reference: Date): Date | null {
  const start = asDate(recurrence.startDate)
  if (Number.isNaN(start.getTime())) return null

  const from = atMidnight(reference)
  const lastIndex = lastAllowedIndex(recurrence)
  const until = endDateOf(recurrence)
  const firstCandidate = candidateIndexFor(start, recurrence.regularity, from)

  for (let step = 0; step < MAX_STEPS_AFTER_JUMP; step++) {
    const index = firstCandidate + step
    if (lastIndex !== null && index > lastIndex) return null

    const occurrence = occurrenceAt(start, recurrence.regularity, index)
    if (until && occurrence.getTime() > until.getTime()) return null
    if (occurrence.getTime() >= from.getTime()) return occurrence
  }

  return null
}

/** True when no occurrence remains on or after `reference`. */
export function hasRecurrenceEnded(recurrence: RecurrenceLike, reference: Date): boolean {
  return nextOccurrenceOnOrAfter(recurrence, reference) === null
}

/**
 * Every occurrence falling inside the given calendar month.
 *
 * Used for the monthly commitment, which counts what actually falls in the month rather than
 * normalising a weekly charge into a fractional monthly equivalent.
 */
export function occurrencesInMonth(recurrence: RecurrenceLike, year: number, monthIndex: number): Date[] {
  const monthStart = new Date(year, monthIndex, 1)
  const monthEnd = new Date(year, monthIndex, lastDayOfMonth(year, monthIndex))
  const occurrences: Date[] = []

  let cursor = nextOccurrenceOnOrAfter(recurrence, monthStart)
  while (cursor && cursor.getTime() <= monthEnd.getTime() && occurrences.length < MAX_OCCURRENCES_PER_MONTH) {
    occurrences.push(cursor)
    const dayAfter = new Date(cursor.getTime())
    dayAfter.setDate(dayAfter.getDate() + 1)
    cursor = nextOccurrenceOnOrAfter(recurrence, dayAfter)
  }

  return occurrences
}
