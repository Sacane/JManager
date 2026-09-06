import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
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

function mountDialog(props: Record<string, unknown> = {}, slot = 'Conséquence irréversible.') {
  return mount(ConfirmByTypingDialog, {
    props: {
      visible: true,
      header: 'Supprimer ?',
      confirmationWord: 'Livret A',
      confirmLabel: 'Supprimer définitivement',
      loading: false,
      ...props,
    },
    slots: { default: slot },
    global: { stubs: { Dialog: DialogStub, InputText: InputTextStub, Button: ButtonStub } },
  })
}

const confirmButton = (wrapper: ReturnType<typeof mountDialog>) => wrapper.find('[data-test="confirm-danger-action"]')
const wordInput = (wrapper: ReturnType<typeof mountDialog>) => wrapper.find('[data-test="confirm-typed-word"]')

describe('components/ConfirmByTypingDialog', () => {
  it('shows the header, the consequence and the word to retype', () => {
    const wrapper = mountDialog()

    expect(wrapper.text()).toContain('Supprimer ?')
    expect(wrapper.text()).toContain('Conséquence irréversible.')
    expect(wrapper.text()).toContain('Livret A')
  })

  it('keeps the action disabled until the word is retyped', async () => {
    const wrapper = mountDialog()

    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()

    await wordInput(wrapper).setValue('Livret A')

    expect(confirmButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('refuses a word that does not match', async () => {
    const wrapper = mountDialog()

    await wordInput(wrapper).setValue('Livret B')

    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('ignores surrounding whitespace and case', async () => {
    const wrapper = mountDialog()

    await wordInput(wrapper).setValue('  livret a  ')

    expect(confirmButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('emits the confirmation once the word matches', async () => {
    const wrapper = mountDialog()

    await wordInput(wrapper).setValue('Livret A')
    await confirmButton(wrapper).trigger('click')

    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })

  it('emits nothing when cancelled', async () => {
    const wrapper = mountDialog()

    await wrapper.find('[data-test="cancel-danger-action"]').trigger('click')

    expect(wrapper.emitted('confirm')).toBeUndefined()
    expect(wrapper.emitted('update:visible')?.at(-1)).toEqual([false])
  })

  it('does not confirm while the action is running', async () => {
    const wrapper = mountDialog()

    await wordInput(wrapper).setValue('Livret A')
    await wrapper.setProps({ loading: true })
    await confirmButton(wrapper).trigger('click')

    expect(wrapper.emitted('confirm')).toBeUndefined()
  })

  it('clears the typed word when reopened for another target', async () => {
    const wrapper = mountDialog({ visible: false })

    await wrapper.setProps({ visible: true })
    await wordInput(wrapper).setValue('Livret A')
    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })

    expect((wordInput(wrapper).element as HTMLInputElement).value).toBe('')
    expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('uses the confirm label the caller asked for', () => {
    const wrapper = mountDialog({ confirmLabel: 'Supprimer mon compte' })

    expect(confirmButton(wrapper).text()).toBe('Supprimer mon compte')
  })
})
