import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletDeleteDialog from '../../components/booklet/BookletDeleteDialog.vue'
import ConfirmByTypingDialog from '../../components/ConfirmByTypingDialog.vue'

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

// The retype-to-confirm behaviour lives in ConfirmByTypingDialog and is covered by its own spec.
// What is booklet-specific is the wording and the transaction count, so the generic dialog is
// rendered for real here rather than stubbed.
function mountDialog(props: Record<string, unknown> = {}) {
  return mount(BookletDeleteDialog, {
    props: { visible: true, label: 'Livret A', transactionCount: 0, loading: false, ...props },
    global: {
      components: { ConfirmByTypingDialog },
      stubs: { Dialog: DialogStub, InputText: InputTextStub, Button: ButtonStub },
    },
  })
}

describe('components/booklet/BookletDeleteDialog', () => {
  it('names the booklet being deleted', () => {
    const wrapper = mountDialog()

    expect(wrapper.text()).toContain('Livret A')
  })

  it('asks for the booklet label before allowing the deletion', async () => {
    const wrapper = mountDialog()
    const confirm = wrapper.find('[data-test="confirm-danger-action"]')

    expect(confirm.attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="confirm-typed-word"]').setValue('Livret A')

    expect(wrapper.find('[data-test="confirm-danger-action"]').attributes('disabled')).toBeUndefined()
  })

  it('forwards the confirmation to its caller', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="confirm-typed-word"]').setValue('Livret A')
    await wrapper.find('[data-test="confirm-danger-action"]').trigger('click')

    expect(wrapper.emitted('confirm')).toHaveLength(1)
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
})
