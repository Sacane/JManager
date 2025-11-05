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

export function capitalizeFirst(s: string | null | undefined) {
  if (!s) return ''
  const trimmed = s.toString().trim()
  return trimmed.length ? trimmed.charAt(0).toUpperCase() + trimmed.slice(1) : ''
}
