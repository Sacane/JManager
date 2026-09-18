import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import RegularTransactionCreationDialog from '../../components/dialog/RegularTransactionCreationDialog.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({
    formattedDateString: (value: Date) => value.toISOString().slice(0, 10),
    frequencyToString: (value: string) => value,
    strToFrequency: (value: string) => value,
  }),
}))

const BOOKLETS = [{ id: 'b-1', label: 'Compte courant', amount: 100, currency: 'EUR' }]

const InputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input class="label" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
}

const InputNumberStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input class="amount" :value="modelValue" @input="$emit('update:modelValue', Number($event.target.value))" />`,
}

/** Selects every booklet on click, standing in for the MultiSelect. */
const MultiSelectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: `<button type="button" class="pick-booklets" @click="$emit('update:modelValue', options)" />`,
}

function mountDialog() {
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))

  return mount(RegularTransactionCreationDialog, {
    props: { booklets: BOOKLETS },
    global: {
      stubs: {
        Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
        InputText: InputTextStub,
        InputNumber: InputNumberStub,
        MultiSelect: MultiSelectStub,
        DatePicker: true,
        Select: { template: '<div><slot /></div>' },
        Tag: true,
        RadioButton: true,
        FrequencySelector: true,
        MonthlyRepeatSelector: true,
        Button: {
          props: ['label', 'disabled'],
          emits: ['click'],
          template: `<button :data-label="label" :disabled="disabled" @click="!disabled && $emit('click')">{{ label }}</button>`,
        },
      },
    },
  })
}

async function submit(wrapper: ReturnType<typeof mountDialog>) {
  await wrapper.find('[data-label="Créer"]').trigger('click')
  await nextTick()
}

async function fillValidForm(wrapper: ReturnType<typeof mountDialog>) {
  await wrapper.find('input.label').setValue('Loyer')
  await wrapper.find('input.amount').setValue(900)
  await wrapper.find('.pick-booklets').trigger('click')
}

describe('components/dialog/RegularTransactionCreationDialog', () => {
  beforeEach(() => vi.clearAllMocks())

  // The dialog returned early without a word on an empty label or a zero amount: the user clicked
  // "Créer" and nothing happened at all.
  it('reports an empty label instead of silently doing nothing', async () => {
    const wrapper = mountDialog()
    await wrapper.find('input.amount').setValue(900)
    await wrapper.find('.pick-booklets').trigger('click')

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-rt-label"]').text()).toMatch(/libellé/i)
    expect(wrapper.emitted('createTransaction')).toBeUndefined()
  })

  it('reports a zero amount on the amount field', async () => {
    const wrapper = mountDialog()
    await wrapper.find('input.label').setValue('Loyer')
    await wrapper.find('input.amount').setValue(0)
    await wrapper.find('.pick-booklets').trigger('click')

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-rt-amount"]').text()).toMatch(/montant/i)
    expect(wrapper.find('[data-test="error-rt-label"]').exists()).toBe(false)
  })

  // The submit button used to be disabled while no booklet was picked, without saying so. It is
  // now always usable, and the missing booklet is reported on the booklet field.
  it('keeps the create action usable and names the missing booklet', async () => {
    const wrapper = mountDialog()
    await wrapper.find('input.label').setValue('Loyer')
    await wrapper.find('input.amount').setValue(900)

    expect(wrapper.find('[data-label="Créer"]').attributes('disabled')).toBeUndefined()

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-rt-booklets"]').text()).toMatch(/livret/i)
    expect(wrapper.emitted('createTransaction')).toBeUndefined()
  })

  it('reports every invalid field at once', async () => {
    const wrapper = mountDialog()

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-rt-label"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="error-rt-amount"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="error-rt-booklets"]').exists()).toBe(true)
  })

  it('clears a field error as soon as it is corrected', async () => {
    const wrapper = mountDialog()
    await submit(wrapper)

    await wrapper.find('input.label').setValue('Loyer')
    await nextTick()

    expect(wrapper.find('[data-test="error-rt-label"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-rt-amount"]').exists()).toBe(true)
  })

  it('emits the regular transaction once every field is valid', async () => {
    const wrapper = mountDialog()
    await fillValidForm(wrapper)

    await submit(wrapper)

    expect(wrapper.emitted('createTransaction')).toHaveLength(1)
    expect(wrapper.find('[data-test="error-rt-label"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-rt-amount"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-rt-booklets"]').exists()).toBe(false)
  })
})
