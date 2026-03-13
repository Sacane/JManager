import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TransactionCreationDialog from '../../components/dialog/TransactionCreationDialog.vue'

function createTransactionPlaceholder(overrides: Partial<Record<string, any>> = {}) {
  return {
    id: null,
    label: '',
    value: null,
    isIncome: false,
    date: new Date('2026-03-13'),
    tagDTO: { tagId: undefined, label: '', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
    isPreview: false,
    ...overrides,
  }
}

const ButtonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button data-test="btn" @click="$emit(\'click\')">{{ label }}</button>',
}

describe('components/dialog/TransactionCreationDialog', () => {
  it('shows warning and does not emit createTransaction when transaction is invalid', async () => {
    const warn = vi.fn()
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))
    vi.stubGlobal('useJToast', () => ({ warn }))

    const wrapper = mount(TransactionCreationDialog, {
      props: {
        title: 'Créer transaction',
        digitPlaceholder: null,
        transactionPlaceholder: createTransactionPlaceholder(),
      },
      global: {
        stubs: {
          Dialog: { template: '<div><slot /></div>' },
          InputText: true,
          RadioButton: true,
          InputNumber: true,
          DatePicker: true,
          Select: true,
          Tag: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.findAll('[data-test="btn"]')[1]?.trigger('click')

    expect(warn).toHaveBeenCalledWith('Veuillez saisir un montant supérieur à 0')
    expect(wrapper.emitted('createTransaction')).toBeUndefined()
  })

  it('emits createTransaction when data is valid', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))
    vi.stubGlobal('useJToast', () => ({ warn: vi.fn() }))

    const payload = createTransactionPlaceholder({
      label: 'Salaire',
      value: 2500,
      isIncome: true,
    })

    const wrapper = mount(TransactionCreationDialog, {
      props: {
        title: 'Créer transaction',
        digitPlaceholder: null,
        transactionPlaceholder: payload,
      },
      global: {
        stubs: {
          Dialog: { template: '<div><slot /></div>' },
          InputText: true,
          RadioButton: true,
          InputNumber: true,
          DatePicker: true,
          Select: true,
          Tag: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.findAll('[data-test="btn"]')[1]?.trigger('click')

    const emitted = wrapper.emitted('createTransaction')
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]?.[0]).toMatchObject({
      label: 'Salaire',
      value: 2500,
      isIncome: true,
    })
  })

  it('emits visible=false and cancelCreation when cancel button is clicked', async () => {
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([]),
    }))
    vi.stubGlobal('useJToast', () => ({ warn: vi.fn() }))

    const wrapper = mount(TransactionCreationDialog, {
      props: {
        title: 'Créer transaction',
        digitPlaceholder: null,
        transactionPlaceholder: createTransactionPlaceholder(),
      },
      global: {
        stubs: {
          Dialog: { template: '<div><slot /></div>' },
          InputText: true,
          RadioButton: true,
          InputNumber: true,
          DatePicker: true,
          Select: true,
          Tag: true,
          Button: ButtonStub,
        },
      },
    })

    await wrapper.findAll('[data-test="btn"]')[0]?.trigger('click')

    expect(wrapper.emitted('visible')?.[0]).toEqual([false])
    expect(wrapper.emitted('cancelCreation')).toBeTruthy()
  })
})
