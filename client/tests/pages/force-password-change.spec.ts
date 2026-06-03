import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import ForcePasswordChangePage from '../../pages/force-password-change.vue'

const InputTextStub = {
  name: 'InputText',
  props: ['modelValue', 'type', 'placeholder', 'maxlength', 'disabled'],
  emits: ['update:modelValue'],
  template: '<input :type="type || \'text\'" :value="modelValue" :maxlength="maxlength" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" data-testid="submit-btn" @click="$emit(\'click\')">{{ label }}</button>',
}

const submitMock = vi.fn()
const newPasswordRef = ref('')
const confirmPasswordRef = ref('')
const passwordErrorRef = ref<string | null>(null)
const isSubmittingRef = ref(false)

function makeForceChangeMock(overrides: { isSubmitting?: boolean, passwordError?: string | null } = {}) {
  return {
    newPassword: newPasswordRef,
    confirmPassword: confirmPasswordRef,
    passwordError: ref(overrides.passwordError ?? null),
    isSubmitting: ref(overrides.isSubmitting ?? false),
    submit: submitMock,
  }
}

describe('pages/force-password-change', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    newPasswordRef.value = ''
    confirmPasswordRef.value = ''
    passwordErrorRef.value = null
    isSubmittingRef.value = false

    vi.stubGlobal('useForceChangePassword', () => makeForceChangeMock())
    vi.stubGlobal('definePageMeta', vi.fn())
  })

  function mountPage(overrides = {}) {
    vi.stubGlobal('useForceChangePassword', () => makeForceChangeMock(overrides))
    return shallowMount(ForcePasswordChangePage, {
      global: {
        stubs: {
          InputText: InputTextStub,
          Button: ButtonStub,
        },
      },
    })
  }

  it('renders the page title', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('Changement de mot de passe requis')
  })

  it('renders new password and confirm password fields', () => {
    const wrapper = mountPage()
    expect(wrapper.find('[data-test="new-password-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="confirm-password-input"]').exists()).toBe(true)
  })

  it('does not render a current password field', () => {
    const wrapper = mountPage()
    expect(wrapper.find('[data-test="current-password-input"]').exists()).toBe(false)
  })

  it('renders the submit button', () => {
    const wrapper = mountPage()
    expect(wrapper.find('[data-testid="submit-btn"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="submit-btn"]').text()).toContain('Changer mon mot de passe')
  })

  it('submit button is disabled while submitting', () => {
    const wrapper = mountPage({ isSubmitting: true })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button is enabled when not submitting', () => {
    const wrapper = mountPage({ isSubmitting: false })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('calls submit when button is clicked', async () => {
    const wrapper = mountPage()
    await wrapper.find('[data-testid="submit-btn"]').trigger('click')
    expect(submitMock).toHaveBeenCalledTimes(1)
  })

  it('does not show password error when null', () => {
    const wrapper = mountPage({ passwordError: null })
    expect(wrapper.find('[data-test="password-error"]').exists()).toBe(false)
  })

  it('shows password error message when set', () => {
    const wrapper = mountPage({ passwordError: 'Les mots de passe ne correspondent pas' })
    expect(wrapper.find('[data-test="password-error"]').text()).toContain('Les mots de passe ne correspondent pas')
  })

  it('password inputs have maxlength of 100', () => {
    const wrapper = mountPage()
    expect(wrapper.find('[data-test="new-password-input"]').attributes('maxlength')).toBe('100')
    expect(wrapper.find('[data-test="confirm-password-input"]').attributes('maxlength')).toBe('100')
  })
})
