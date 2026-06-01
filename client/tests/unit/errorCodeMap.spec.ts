import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  buildDiagnosticText,
  extractDiagnosticContext,
  extractErrorCode,
  getErrorMessageByCode,
} from '../../utils/errorCodeMap'

describe('utils/errorCodeMap', () => {
  it('extracts code from problem detail properties', () => {
    const code = extractErrorCode({ properties: { code: 1001 } })
    expect(code).toBe(1001)
  })

  it('extracts code from root problem detail payload', () => {
    const code = extractErrorCode({ code: 144 })
    expect(code).toBe(144)
  })

  it('returns undefined when code is not present', () => {
    expect(extractErrorCode({ detail: 'boom' })).toBeUndefined()
    expect(extractErrorCode(null)).toBeUndefined()
  })

  it('returns mapped message for known code', () => {
    expect(getErrorMessageByCode(1001)).toEqual({
      summary: 'Compte introuvable',
      detail: 'Le compte demande est introuvable.',
    })
  })

  it('returns default message for unknown code', () => {
    expect(getErrorMessageByCode(999999)).toEqual({
      summary: 'Erreur',
      detail: 'Une erreur inconnue est survenue.',
    })
  })

  describe('extractDiagnosticContext', () => {
    it('extracts requestId, userId and code from payload', () => {
      const ctx = extractDiagnosticContext({ code: 404, requestId: 'req-abc', userId: 'user-xyz' })
      expect(ctx).toEqual({ requestId: 'req-abc', userId: 'user-xyz', code: 404 })
    })

    it('returns empty object for null payload', () => {
      expect(extractDiagnosticContext(null)).toEqual({})
    })

    it('omits requestId when not a string', () => {
      const ctx = extractDiagnosticContext({ requestId: 42 })
      expect(ctx.requestId).toBeUndefined()
    })

    it('omits userId when absent', () => {
      const ctx = extractDiagnosticContext({ requestId: 'req-123' })
      expect(ctx.userId).toBeUndefined()
    })
  })

  describe('buildDiagnosticText', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-06-01T12:00:00.000Z'))
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('includes all fields when all are present', () => {
      const text = buildDiagnosticText({ requestId: 'req-abc', userId: 'user-xyz', code: 404 })
      expect(text).toContain('requestId: req-abc')
      expect(text).toContain('userId: user-xyz')
      expect(text).toContain('code: 404')
      expect(text).toContain('date: 2026-06-01T12:00:00.000Z')
    })

    it('omits userId line when undefined', () => {
      const text = buildDiagnosticText({ requestId: 'req-abc', code: 500 })
      expect(text).not.toContain('userId')
    })

    it('omits code line when undefined', () => {
      const text = buildDiagnosticText({ requestId: 'req-abc' })
      expect(text).not.toContain('code:')
    })

    it('always includes date', () => {
      const text = buildDiagnosticText({})
      expect(text).toContain('date:')
    })
  })
})
