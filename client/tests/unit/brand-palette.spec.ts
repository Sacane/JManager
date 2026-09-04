import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { describe, expect, it } from 'vitest'

/**
 * The tag page styled its primary action with an indigo gradient and TagCard fell back to
 * #6366f1 in five places. Neither belongs to the brand palette, which is built on the violet
 * #6508CC — so the most prominent button of the page was off-brand (UX-29).
 *
 * Scoped to the tag screens for now; the wider de-hardcoding sweep is UX-28 and UX-30.
 */
const TAG_SCREENS = [
  'pages/tag/index.vue',
  'components/tag/TagCard.vue',
]

/** Indigo values that have no counterpart in the brand palette. */
const OFF_PALETTE = /#6366f1|#4f46e5|rgba?\(\s*99\s*,\s*102\s*,\s*241|rgba?\(\s*79\s*,\s*70\s*,\s*229/gi

function read(file: string): string {
  return readFileSync(resolve(process.cwd(), file), 'utf-8')
}

describe('brand palette', () => {
  it.each(TAG_SCREENS)('%s holds no off-palette indigo', (file) => {
    const offenders = [...new Set(read(file).match(OFF_PALETTE) ?? [])]

    expect(offenders).toEqual([])
  })

  it.each(TAG_SCREENS)('%s references the primary token', (file) => {
    expect(read(file)).toContain('var(--primary')
  })
})
