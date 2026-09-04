import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletDeleteDialog from '../../components/booklet/BookletDeleteDialog.vue'

const DialogStub = {
  name: 'Dialog',
  props: ['visible', 'header'],
  template: '<section v-if="visible"><h2>{{ header }}</h2><slot /><slot name="footer" /></section>',
}

const InputTextStub = {
  name: 'InputText',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" v-bind="$attrs" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" v-bind="$attrs" @click="$emit(\'click\')">{{ label }}</button>',
}

function mountDialog(props: Record<string, unknown> = {}) {
  return mount(BookletDeleteDialog, {
    props: { visible: true, label: 'Livret A', transactionCount: 0, loading: false, ...props },
    global: { stubs: { Dialog: DialogStub, InputText: InputTextStub, Button: ButtonStub } },
  })
}

function confirmButton(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.find('[data-test="confirm-delete-booklet"]')
}

describe('components/booklet/BookletDeleteDialog', () => {
  it('names the booklet being deleted', () => {
    const wrapper = mountDialog()

    expect(wrapper.text()).toContain('Livret A')
  })

  it('keeps the confirmation disabled until the label is retyped', async () => {
    const wrapper = mountDialog()

    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="confirm-label-input"]').setValue('Livret A')

    expect(confirmButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('refuses a label that does not match', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="confirm-label-input"]').setValue('Livret B')

    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('ignores surrounding whitespace and case', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="confirm-label-input"]').setValue('  livret a  ')

    expect(confirmButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('emits the confirmation once the label matches', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="confirm-label-input"]').setValue('Livret A')
    await confirmButton(wrapper).trigger('click')

    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })

  it('emits nothing when cancelled', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="cancel-delete-booklet"]').trigger('click')

    expect(wrapper.emitted('confirm')).toBeUndefined()
    expect(wrapper.emitted('update:visible')?.at(-1)).toEqual([false])
  })

  // Wording moved here from the confirm.require call the page used to make; the UX-07 spelling
  // fixes have to survive the move.
  it('states the consequence, correctly spelled', () => {
    const wrapper = mountDialog()
    const text = wrapper.text()

    expect(text).toContain('Cette action est irréversible')
    expect(text).toContain('transactions')
    expect(text).not.toContain('Cette action et irréversible')
    expect(text).not.toMatch(/transactions enregistrés\b/)
  })

  it('names the number of transactions when it is known', () => {
    const wrapper = mountDialog({ transactionCount: 42 })

    expect(wrapper.text()).toContain('42')
  })

  // The list endpoint may return an empty snapshot for a booklet that does hold transactions.
  // Announcing "0 transactions" there would tell the user the deletion is harmless when it is not.
  it('never announces a count of zero', () => {
    const wrapper = mountDialog({ transactionCount: 0 })

    expect(wrapper.text()).not.toMatch(/\b0 transaction/)
    expect(wrapper.text()).toMatch(/toutes ses transactions/i)
  })

  it('clears the typed label when reopened', async () => {
    const wrapper = mountDialog({ visible: false })

    await wrapper.setProps({ visible: true })
    await wrapper.find('[data-test="confirm-label-input"]').setValue('Livret A')
    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })

    expect((wrapper.find('[data-test="confirm-label-input"]').element as HTMLInputElement).value).toBe('')
    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()
  })
})
