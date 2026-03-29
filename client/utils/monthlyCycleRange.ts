export interface MonthlyCycleRange {
  start: Date
  end: Date
}

function normalizeCycleDay(day: number): number {
  if (Number.isNaN(day)) {
    return 1
  }

  return Math.min(31, Math.max(1, Math.trunc(day)))
}

function asLocalDate(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function resolveMonthlyCycleBoundary(year: number, monthIndex: number, cycleDay: number): Date {
  const safeCycleDay = normalizeCycleDay(cycleDay)
  const lastDayOfMonth = new Date(year, monthIndex + 1, 0).getDate()
  const day = Math.min(safeCycleDay, lastDayOfMonth)
  return new Date(year, monthIndex, day)
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  next.setDate(next.getDate() + days)
  return next
}

export function resolveMonthlyCycleRangeFromAnchor(anchorDate: Date, cycleDay: number): MonthlyCycleRange {
  const anchor = asLocalDate(anchorDate)
  const currentBoundary = resolveMonthlyCycleBoundary(anchor.getFullYear(), anchor.getMonth(), cycleDay)

  if (anchor < currentBoundary) {
    const previousBoundary = resolveMonthlyCycleBoundary(anchor.getFullYear(), anchor.getMonth() - 1, cycleDay)
    return {
      start: previousBoundary,
      end: addDays(currentBoundary, -1),
    }
  }

  const nextBoundary = resolveMonthlyCycleBoundary(anchor.getFullYear(), anchor.getMonth() + 1, cycleDay)
  return {
    start: currentBoundary,
    end: addDays(nextBoundary, -1),
  }
}

export function resolveMonthlyCycleRangeForTargetMonth(targetYear: number, targetMonth: number, cycleDay: number): MonthlyCycleRange {
  const boundaryCurrent = resolveMonthlyCycleBoundary(targetYear, targetMonth - 1, cycleDay)
  const boundaryPrevious = resolveMonthlyCycleBoundary(targetYear, targetMonth - 2, cycleDay)

  return {
    start: boundaryPrevious,
    end: addDays(boundaryCurrent, -1),
  }
}

export function toIsoLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
