import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { computed } from 'vue'
import ThemePicker from '../../components/ThemePicker.vue'

function stubDark(prefValue: string) {
  const setPreference = vi.fn()
  vi.stubGlobal('useDark', () => ({
    preference: computed(() => prefValue),
    setPreference,
  }))
  return { setPreference }
}

describe('components/ThemePicker', () => {
  it('renders 3 theme option cards', () => {
    stubDark('system')
    const wrapper = mount(ThemePicker)

    const cards = wrapper.findAll('[role="radio"]')
    expect(cards).toHaveLength(3)
    expect(wrapper.text()).toContain('Clair')
    expect(wrapper.text()).toContain('Sombre')
    expect(wrapper.text()).toContain('Système')
  })

  it('marks the active card with aria-checked true', () => {
    stubDark('dark')
    const wrapper = mount(ThemePicker)

    const darkCard = wrapper.find('[data-test="theme-option-dark"]')
    const lightCard = wrapper.find('[data-test="theme-option-light"]')

    expect(darkCard.attributes('aria-checked')).toBe('true')
    expect(lightCard.attributes('aria-checked')).toBe('false')
  })

  it('applies the active CSS class to the selected option', () => {
    stubDark('light')
    const wrapper = mount(ThemePicker)

    expect(wrapper.find('[data-test="theme-option-light"]').classes()).toContain('theme-card--active')
    expect(wrapper.find('[data-test="theme-option-dark"]').classes()).not.toContain('theme-card--active')
    expect(wrapper.find('[data-test="theme-option-system"]').classes()).not.toContain('theme-card--active')
  })

  it('calls setPreference with the correct value on click', async () => {
    const { setPreference } = stubDark('light')
    const wrapper = mount(ThemePicker)

    await wrapper.find('[data-test="theme-option-dark"]').trigger('click')
    expect(setPreference).toHaveBeenCalledWith('dark')

    await wrapper.find('[data-test="theme-option-system"]').trigger('click')
    expect(setPreference).toHaveBeenCalledWith('system')
  })

  it('marks system option active when preference is system', () => {
    stubDark('system')
    const wrapper = mount(ThemePicker)

    expect(wrapper.find('[data-test="theme-option-system"]').classes()).toContain('theme-card--active')
  })
})
