import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'
import process from 'node:process'
import { describe, expect, it } from 'vitest'

// Vitest runs from the client/ root (see vitest.config.ts).
const ROOT = process.cwd()
const SOURCE_DIRS = ['components', 'layouts', 'pages', 'app.vue']

/**
 * `bg-[var(--primary)]/5` reads as "primary at 5%", but UnoCSS cannot apply an opacity to a CSS
 * variable: it silently drops the `/5` and paints the full colour. Write the tint explicitly instead,
 * `bg-[color-mix(in_srgb,var(--primary)_5%,transparent)]`, which also follows the theme.
 */
const OPACITY_ON_VARIABLE = /\[var\(--[\w-]+\)\]\/\d+/g

function sourceFiles(path: string): string[] {
  const absolute = resolve(ROOT, path)
  if (!statSync(absolute, { throwIfNoEntry: false })) return []
  if (statSync(absolute).isFile()) return absolute.endsWith('.vue') ? [absolute] : []
  return readdirSync(absolute).flatMap(entry => sourceFiles(join(path, entry)))
}

describe('opacity on CSS variables', () => {
  it('never asks UnoCSS for an opacity it would drop', () => {
    const offenders = SOURCE_DIRS.flatMap(sourceFiles).flatMap((file) => {
      const lines = readFileSync(file, 'utf-8').split('\n')
      return lines.flatMap((line, index) =>
        [...line.matchAll(OPACITY_ON_VARIABLE)].map(match => `${relative(ROOT, file)}:${index + 1} ${match[0]}`),
      )
    })

    expect(offenders).toEqual([])
  })
})
