import { beforeEach, describe, expect, it, vi } from 'vitest'
import useJToast from '../../composables/useJToast'

describe('composables/useJToast', () => {
  const add = vi.fn()

  beforeEach(() => {
    add.mockReset()
    vi.stubGlobal('useToast', () => ({ add }))
  })

  it('maps backend error code from properties into localized toast message', () => {
    const { errorAxios } = useJToast()
    const axiosError = {
      response: {
        data: {
          properties: { code: 1002 },
        },
      },
    } as any

    errorAxios(axiosError)

    expect(add).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'error',
      summary: 'Transaction introuvable',
      detail: 'La transaction demandee est introuvable.',
    }))
  })

  it('uses provided title override while keeping code-based detail', () => {
    const { errorAxios } = useJToast()
    const axiosError = {
      response: {
        data: {
          code: 65,
        },
      },
    } as any

    errorAxios(axiosError, 'Erreur API')

    expect(add).toHaveBeenCalledWith(expect.objectContaining({
      summary: 'Erreur API',
      detail: 'Certaines donnees saisies sont invalides.',
    }))
  })

  it('falls back to unknown error message when code is absent', () => {
    const { errorAxios } = useJToast()
    const axiosError = {
      response: {
        data: {
          detail: 'Internal text ignored by frontend mapping',
        },
      },
    } as any

    errorAxios(axiosError)

    expect(add).toHaveBeenCalledWith(expect.objectContaining({
      summary: 'Erreur',
      detail: 'Une erreur inconnue est survenue.',
    }))
  })
})
