import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import DarkModeToggle from '../../components/DarkModeToggle.vue'

describe('components/DarkModeToggle', () => {
  it('shows moon icon and calls toggle when clicked in light mode', async () => {
    const toggle = vi.fn()
    vi.stubGlobal('useDark', () => ({
      isDark: ref(false),
      toggle,
    }))

    const wrapper = mount(DarkModeToggle)

    expect(wrapper.find('.pi-moon').exists()).toBe(true)
    expect(wrapper.find('.pi-sun').exists()).toBe(false)

    await wrapper.get('button').trigger('click')

    expect(toggle).toHaveBeenCalledTimes(1)
  })

  it('shows sun icon in dark mode', () => {
    vi.stubGlobal('useDark', () => ({
      isDark: ref(true),
      toggle: vi.fn(),
    }))

    const wrapper = mount(DarkModeToggle)

    expect(wrapper.find('.pi-sun').exists()).toBe(true)
    expect(wrapper.find('.pi-moon').exists()).toBe(false)
    expect(wrapper.get('button').attributes('aria-label')).toBe('Activer le mode clair')
  })
})
