import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import NHeader from '../../components/NHeader.vue'

function mountHeader() {
  const toggle = vi.fn()
  vi.stubGlobal('useDark', () => ({
    isDark: ref(true),
    value: ref('dark'),
    preference: computed(() => 'system'),
    toggle,
    setPreference: vi.fn(),
  }))

  const wrapper = shallowMount(NHeader, {
    global: {
      stubs: {
        ClientOnly: { template: '<div><slot /></div>' },
        LocaleSwitch: { template: '<select class="locale-switch" />' },
      },
    },
  })

  return { wrapper, toggle }
}

describe('components/NHeader', () => {
  // The header sits on top of the consent, email verification and legal screens. The name of the
  // active theme was its largest text, labelling nothing the toggle beside it did not already
  // say — see UX-32.
  it('does not display the name of the active theme', () => {
    const { wrapper } = mountHeader()

    expect(wrapper.text()).not.toMatch(/Clair|Sombre|theme\.(light|dark)/)
  })

  it('keeps the locale switch and the theme toggle', () => {
    const { wrapper } = mountHeader()

    expect(wrapper.find('.locale-switch').exists()).toBe(true)
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('gives the theme toggle an accessible name', () => {
    const { wrapper } = mountHeader()
    const toggleButton = wrapper.find('button')

    expect(toggleButton.attributes('aria-label')).toBeTruthy()
  })

  it('toggles the theme when the control is activated', async () => {
    const { wrapper, toggle } = mountHeader()

    await wrapper.find('button').trigger('click')

    expect(toggle).toHaveBeenCalledTimes(1)
  })
})
