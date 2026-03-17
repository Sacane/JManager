import { describe, expect, it } from 'vitest'
import { extractErrorCode, getErrorMessageByCode } from '../../utils/errorCodeMap'

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
})
