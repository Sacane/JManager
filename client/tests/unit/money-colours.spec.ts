import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { describe, expect, it } from 'vitest'

/**
 * The four screens that display money amounts side by side. Income used to be blue on the
 * booklet detail and green everywhere else, so the colour code had to be relearned when
 * changing page (UX-12).
 *
 * The rest of the client still holds raw palette classes; sweeping those is tracked as UX-28
 * and UX-30. This guard grows as those land — it is deliberately not repo-wide yet.
 */
const MONEY_SCREENS = [
  'pages/index.vue',
  'pages/booklet/[id].vue',
  'pages/booklet/index.vue',
  'pages/regular-transaction/index.vue',
]

/** Palette classes that carry a money or status meaning and must go through a token. */
const RAW_PALETTE_CLASS = /\b(?:text|bg|border|from|to)-(?:green|red|emerald|rose)-\d{2,3}\b/g

/** Hex values previously hardcoded for income, expense or balance sign. */
const RAW_MONEY_HEX = /#(?:10b981|ef4444|009CFE|FF084B)\b/gi

function read(file: string): string {
  return readFileSync(resolve(process.cwd(), file), 'utf-8')
}

describe('money colour code', () => {
  it.each(MONEY_SCREENS)('%s uses no raw palette class for money or status', (file) => {
    const offenders = [...new Set(read(file).match(RAW_PALETTE_CLASS) ?? [])]

    expect(offenders).toEqual([])
  })

  it.each(MONEY_SCREENS)('%s hardcodes no money hex value', (file) => {
    const offenders = [...new Set(read(file).match(RAW_MONEY_HEX) ?? [])]

    expect(offenders).toEqual([])
  })

  it.each(MONEY_SCREENS)('%s colours amounts through the income and expense tokens', (file) => {
    const source = read(file)

    expect(source).toMatch(/var\(--income\)|amount-positive/)
    expect(source).toMatch(/var\(--expense\)|amount-negative/)
  })
})
