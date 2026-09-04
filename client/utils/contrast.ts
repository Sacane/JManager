/**
 * WCAG 2.1 contrast helpers.
 *
 * `toReadableTagTextColor` picks a readable text colour from an HSL lightness heuristic, which is
 * good enough for a tag chip but says nothing about WCAG conformance. These helpers compute the
 * real relative luminance and contrast ratio, so a colour choice can be verified rather than
 * guessed.
 */

function parseHex(hex: string): { r: number, g: number, b: number } {
  const normalized = hex.trim().replace('#', '')
  const expanded = normalized.length === 3
    ? normalized.split('').map(char => char + char).join('')
    : normalized

  return {
    r: Number.parseInt(expanded.slice(0, 2), 16) / 255,
    g: Number.parseInt(expanded.slice(2, 4), 16) / 255,
    b: Number.parseInt(expanded.slice(4, 6), 16) / 255,
  }
}

function toLinear(channel: number): number {
  return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
}

/** Relative luminance of a hex colour, from 0 (black) to 1 (white). */
export function relativeLuminance(hex: string): number {
  const { r, g, b } = parseHex(hex)

  return 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b)
}

/**
 * Contrast ratio between two hex colours, from 1 (identical) to 21 (black on white).
 *
 * WCAG AA asks for at least 4.5:1 for normal text, and 3:1 for large text
 * (from 18.66px bold, or 24px regular).
 */
export function contrastRatio(foreground: string, background: string): number {
  const a = relativeLuminance(foreground)
  const b = relativeLuminance(background)
  const lighter = Math.max(a, b)
  const darker = Math.min(a, b)

  return (lighter + 0.05) / (darker + 0.05)
}
