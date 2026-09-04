import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { describe, expect, it } from 'vitest'
import { contrastRatio } from '../../utils/contrast'

// Vitest runs from the client/ root (see vitest.config.ts).
const css = readFileSync(resolve(process.cwd(), 'assets/css/variables.css'), 'utf-8')

/** Body of every `selector { ... }` block, in file order. Blocks hold no nested braces. */
function blocksFor(selector: RegExp): string[] {
  return [...css.matchAll(selector)].map(match => match[1] ?? '')
}

function readToken(block: string, name: string): string | null {
  const match = block.match(new RegExp(`--${name}:\\s*([^;]+);`))
  return match ? match[1]!.trim() : null
}

// The brand palette comes first, the semantic aliases second — the tokens live in the latter.
const rootBlocks = blocksFor(/:root\s*\{([^}]*)\}/g)
const light = rootBlocks[rootBlocks.length - 1]!
const dark = blocksFor(/\.dark\s*\{([^}]*)\}/g)[0]!

const SEMANTIC_TOKENS = ['success', 'danger', 'warning', 'info', 'income', 'expense']

const THEMES = [
  { name: 'light', block: light, cardBg: '#ffffff' },
  { name: 'dark', block: dark, cardBg: '#1e293b' },
]

describe('design tokens — semantic colours', () => {
  it('resolves both theme blocks', () => {
    expect(light).toContain('--primary:')
    expect(dark).toContain('--bg-primary:')
  })

  it('resolves the card backgrounds the contrast checks assume', () => {
    expect(readToken(light, 'card-bg')).toBe('#ffffff')
    expect(readToken(dark, 'card-bg')).toBe('#1e293b')
  })

  describe.each(THEMES)('$name theme', ({ block, cardBg }) => {
    it.each(SEMANTIC_TOKENS)('defines --%s', (token) => {
      expect(readToken(block, token)).not.toBeNull()
    })

    // Every semantic colour is used as text on a card. Below 4.5:1 it fails WCAG AA for the
    // 14px table cells that carry most of the amounts — see UX-11. The raw brand red is
    // 3.90:1 on white, which is why these are shades and tints rather than brand values.
    it.each(SEMANTIC_TOKENS)('keeps --%s readable on the card background', (token) => {
      expect(contrastRatio(readToken(block, token)!, cardBg)).toBeGreaterThanOrEqual(4.5)
    })
  })
})
