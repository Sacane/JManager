import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MonthlyRepeatSelector from '../../components/frequency-part/MonthlyRepeatSelector.vue'

function normalizeText(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036F]/g, '')
}

const ToggleSwitchStub = {
  props: ['modelValue'],
  emits: ['update:model-value'],
  template: '<button data-test="toggle" @click="$emit(\'update:model-value\', !modelValue)">toggle</button>',
}

const InputNumberStub = {
  props: ['modelValue'],
  emits: ['update:model-value'],
  template: '<input data-test="repeat-day" :value="modelValue" @input="$emit(\'update:model-value\', Number($event.target.value))">',
}

describe('components/frequency-part/MonthlyRepeatSelector', () => {
  it('shows default help text when repeat day selection is disabled', () => {
    const wrapper = mount(MonthlyRepeatSelector, {
      props: { repeatDay: null },
      global: {
        stubs: {
          ToggleSwitch: ToggleSwitchStub,
          InputNumber: InputNumberStub,
        },
      },
    })

    expect(normalizeText(wrapper.text())).toContain('La transaction sera repetee le meme jour que la date de debut chaque mois')
  })

  it('emits selected day when toggle is enabled', async () => {
    const wrapper = mount(MonthlyRepeatSelector, {
      props: { repeatDay: null },
      global: {
        stubs: {
          ToggleSwitch: ToggleSwitchStub,
          InputNumber: InputNumberStub,
        },
      },
    })

    await wrapper.get('[data-test="toggle"]').trigger('click')

    const emitted = wrapper.emitted('update:repeatDay')
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]).toEqual([1])
  })

  it('shows an error when day is outside allowed range', async () => {
    const wrapper = mount(MonthlyRepeatSelector, {
      props: { repeatDay: 1 },
      global: {
        stubs: {
          ToggleSwitch: ToggleSwitchStub,
          InputNumber: InputNumberStub,
        },
      },
    })

    await wrapper.get('[data-test="repeat-day"]').setValue('35')

    expect(normalizeText(wrapper.text())).toContain('Le jour doit etre entre 1 et 31')
  })
})
