import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import NotFoundPage from '../../pages/[...all].vue'

function mountNotFound(isAuthenticated = false) {
  vi.stubGlobal('useAuth', () => ({
    user: ref(null),
    isAuthenticated: ref(isAuthenticated),
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    isAdmin: ref(false),
    tryRefresh: vi.fn(),
    initializeSession: vi.fn(),
  }))

  return shallowMount(NotFoundPage, {
    global: {
      stubs: {
        NuxtLink: { props: ['to'], template: '<a class="exit-link" :href="to"><slot /></a>' },
      },
    },
  })
}

describe('pages/[...all]', () => {
  it('explains in French that the address does not exist', () => {
    const wrapper = mountNotFound()

    expect(wrapper.text()).toContain('404')
    expect(wrapper.text()).toContain('Cette page n\'existe pas')
    expect(wrapper.text()).not.toContain('Error 404')
  })

  it('sends an authenticated user back to the dashboard', () => {
    const wrapper = mountNotFound(true)
    const link = wrapper.find('.exit-link')

    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/')
    expect(link.text()).toContain('tableau de bord')
  })

  it('sends an anonymous visitor to the login page', () => {
    const wrapper = mountNotFound(false)
    const link = wrapper.find('.exit-link')

    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/login')
    expect(link.text()).toContain('connexion')
  })
})
