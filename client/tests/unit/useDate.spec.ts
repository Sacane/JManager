import { describe, expect, it } from 'vitest'
import useDate from '../../composables/useDate'

describe('composables/useDate', () => {
  it('translates months between English and French', () => {
    const date = useDate()

    expect(date.translate('JANUARY')).toBe('JANVIER')
    expect(date.translate('UNKNOWN')).toBe('')
    expect(date.englishMonth('JANVIER')).toBe('JANUARY')
    expect(date.englishMonth('INCONNU')).toBe('')
  })

  it('maps month names to numbers and back', () => {
    const date = useDate()

    expect(date.monthFromNumber(12)).toBe('DECEMBER')
    expect(date.monthFromNumber(99)).toBeUndefined()
    expect(date.numberFromMonth('MARCH')).toBe(3)
    expect(date.numberFromMonth('UNKNOWN')).toBeUndefined()
  })

  it('parses and formats date values', () => {
    const date = useDate()
    const parsed = date.dateFromString('2026-03-13')

    expect(parsed.getFullYear()).toBe(2026)
    expect(parsed.getMonth()).toBe(2)
    expect(parsed.getDate()).toBe(13)
    expect(date.formattedDateString(new Date(2026, 0, 5))).toBe('2026-01-05')
  })

  it('converts frequencies in both directions', () => {
    const date = useDate()

    expect(date.frequencyToString('DAILY')).toBe('Tous les jours')
    expect(date.frequencyToString('CUSTOM')).toBe('CUSTOM')
    expect(date.strToFrequency('Chaque semaine')).toBe('WEEKLY')
    expect(date.strToFrequency('Unknown')).toBeUndefined()
  })
})
