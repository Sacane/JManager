import { shallowMount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
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

  describe('desktop collapse (card #145)', () => {
    const originalWidth = window.innerWidth

    afterEach(() => {
      window.innerWidth = originalWidth
      vi.stubGlobal('useLocalStorage', (_key: string, defaultValue: unknown) => ref(defaultValue))
    })

    it('is expanded by default, with a collapse button and no expand button', () => {
      const wrapper = mountSidebar()

      expect(wrapper.find('aside').classes()).not.toContain('sidebar-collapsed')
      expect(wrapper.find('[data-test="sidebar-collapse"]').exists()).toBe(true)
      expect(wrapper.find('[data-test="sidebar-expand"]').exists()).toBe(false)
    })

    it('folds when the collapse button is clicked and leaves an expand button behind', async () => {
      const wrapper = mountSidebar()

      await wrapper.find('[data-test="sidebar-collapse"]').trigger('click')

      expect(wrapper.find('aside').classes()).toContain('sidebar-collapsed')
      expect(wrapper.find('aside').attributes('aria-hidden')).toBe('true')
      expect(wrapper.find('[data-test="sidebar-expand"]').attributes('aria-label')).toBe('Déplier le menu')
    })

    it('unfolds again from the expand button', async () => {
      const wrapper = mountSidebar()
      await wrapper.find('[data-test="sidebar-collapse"]').trigger('click')

      await wrapper.find('[data-test="sidebar-expand"]').trigger('click')

      expect(wrapper.find('aside').classes()).not.toContain('sidebar-collapsed')
      expect(wrapper.find('[data-test="sidebar-expand"]').exists()).toBe(false)
    })

    it('starts folded when the stored choice says so', () => {
      vi.stubGlobal('useLocalStorage', () => ref(true))

      const wrapper = mountSidebar()

      expect(wrapper.find('aside').classes()).toContain('sidebar-collapsed')
    })

    it('keeps the hamburger drawer on small screens, without the desktop collapse button', async () => {
      window.innerWidth = 500
      vi.stubGlobal('useLocalStorage', () => ref(true))

      const wrapper = mountSidebar()
      await nextTick()

      expect(wrapper.find('[data-test="sidebar-collapse"]').exists()).toBe(false)
      expect(wrapper.find('[data-test="sidebar-expand"]').exists()).toBe(false)
      expect(wrapper.find('aside').classes()).not.toContain('sidebar-collapsed')
    })
  })
})
