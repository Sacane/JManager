import { describe, expect, it } from 'vitest'
import { matchesLabel, normalizedSearch } from '../../utils/labelSearch'

// Mirrors LabelSearch on the server: the "Tout le mois" view filters the period it already holds.
describe('utils/labelSearch', () => {
  it('matches a label containing the fragment', () => {
    expect(matchesLabel('Courses Carrefour Market', 'carrefour')).toBe(true)
    expect(matchesLabel('Essence', 'carrefour')).toBe(false)
  })

  it('ignores case and accents on both sides', () => {
    expect(matchesLabel('Péage autoroute', 'peage')).toBe(true)
    expect(matchesLabel('peage autoroute', 'PÉAGE')).toBe(true)
    expect(matchesLabel('Cadeaux de Noël', 'noel')).toBe(true)
  })

  it('matches the fragment as a whole, spaces included', () => {
    expect(matchesLabel('Courses Carrefour', 'courses carre')).toBe(true)
    expect(matchesLabel('Courses Carrefour', 'carrefour courses')).toBe(false)
  })

  it('matches everything when the fragment is blank', () => {
    expect(matchesLabel('Anything', '   ')).toBe(true)
    expect(normalizedSearch('  Loyer ')).toBe('Loyer')
    expect(normalizedSearch('   ')).toBe('')
  })
})
