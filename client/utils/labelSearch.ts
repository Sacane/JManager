const COMBINING_MARKS = /\p{M}+/gu

function fold(text: string): string {
  return text.normalize('NFD').replace(COMBINING_MARKS, '').toLowerCase()
}

/** The search as sent to the server: surrounding spaces removed, empty when blank. */
export function normalizedSearch(raw: string): string {
  return raw.trim()
}

/**
 * Whether [label] contains [fragment], regardless of case and accents — the rule the server applies,
 * reproduced for the views that already hold the whole period. A blank fragment matches every label.
 */
export function matchesLabel(label: string, fragment: string): boolean {
  const wanted = fold(fragment.trim())
  return wanted === '' || fold(label).includes(wanted)
}
