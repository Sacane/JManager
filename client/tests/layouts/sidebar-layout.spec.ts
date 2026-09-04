import { shallowMount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SidebarLayout from '../../layouts/sidebar-layout.vue'

function mountLayout() {
  return shallowMount(SidebarLayout, {
    slots: { default: '<h1 class="page-title">Mes Livrets</h1>' },
  })
}

describe('layouts/sidebar-layout', () => {
  it('renders the routed page inside the scrollable main region', () => {
    const wrapper = mountLayout()

    expect(wrapper.find('main .page-title').text()).toBe('Mes Livrets')
  })

  // The floating menu button of app-sidebar.vue is `position: fixed` at the top-left below
  // 769px. The offset that keeps page content clear of it belongs to the layout, so no page
  // has to re-declare it — see UX-01.
  it('carries the offset that keeps content clear of the floating menu button', () => {
    const wrapper = mountLayout()

    expect(wrapper.find('main').classes()).toContain('page-content')
  })
})
