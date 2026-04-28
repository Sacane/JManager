import { describe, expect, it } from 'vitest'
import { capitalizeFirst, getTagStyle, hexToRgb, rgbToHex, toReadableTagTextColor } from '../../utils/util'

describe('utils/util', () => {
  it('converts hex to rgb', () => {
    expect(hexToRgb('#A1B2C3')).toEqual({ r: 161, g: 178, b: 195 })
  })

  it('returns black rgb on invalid hex', () => {
    expect(hexToRgb('invalid')).toEqual({ r: 0, g: 0, b: 0 })
  })

  it('converts rgb object to clamped hex', () => {
    expect(rgbToHex({ red: -12, green: 16, blue: 300 })).toBe('#0010ff')
  })

  it('builds readable tag style based on brightness', () => {
    expect(getTagStyle({ red: 255, green: 255, blue: 255 })).toEqual({
      backgroundColor: 'rgb(255, 255, 255)',
      color: 'black',
    })
    expect(getTagStyle({ red: 0, green: 0, blue: 0 })).toEqual({
      backgroundColor: 'rgb(0, 0, 0)',
      color: 'white',
    })
  })

  it('capitalizes first character and trims spaces', () => {
    expect(capitalizeFirst('  hello')).toBe('Hello')
    expect(capitalizeFirst('')).toBe('')
    expect(capitalizeFirst(null)).toBe('')
  })

  describe('toReadableTagTextColor', () => {
    it('clamps a very light color (near white) to at most 60% lightness', () => {
      const result = toReadableTagTextColor({ red: 250, green: 250, blue: 200 })
      const match = result.match(/hsl\(\d+,\s*\d+%,\s*(\d+)%\)/)
      const lightness = Number(match?.[1])
      expect(lightness).toBeLessThanOrEqual(60)
    })

    it('clamps a very dark color (near black) to at least 35% lightness', () => {
      const result = toReadableTagTextColor({ red: 10, green: 10, blue: 10 })
      const match = result.match(/hsl\(\d+,\s*\d+%,\s*(\d+)%\)/)
      const lightness = Number(match?.[1])
      expect(lightness).toBeGreaterThanOrEqual(35)
    })

    it('preserves hue for a mid-range color', () => {
      // A pure red (hsl 0°) should stay red
      const result = toReadableTagTextColor({ red: 200, green: 50, blue: 50 })
      expect(result.startsWith('hsl(0,')).toBe(true)
    })

    it('returns hsl format', () => {
      const result = toReadableTagTextColor({ red: 130, green: 80, blue: 200 })
      expect(result).toMatch(/^hsl\(\d+, \d+%, \d+%\)$/)
    })
  })
})
