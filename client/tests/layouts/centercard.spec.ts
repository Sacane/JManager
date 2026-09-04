import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import CenterCardLayout from '../../layouts/centercard.vue'

function mountLayout(meta: Record<string, unknown> = {}) {
  vi.stubGlobal('useRoute', () => ({ params: {}, query: {}, meta }))

  return shallowMount(CenterCardLayout, {
    slots: { default: '<p class="page-body">Contenu</p>' },
    global: {
      stubs: {
        NHeader: true,
        NuxtLinkLocale: { template: '<a class="back-link"><slot /></a>' },
      },
    },
  })
}

describe('layouts/centercard', () => {
  it('renders the routed page content', () => {
    const wrapper = mountLayout()

    expect(wrapper.find('.page-body').text()).toBe('Contenu')
  })

  it('offers a way back by default', () => {
    const wrapper = mountLayout()

    expect(wrapper.find('.back-link').exists()).toBe(true)
  })

  // /consent and /force-password-change are deliberate walls: the user must accept the terms
  // or set a new password before reaching the application. Showing a way out invites an
  // escape the middleware then undoes, which reads as a redirect loop — see UX-05.
  it('hides the way back when the route forbids it', () => {
    const wrapper = mountLayout({ allowBack: false })

    expect(wrapper.find('.back-link').exists()).toBe(false)
  })
})
