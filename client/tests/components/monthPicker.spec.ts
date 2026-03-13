import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import useDate from '../../composables/useDate'
import MonthPicker from '../../components/monthPicker.vue'

describe('components/monthPicker', () => {
  it('renders all months from useDate composable', () => {
    vi.stubGlobal('useDate', () => useDate())

    const wrapper = mount(MonthPicker)
    const options = wrapper.findAll('option')

    // 12 months + 1 placeholder option
    expect(options.length).toBe(13)
    expect(options[1]?.text()).toBe('JANUARY')
    expect(options[12]?.text()).toBe('DECEMBER')
  })

  it('emits selected month when the user changes the select value', async () => {
    vi.stubGlobal('useDate', () => useDate())

    const wrapper = mount(MonthPicker)
    const select = wrapper.get('select')

    await select.setValue('MARCH')
    await select.trigger('change')

    const emitted = wrapper.emitted('update:modelValue')
    const lastPayload = emitted?.at(-1)

    expect(emitted).toBeTruthy()
    expect(lastPayload).toEqual(['MARCH'])
  })
})
