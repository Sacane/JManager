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

function normalizeOptionalCycleDay(day: number | null | undefined): number | undefined {
  if (day === null || day === undefined || Number.isNaN(day)) {
    return undefined
  }

  return Math.min(31, Math.max(1, Math.trunc(day)))
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

function resolveRangeEndFromStart(start: Date, cycleStartDay: number, cycleEndDay?: number | null): Date {
  const normalizedEndDay = normalizeOptionalCycleDay(cycleEndDay)
  if (normalizedEndDay !== undefined) {
    return resolveMonthlyCycleBoundary(start.getFullYear(), start.getMonth() + 1, normalizedEndDay)
  }

  const nextStartBoundary = resolveMonthlyCycleBoundary(start.getFullYear(), start.getMonth() + 1, cycleStartDay)
  return addDays(nextStartBoundary, -1)
}

export function resolveMonthlyCycleRangeFromAnchor(anchorDate: Date, cycleStartDay: number, cycleEndDay?: number | null): MonthlyCycleRange {
  const anchor = asLocalDate(anchorDate)
  const currentBoundary = resolveMonthlyCycleBoundary(anchor.getFullYear(), anchor.getMonth(), cycleStartDay)

  if (anchor < currentBoundary) {
    const previousBoundary = resolveMonthlyCycleBoundary(anchor.getFullYear(), anchor.getMonth() - 1, cycleStartDay)
    return {
      start: previousBoundary,
      end: resolveRangeEndFromStart(previousBoundary, cycleStartDay, cycleEndDay),
    }
  }

  return {
    start: currentBoundary,
    end: resolveRangeEndFromStart(currentBoundary, cycleStartDay, cycleEndDay),
  }
}

export function resolveMonthlyCycleRangeForTargetMonth(targetYear: number, targetMonth: number, cycleStartDay: number, cycleEndDay?: number | null): MonthlyCycleRange {
  const boundaryPrevious = resolveMonthlyCycleBoundary(targetYear, targetMonth - 2, cycleStartDay)

  return {
    start: boundaryPrevious,
    end: resolveRangeEndFromStart(boundaryPrevious, cycleStartDay, cycleEndDay),
  }
}

export function toIsoLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
