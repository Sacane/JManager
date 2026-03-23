import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import RegularTransactionDialogCard from '../../components/dialog/RegularTransactionDialogCard.vue'

function createTransaction(overrides: Partial<Record<string, any>> = {}) {
  return {
    id: 'rtx-1',
    label: 'Loyer',
    value: 1200,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-03-01',
    frequencyProperty: {
      type: 'FOREVER',
      untilDate: undefined,
      times: undefined,
    },
    tagDTO: {
      tagId: 'tag-1',
      label: 'Maison',
      colorDTO: { red: 120, green: 120, blue: 255 },
      isDefault: false,
    },
    ...overrides,
  }
}

const DialogStub = {
  props: ['visible'],
  template: '<div><slot /><slot name="footer" /></div>',
}

const InputTextStub = {
  props: ['modelValue', 'id'],
  emits: ['update:modelValue'],
  template: '<input :id="id" data-test="input-text" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
}

const ButtonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button :data-label="label" @click="$emit(\'click\')">{{ label }}</button>',
}

describe('components/dialog/RegularTransactionDialogCard', () => {
  it('loads tags on mount using useTag composable', async () => {
    const getAllTags = vi.fn().mockResolvedValue([])
    vi.stubGlobal('useTag', () => ({ getAllTags }))

    mount(RegularTransactionDialogCard, {
      props: {
        modelValue: true,
        transaction: createTransaction(),
      },
      global: {
        stubs: {
          Dialog: DialogStub,
          InputText: InputTextStub,
          InputNumber: true,
          RadioButton: true,
          Tag: true,
          Select: true,
          DatePicker: true,
          ProgressSpinner: true,
          Button: ButtonStub,
        },
      },
    })

    await Promise.resolve()

    expect(getAllTags).toHaveBeenCalledTimes(1)
  })

  it('emits save when form is changed and save is clicked', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))

    const wrapper = mount(RegularTransactionDialogCard, {
      props: {
        modelValue: true,
        transaction: createTransaction(),
      },
      global: {
        stubs: {
          Dialog: DialogStub,
          InputText: InputTextStub,
          InputNumber: true,
          RadioButton: true,
          Tag: true,
          Select: true,
          DatePicker: true,
          ProgressSpinner: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.get('#label').setValue('Nouveau loyer')
    await wrapper.get('[data-label="Enregistrer"]').trigger('click')

    const emitted = wrapper.emitted('save')
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]?.[0]).toMatchObject({ label: 'Nouveau loyer' })
  })

  it('emits delete with transaction id when delete is clicked', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))

    const wrapper = mount(RegularTransactionDialogCard, {
      props: {
        modelValue: true,
        transaction: createTransaction({ id: 'rtx-42' }),
      },
      global: {
        stubs: {
          Dialog: DialogStub,
          InputText: InputTextStub,
          InputNumber: true,
          RadioButton: true,
          Tag: true,
          Select: true,
          DatePicker: true,
          ProgressSpinner: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.get('[data-label="Supprimer"]').trigger('click')

    expect(wrapper.emitted('delete')?.[0]).toEqual(['rtx-42'])
  })

  it('emits update:modelValue=false when cancel is clicked', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))

    const wrapper = mount(RegularTransactionDialogCard, {
      props: {
        modelValue: true,
        transaction: createTransaction(),
      },
      global: {
        stubs: {
          Dialog: DialogStub,
          InputText: InputTextStub,
          InputNumber: true,
          RadioButton: true,
          Tag: true,
          Select: true,
          DatePicker: true,
          ProgressSpinner: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.get('[data-label="Annuler"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false])
  })

  it('displays linked booklet labels when booklet ids are provided', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))

    const wrapper = mount(RegularTransactionDialogCard, {
      props: {
        modelValue: true,
        transaction: createTransaction({ bookletIds: ['booklet-1'] }),
        booklets: [{ id: 'booklet-1', amount: 200, labelAccount: 'Compte principal', currency: 'EUR' }],
      },
      global: {
        stubs: {
          Dialog: DialogStub,
          InputText: InputTextStub,
          InputNumber: true,
          RadioButton: true,
          Tag: true,
          Select: true,
          DatePicker: true,
          ProgressSpinner: true,
          Button: ButtonStub,
        },
      },
    })

    expect(wrapper.text()).toContain('Livrets associés')
    expect(wrapper.text()).toContain('Compte principal')
  })
})
