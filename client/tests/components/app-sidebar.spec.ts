import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import AppSidebar from '../../components/app-sidebar.vue'

vi.mock('../../composables/useAuth', () => ({
  default: () => ({ isAuthenticated: ref(true), isAdmin: ref(false) }),
}))

function mountSidebar() {
  return shallowMount(AppSidebar, {
    global: {
      stubs: {
        NuxtLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        Profile: true,
      },
    },
  })
}

describe('components/app-sidebar', () => {
  // The global transaction search (UX-22) is reached from the menu, like every other page.
  it('leads to the transaction search, right after the booklets', () => {
    const links = mountSidebar().findAll('nav a').map(link => ({ href: link.attributes('href'), text: link.text() }))

    const search = links.findIndex(link => link.href === '/search')
    expect(links[search]?.text).toBe('Rechercher')
    expect(links[search - 1]?.href).toBe('/booklet')
  })
})
