export function hexToRgb(hex: string): { r: number, g: number, b: number } {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result
    ? {
        r: Number.parseInt(result[1] as string, 16),
        g: Number.parseInt(result[2] as string, 16),
        b: Number.parseInt(result[3] as string, 16),
      }
    : { r: 0, g: 0, b: 0 }
}

export function rgbToHex(color: { red: number, green: number, blue: number }): string {
  const toHex = (value: number) => {
    const hex = Math.max(0, Math.min(255, value)).toString(16)
    return hex.length === 1 ? `0${hex}` : hex
  }
  return `#${toHex(color.red)}${toHex(color.green)}${toHex(color.blue)}`
}

export function getTagStyle(colorDTO: { red: number, green: number, blue: number }) {
  const { red, green, blue } = colorDTO
  const brightness = (red * 299 + green * 587 + blue * 114) / 1000
  const textColor = brightness > 125 ? 'black' : 'white'
  return {
    backgroundColor: `rgb(${red}, ${green}, ${blue})`,
    color: textColor,
  }
}

/**
 * Returns a readable text color derived from a tag's colorDTO.
 * The hue and saturation are preserved but the lightness is clamped
 * to [35%, 60%] so the color is legible against both light and dark
 * neutral backgrounds, even when the original tag color is very pale
 * or very dark.
 */
export function toReadableTagTextColor(colorDTO: { red: number, green: number, blue: number }): string {
  const r = colorDTO.red / 255
  const g = colorDTO.green / 255
  const b = colorDTO.blue / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const delta = max - min

  let h = 0
  if (delta !== 0) {
    if (max === r) h = ((g - b) / delta) % 6
    else if (max === g) h = (b - r) / delta + 2
    else h = (r - g) / delta + 4
    h = Math.round(h * 60)
    if (h < 0) h += 360
  }

  const l = (max + min) / 2
  const s = delta === 0 ? 0 : delta / (1 - Math.abs(2 * l - 1))

  // Clamp lightness to a readable range [35%, 60%]
  const clampedL = Math.min(60, Math.max(35, Math.round(l * 100)))
  const clampedS = Math.round(Math.max(s * 100, 40)) // keep saturation vivid

  return `hsl(${h}, ${clampedS}%, ${clampedL}%)`
}

export function capitalizeFirst(s: string | null | undefined) {
  if (!s) return ''
  const trimmed = s.toString().trim()
  return trimmed.length ? trimmed.charAt(0).toUpperCase() + trimmed.slice(1) : ''
}
