import { describe, expect, it } from 'vitest'
import { contrastRatio, relativeLuminance } from '../../utils/contrast'

describe('utils/contrast', () => {
  it('computes the luminance of the extremes', () => {
    expect(relativeLuminance('#000000')).toBeCloseTo(0, 5)
    expect(relativeLuminance('#ffffff')).toBeCloseTo(1, 5)
  })

  it('gives the maximum ratio for black on white', () => {
    expect(contrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 2)
  })

  it('gives a ratio of 1 for a colour against itself', () => {
    expect(contrastRatio('#6508CC', '#6508CC')).toBeCloseTo(1, 5)
  })

  it('is symmetric', () => {
    expect(contrastRatio('#006EA3', '#ffffff')).toBeCloseTo(contrastRatio('#ffffff', '#006EA3'), 5)
  })

  it('accepts hex values without the leading hash and in any case', () => {
    expect(contrastRatio('006ea3', '#FFFFFF')).toBeCloseTo(contrastRatio('#006EA3', '#ffffff'), 5)
  })

  it('matches known WCAG reference values', () => {
    // Brand blue-dark on a white card, and brand blue on the dark card.
    expect(contrastRatio('#006EA3', '#ffffff')).toBeGreaterThan(5.5)
    expect(contrastRatio('#009CFE', '#1e293b')).toBeGreaterThan(5)
  })
})
