import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppToast from '../../components/AppToast.vue'

const writeTextMock = vi.fn().mockResolvedValue(undefined)

Object.defineProperty(globalThis, 'navigator', {
  configurable: true,
  value: { clipboard: { writeText: writeTextMock } },
})

function makeToastStub(message: Record<string, unknown>) {
  return {
    name: 'Toast',
    setup(_: unknown, { slots }: { slots: Record<string, ((p: unknown) => unknown) | undefined> }) {
      return () => slots.message?.({ message })
    },
  }
}

describe('components/AppToast', () => {
  beforeEach(() => {
    writeTextMock.mockClear()
    writeTextMock.mockResolvedValue(undefined)
  })

  it('renders summary and detail from the message slot', () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Quelque chose a échoué' }),
        },
      },
    })

    expect(wrapper.text()).toContain('Erreur')
    expect(wrapper.text()).toContain('Quelque chose a échoué')
  })

  it('renders severity icon when severity is provided', () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec', severity: 'error' }),
        },
      },
    })

    expect(wrapper.find('i.pi.pi-times-circle').exists()).toBe(true)
  })

  it('renders correct icon class per severity', () => {
    const cases: Array<{ severity: string, expectedClass: string }> = [
      { severity: 'success', expectedClass: 'pi-check-circle' },
      { severity: 'info', expectedClass: 'pi-info-circle' },
      { severity: 'warn', expectedClass: 'pi-exclamation-triangle' },
      { severity: 'error', expectedClass: 'pi-times-circle' },
    ]

    for (const { severity, expectedClass } of cases) {
      const wrapper = mount(AppToast, {
        global: {
          stubs: {
            Toast: makeToastStub({ summary: 'Msg', detail: 'Détail', severity }),
          },
        },
      })
      expect(wrapper.find(`i.${expectedClass}`).exists()).toBe(true)
    }
  })

  it('does not render icon when severity is absent', () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec' }),
        },
      },
    })

    expect(wrapper.find('i').exists()).toBe(false)
  })

  it('shows copy button when requestId is present', () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec', requestId: 'req-abc' }),
        },
      },
    })

    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Copier les infos de débogage')
  })

  it('does not show copy button when requestId is absent', () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec' }),
        },
      },
    })

    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('calls clipboard.writeText with diagnostic text on button click', async () => {
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec', requestId: 'req-abc', userId: 'user-xyz', code: 500 }),
        },
      },
    })

    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(writeTextMock).toHaveBeenCalledOnce()
    const text: string = writeTextMock.mock.calls[0][0]
    expect(text).toContain('requestId: req-abc')
    expect(text).toContain('userId: user-xyz')
    expect(text).toContain('code: 500')
  })

  it('changes button label to "Copié !" after successful copy', async () => {
    vi.useFakeTimers()
    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec', requestId: 'req-abc' }),
        },
      },
    })

    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('button').text()).toBe('Copié !')

    vi.advanceTimersByTime(2000)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('button').text()).toBe('Copier les infos de débogage')
    vi.useRealTimers()
  })

  it('shows "Échec de la copie" when clipboard write fails', async () => {
    vi.useFakeTimers()
    writeTextMock.mockRejectedValueOnce(new Error('Permission denied'))

    const wrapper = mount(AppToast, {
      global: {
        stubs: {
          Toast: makeToastStub({ summary: 'Erreur', detail: 'Échec', requestId: 'req-abc' }),
        },
      },
    })

    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('button').text()).toBe('Échec de la copie')

    vi.advanceTimersByTime(2000)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('button').text()).toBe('Copier les infos de débogage')
    vi.useRealTimers()
  })
})
