import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'
import { describe, expect, it } from 'vitest'

/**
 * Four pages each declared their own frame and header: the dashboard inline, the booklets, tags
 * and admin pages in their own scoped SCSS. That produced four page-title sizes (2.5rem, 2.25rem,
 * 2.25rem, 2rem) and three padding scales for what is visually the same shell — see UX-30.
 */
const SHELL_PAGES = [
  'pages/index.vue',
  'pages/booklet/index.vue',
  'pages/tag/index.vue',
  'pages/admin/index.vue',
]

const SHORTCUTS = ['page-shell', 'page-header', 'page-heading', 'page-subheading', 'stat-card']

function read(file: string): string {
  return readFileSync(resolve(process.cwd(), file), 'utf-8')
}

const config = read('unocss.config.ts')

describe('page shell shortcuts', () => {
  it.each(SHORTCUTS)('declares the %s shortcut', (shortcut) => {
    expect(config).toMatch(new RegExp(`\\['${shortcut}',`))
  })

  it.each(SHELL_PAGES)('%s builds its frame from the shared shortcut', (file) => {
    expect(read(file)).toContain('page-shell')
  })

  // The bespoke frames are what diverged in the first place; leaving them in place next to the
  // shortcut would let them drift again.
  it.each(SHELL_PAGES)('%s declares no bespoke page frame any more', (file) => {
    const source = read(file)

    expect(source).not.toMatch(/\.(?:booklets|tag|admin)-page\s*\{/)
    expect(source).not.toMatch(/^\s*\.page-title\s*\{/m)
    expect(source).not.toMatch(/^\s*\.page-subtitle\s*\{/m)
  })

  it('gives the dashboard cards a single definition', () => {
    const dashboard = read('pages/index.vue')

    expect(dashboard).toContain('stat-card')
    expect(dashboard).not.toMatch(/rounded-2xl p-6 shadow-lg/)
  })

  // A page title is not a section title: page-heading sits deliberately above heading-1.
  it('keeps the page heading above the section heading in the scale', () => {
    expect(config).toMatch(/\['page-heading',\s*'[^']*text-4xl/)
    expect(config).toMatch(/\['heading-1',\s*'[^']*text-3xl/)
  })
})
