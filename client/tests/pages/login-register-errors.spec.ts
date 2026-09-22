import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, nextTick, ref } from 'vue'
import FieldError from '../../components/FieldError.vue'
import PasswordField from '../../components/PasswordField.vue'
import PasswordRules from '../../components/PasswordRules.vue'
import LoginPage from '../../pages/login.vue'

const InputTextStub = {
  props: ['modelValue', 'type', 'placeholder', 'maxlength', 'disabled'],
  emits: ['update:modelValue'],
  template: '<input :type="type" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const CheckboxStub = {
  props: ['modelValue', 'inputId', 'binary'],
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :id="inputId" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
}

const registerMock = vi.fn()
const loginMock = vi.fn()

function mountPage() {
  vi.stubGlobal('useAuth', vi.fn(() => ({
    login: loginMock,
    register: registerMock,
    user: ref(null),
    isAuthenticated: ref(false),
    isAdmin: ref(false),
    logout: vi.fn(),
    tryRefresh: vi.fn(),
    initializeSession: vi.fn(),
  })))
  vi.stubGlobal('useFeatureFlags', vi.fn(() => ({
    flags: ref([]),
    isFetching: ref(false),
    isToggling: ref(false),
    isEnabled: vi.fn(() => true),
    fetchFlags: vi.fn(),
    toggleFlag: vi.fn(),
  })))
  vi.stubGlobal('useJToast', vi.fn(() => ({ success: vi.fn(), error: vi.fn(), errorAxios: vi.fn() })))

  return shallowMount(LoginPage, {
    global: {
      stubs: {
        InputText: InputTextStub,
        Button: { props: ['type', 'loading', 'disabled'], template: '<button :type="type"><slot /></button>' },
        Checkbox: CheckboxStub,
        NuxtLink: { props: ['to', 'target'], template: '<a :href="to"><slot /></a>' },
        FeatureGate: {
          props: ['feature'],
          setup: () => ({ enabled: computed(() => true) }),
          template: '<slot v-if="enabled" />',
        },
        // shallowMount stubs every child, globally registered ones included. Without opting these
        // back in, the error element would exist with its data-test attribute and render nothing.
        PasswordField: false,
        PasswordRules: false,
        FieldError: false,
      },
      components: { PasswordField, PasswordRules, FieldError },
    },
  })
}

// Satisfies the published policy (12 characters minimum).
const COMPLIANT = 'secret-password-1'

async function fillRegistration(
  wrapper: ReturnType<typeof mountPage>,
  confirm: string,
  password: string = COMPLIANT,
) {
  const vm = wrapper.vm as any
  vm.switchMode('register')
  await nextTick()
  vm.userRegistered.username = 'johan'
  vm.userRegistered.email = 'johan@example.com'
  vm.userRegistered.password = password
  vm.userRegistered.confirmPassword = confirm
  vm.userRegistered.tosAccepted = true
  vm.userRegistered.privacyAccepted = true
  await nextTick()
}

describe('pages/login registration errors', () => {
  beforeEach(() => vi.clearAllMocks())

  // The mismatch was announced in a block at the bottom of the form, after the consent
  // checkboxes, far from the field that caused it.
  it('reports a password mismatch on the confirmation field', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, 'different')

    await (wrapper.vm as any).registerUser()
    await nextTick()

    expect(wrapper.find('[data-test="error-register-confirm"]').text()).toMatch(/correspondent pas/i)
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('does not repeat the mismatch in the form-level block', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, 'different')

    await (wrapper.vm as any).registerUser()
    await nextTick()

    // The mismatch must be reported — on the field — for the absence below to mean anything.
    expect(wrapper.find('[data-test="error-register-confirm"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="register-form-error"]').exists()).toBe(false)
  })

  it('clears the mismatch as soon as the confirmation matches', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, 'different')
    await (wrapper.vm as any).registerUser()
    await nextTick()
    expect(wrapper.find('[data-test="error-register-confirm"]').exists()).toBe(true)

    ;(wrapper.vm as any).userRegistered.confirmPassword = COMPLIANT
    await nextTick()

    expect(wrapper.find('[data-test="error-register-confirm"]').exists()).toBe(false)
  })

  // The backend collapses "username taken" and "email taken" into one generic failure, so the page
  // cannot point at a field. It stays reported for the whole form.
  it('keeps a server-side registration failure at form level', async () => {
    registerMock.mockImplementation(async (_payload: unknown, _onSuccess: () => void, onError: (e: unknown) => void) => {
      onError(new Error('rejected'))
    })
    const wrapper = mountPage()
    await fillRegistration(wrapper, COMPLIANT)

    await (wrapper.vm as any).registerUser()
    await nextTick()

    expect(wrapper.find('[data-test="register-form-error"]').text()).toMatch(/inscription/i)
    expect(wrapper.find('[data-test="error-register-confirm"]').exists()).toBe(false)
  })

  it('shows the password rules under the password field', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, COMPLIANT)

    const rules = wrapper.find('[data-test="password-rules"]')
    expect(rules.text()).toContain('Au moins 12 caractères')
    expect(rules.text()).toContain('Différent de votre adresse e-mail')
  })

  it('refuses a password that breaks the rules before sending anything', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, 'short-pass', 'short-pass')

    await (wrapper.vm as any).registerUser()
    await nextTick()

    expect(registerMock).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="error-register-password"]').text()).toContain('au moins 12 caractères')
    expect(wrapper.find('[data-test="rule-too_short"]').classes()).toContain('password-rule--unmet')
  })

  // The server decides: it can refuse what the checklist allowed, e.g. when the policy failed to load.
  it('reports a refusal by the password policy on the password field', async () => {
    registerMock.mockImplementation(async (_payload: unknown, _onSuccess: () => void, onError: (e: unknown) => void) => {
      onError({
        isAxiosError: true,
        response: { status: 400, data: { errorKey: 'domain.user.password.policy_violation', reasons: ['equals_email'] } },
      })
    })
    const wrapper = mountPage()
    await fillRegistration(wrapper, COMPLIANT)

    await (wrapper.vm as any).registerUser()
    await nextTick()

    expect(wrapper.find('[data-test="error-register-password"]').text())
      .toBe('Le mot de passe doit être différent de votre adresse e-mail.')
    expect(wrapper.find('[data-test="register-form-error"]').exists()).toBe(false)
  })

  it('clears the password error as soon as the password is edited', async () => {
    const wrapper = mountPage()
    await fillRegistration(wrapper, 'short-pass', 'short-pass')
    await (wrapper.vm as any).registerUser()
    await nextTick()
    expect(wrapper.find('[data-test="error-register-password"]').exists()).toBe(true)

    ;(wrapper.vm as any).userRegistered.password = 'short-pass-longer'
    await nextTick()

    expect(wrapper.find('[data-test="error-register-password"]').exists()).toBe(false)
  })
})
