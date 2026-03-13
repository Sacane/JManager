import { describe, expect, it } from 'vitest'
import { capitalizeFirst, getTagStyle, hexToRgb, rgbToHex } from '../../utils/util'

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
})
