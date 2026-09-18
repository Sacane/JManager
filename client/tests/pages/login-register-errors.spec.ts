import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, nextTick, ref } from 'vue'
import FieldError from '../../components/FieldError.vue'
import PasswordField from '../../components/PasswordField.vue'
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
        FieldError: false,
      },
      components: { PasswordField, FieldError },
    },
  })
}

async function fillRegistration(wrapper: ReturnType<typeof mountPage>, confirm: string) {
  const vm = wrapper.vm as any
  vm.switchMode('register')
  await nextTick()
  vm.userRegistered.username = 'johan'
  vm.userRegistered.email = 'johan@example.com'
  vm.userRegistered.password = 'secret-1'
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

    ;(wrapper.vm as any).userRegistered.confirmPassword = 'secret-1'
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
    await fillRegistration(wrapper, 'secret-1')

    await (wrapper.vm as any).registerUser()
    await nextTick()

    expect(wrapper.find('[data-test="register-form-error"]').text()).toMatch(/inscription/i)
    expect(wrapper.find('[data-test="error-register-confirm"]').exists()).toBe(false)
  })
})
