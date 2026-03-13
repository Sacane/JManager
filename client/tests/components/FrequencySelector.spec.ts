import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import FrequencySelector from '../../components/frequency-part/FrequencySelector.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({
    formattedDateString: (date: Date) => date.toISOString().slice(0, 10),
  }),
}))

describe('components/frequency-part/FrequencySelector', () => {
  it('clears times when switching to UNTIL_DATE', async () => {
    const wrapper = shallowMount(FrequencySelector, {
      props: {
        modelValue: {
          type: 'TIMES',
          untilDate: null,
          times: 5,
        },
      },
      global: {
        stubs: {
          Select: true,
          DatePicker: true,
          InputNumber: true,
        },
      },
    })

    await wrapper.setProps({
      modelValue: {
        type: 'UNTIL_DATE',
        untilDate: null,
        times: 5,
      },
    })

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted?.some(args => args[0]?.times === null)).toBe(true)
  })

  it('clears untilDate when switching to TIMES', async () => {
    const wrapper = shallowMount(FrequencySelector, {
      props: {
        modelValue: {
          type: 'UNTIL_DATE',
          untilDate: new Date('2026-03-13'),
          times: null,
        },
      },
      global: {
        stubs: {
          Select: true,
          DatePicker: true,
          InputNumber: true,
        },
      },
    })

    await wrapper.setProps({
      modelValue: {
        type: 'TIMES',
        untilDate: new Date('2026-03-13'),
        times: null,
      },
    })

    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    expect(emitted?.some(args => args[0]?.untilDate === null)).toBe(true)
  })
})
