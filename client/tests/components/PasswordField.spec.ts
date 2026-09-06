import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PasswordField from '../../components/PasswordField.vue'

function mountField(props: Record<string, unknown> = {}, slots: Record<string, string> = {}) {
  return mount(PasswordField, {
    props: { id: 'pwd', modelValue: '', ...props },
    slots,
  })
}

describe('components/PasswordField', () => {
  it('renders a masked input by default', () => {
    const wrapper = mountField()

    expect(wrapper.find('input').attributes('type')).toBe('password')
  })

  it('reveals the value when the control is activated', async () => {
    const wrapper = mountField()

    await wrapper.find('[data-test="toggle-password-visibility"]').trigger('click')

    expect(wrapper.find('input').attributes('type')).toBe('text')
  })

  it('masks it again on a second activation', async () => {
    const wrapper = mountField()
    const toggle = wrapper.find('[data-test="toggle-password-visibility"]')

    await toggle.trigger('click')
    await toggle.trigger('click')

    expect(wrapper.find('input').attributes('type')).toBe('password')
  })

  // The control has no text, so its accessible name is the only thing describing it — and it must
  // say what activating it does, not what the current state is.
  it('names the control after the action it performs', async () => {
    const wrapper = mountField()
    const toggle = wrapper.find('[data-test="toggle-password-visibility"]')

    expect(toggle.attributes('aria-label')).toContain('Afficher')

    await toggle.trigger('click')

    expect(wrapper.find('[data-test="toggle-password-visibility"]').attributes('aria-label')).toContain('Masquer')
  })

  it('does not submit the surrounding form when activated', () => {
    const wrapper = mountField()

    expect(wrapper.find('[data-test="toggle-password-visibility"]').attributes('type')).toBe('button')
  })

  it('emits what the user types', async () => {
    const wrapper = mountField()

    await wrapper.find('input').setValue('hunter2')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['hunter2'])
  })

  it('displays the bound value', () => {
    const wrapper = mountField({ modelValue: 'hunter2' })

    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('hunter2')
  })

  // UX-46 sets these on the fields; they have to survive being wrapped.
  it('forwards the credential manager attributes', () => {
    const wrapper = mountField({ id: 'reg-password', autocomplete: 'new-password', maxlength: 100 })
    const input = wrapper.find('input')

    expect(input.attributes('id')).toBe('reg-password')
    expect(input.attributes('autocomplete')).toBe('new-password')
    expect(input.attributes('maxlength')).toBe('100')
  })

  it('applies the class the page asks for', () => {
    const wrapper = mountField({ inputClass: 'settings-input' })

    expect(wrapper.find('input').classes()).toContain('settings-input')
  })

  it('can be disabled', () => {
    const wrapper = mountField({ disabled: true })

    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="toggle-password-visibility"]').attributes('disabled')).toBeDefined()
  })

  // UX-27 will hang the password rules and the strength indicator under the field; the slot is the
  // extension point that keeps it from having to rewrite the component.
  it('renders whatever the page hangs under the field', () => {
    const wrapper = mountField({}, { default: '<p class="rules">8 caractères minimum</p>' })

    expect(wrapper.find('.rules').exists()).toBe(true)
  })
})
