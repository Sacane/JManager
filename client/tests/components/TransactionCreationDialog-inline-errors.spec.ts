import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import TransactionCreationDialog from '../../components/dialog/TransactionCreationDialog.vue'
import FieldError from '../../components/FieldError.vue'

const InputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
}

const InputNumberStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input class="number" :value="modelValue" @input="$emit('update:modelValue', Number($event.target.value))" />`,
}

function mountDialog() {
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))
  const warn = vi.fn()
  vi.stubGlobal('useJToast', () => ({ warn, success: vi.fn(), errorAxios: vi.fn() }))

  const wrapper = mount(TransactionCreationDialog, {
    props: {
      title: 'Créer une transaction',
      digitPlaceholder: null,
      transactionPlaceholder: {
        id: null,
        label: '',
        value: null,
        isIncome: false,
        date: new Date(2026, 2, 10),
        tagDTO: { tagId: undefined, label: '', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
        isPreview: false,
      },
    },
    global: {
      components: { FieldError },
      stubs: {
        Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
        InputText: InputTextStub,
        InputNumber: InputNumberStub,
        DatePicker: true,
        Select: { template: '<div><slot /></div>' },
        Tag: true,
        RadioButton: true,
        Button: {
          props: ['label'],
          emits: ['click'],
          template: `<button @click="$emit('click')">{{ label }}</button>`,
        },
      },
    },
  })

  return { wrapper, warn }
}

async function submit(wrapper: ReturnType<typeof mountDialog>['wrapper']) {
  const buttons = wrapper.findAll('button')
  await buttons[buttons.length - 1].trigger('click')
  await nextTick()
}

describe('components/dialog/TransactionCreationDialog inline errors', () => {
  beforeEach(() => vi.clearAllMocks())

  // The single guard reported "montant supérieur à 0" even when the amount was fine and the label
  // was the problem, so the message could be plainly wrong.
  it('reports an empty label on the label field', async () => {
    const { wrapper } = mountDialog()

    await wrapper.find('input.number').setValue(12)
    await submit(wrapper)

    expect(wrapper.find('[data-test="error-label"]').text()).toMatch(/libellé/i)
    expect(wrapper.find('[data-test="error-value"]').exists()).toBe(false)
    expect(wrapper.emitted('createTransaction')).toBeUndefined()
  })

  it('reports a zero amount on the amount field', async () => {
    const { wrapper } = mountDialog()

    await wrapper.find('input').setValue('Courses')
    await wrapper.find('input.number').setValue(0)
    await submit(wrapper)

    expect(wrapper.find('[data-test="error-value"]').text()).toMatch(/montant/i)
    expect(wrapper.find('[data-test="error-label"]').exists()).toBe(false)
    expect(wrapper.emitted('createTransaction')).toBeUndefined()
  })

  it('reports a missing amount on the amount field', async () => {
    const { wrapper } = mountDialog()

    await wrapper.find('input').setValue('Courses')
    await submit(wrapper)

    expect(wrapper.find('[data-test="error-value"]').exists()).toBe(true)
  })

  it('reports both fields when both are wrong', async () => {
    const { wrapper } = mountDialog()

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-label"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="error-value"]').exists()).toBe(true)
  })

  it('clears a field error as soon as the field becomes valid', async () => {
    const { wrapper } = mountDialog()

    await submit(wrapper)
    expect(wrapper.find('[data-test="error-label"]').exists()).toBe(true)

    await wrapper.find('input').setValue('Courses')
    await nextTick()

    expect(wrapper.find('[data-test="error-label"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-value"]').exists()).toBe(true)
  })

  // Field-level problems belong next to the field; the toast is for what the form cannot know.
  it('stops warning by toast about a field it can point at', async () => {
    const { wrapper, warn } = mountDialog()

    await submit(wrapper)

    expect(warn).not.toHaveBeenCalled()
  })

  it('emits the transaction once every field is valid', async () => {
    const { wrapper } = mountDialog()

    await wrapper.find('input').setValue('Courses')
    await wrapper.find('input.number').setValue(42)
    await submit(wrapper)

    expect(wrapper.emitted('createTransaction')).toHaveLength(1)
    expect(wrapper.find('[data-test="error-label"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="error-value"]').exists()).toBe(false)
  })

  it('associates each error with its input for assistive technology', async () => {
    const { wrapper } = mountDialog()

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-label"]').attributes('role')).toBe('alert')
    expect(wrapper.find('[data-test="error-value"]').attributes('role')).toBe('alert')
  })
})
