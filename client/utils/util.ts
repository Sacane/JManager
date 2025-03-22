export function hexToRgb(hex: string): { r: number, g: number, b: number } {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result
    ? {
        r: Number.parseInt(result[1], 16),
        g: Number.parseInt(result[2], 16),
        b: Number.parseInt(result[3], 16),
      }
    : { r: 0, g: 0, b: 0 }
}

export function getTagStyle(colorDTO) {
  const { red, green, blue } = colorDTO
  const brightness = (red * 299 + green * 587 + blue * 114) / 1000
  const textColor = brightness > 125 ? 'black' : 'white'
  return {
    backgroundColor: `rgb(${red}, ${green}, ${blue})`,
    color: textColor,
  }
}
