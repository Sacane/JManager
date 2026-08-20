import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, ref } from 'vue'
import LoginPage from '../../pages/login.vue'

// ---------------------------------------------------------------------------
// PrimeVue stubs
// ---------------------------------------------------------------------------
const InputTextStub = {
  name: 'InputText',
  props: ['modelValue', 'type', 'placeholder', 'maxlength', 'disabled'],
  emits: ['update:modelValue'],
  template: '<input :type="type" :value="modelValue" :placeholder="placeholder" :maxlength="maxlength" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['type', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :type="type" :disabled="disabled || loading" data-testid="submit-btn" @click="$emit(\'click\')">slot</button>',
}

const CheckboxStub = {
  name: 'Checkbox',
  props: ['modelValue', 'inputId', 'binary'],
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :id="inputId" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
}

const NuxtLinkStub = {
  name: 'NuxtLink',
  props: ['to', 'target'],
  template: '<a :href="to"><slot /></a>',
}

// ---------------------------------------------------------------------------
// Composable mocks
// ---------------------------------------------------------------------------
const loginMock = vi.fn()
const registerMock = vi.fn()
const isEnabledMock = vi.fn(() => false)
const toastSuccessMock = vi.fn()
const toastErrorAxiosMock = vi.fn()

// FeatureGate renders its default slot only when isEnabled returns true for the given key.
// We wire it to the same isEnabledMock so flag tests work end-to-end through the component.
const FeatureGateStub = {
  name: 'FeatureGate',
  props: ['feature'],
  setup(props: { feature: string }) {
    const enabled = computed(() => isEnabledMock(props.feature))
    return { enabled }
  },
  template: '<slot v-if="enabled" /><slot v-else name="fallback" />',
}

function mountPage() {
  return shallowMount(LoginPage, {
    global: {
      stubs: {
        InputText: InputTextStub,
        Button: ButtonStub,
        Checkbox: CheckboxStub,
        NuxtLink: NuxtLinkStub,
        FeatureGate: FeatureGateStub,
      },
    },
  })
}

// ---------------------------------------------------------------------------
describe('pages/login', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    isEnabledMock.mockReturnValue(false)

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
      isEnabled: isEnabledMock,
      fetchFlags: vi.fn(),
      toggleFlag: vi.fn(),
    })))

    vi.stubGlobal('useJToast', vi.fn(() => ({
      success: toastSuccessMock,
      error: vi.fn(),
      errorAxios: toastErrorAxiosMock,
    })))
  })

  // -------------------------------------------------------------------------
  describe('login mode (default)', () => {
    it('renders the email and password fields by default', () => {
      const wrapper = mountPage()
      expect(wrapper.find('#email').exists()).toBe(true)
      expect(wrapper.find('#password').exists()).toBe(true)
    })

    it('email field has type email and maxlength 255', () => {
      const wrapper = mountPage()
      const emailInput = wrapper.find('#email')
      expect(emailInput.attributes('type')).toBe('email')
      expect(emailInput.attributes('maxlength')).toBe('255')
    })

    it('does not render the registration form initially', () => {
      const wrapper = mountPage()
      expect(wrapper.find('#reg-username').exists()).toBe(false)
    })

    it('does not show the switch-to-register toggle when flag is disabled', () => {
      isEnabledMock.mockReturnValue(false)
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="switch-to-register"]').exists()).toBe(false)
    })

    it('shows the switch-to-register toggle when USER_REGISTRATION flag is enabled', () => {
      isEnabledMock.mockReturnValue(true)
      const wrapper = mountPage()
      expect(wrapper.find('[data-testid="switch-to-register"]').exists()).toBe(true)
    })

    it('calls login with email and password on form submit', async () => {
      const wrapper = mountPage()
      await wrapper.find('#email').setValue('alice@example.com')
      await wrapper.find('#password').setValue('secret')
      await wrapper.find('form').trigger('submit')
      expect(loginMock).toHaveBeenCalledWith(
        { email: 'alice@example.com', password: 'secret' },
        expect.any(Function),
      )
    })
  })

  // -------------------------------------------------------------------------
  describe('switching to register mode', () => {
    beforeEach(() => {
      isEnabledMock.mockReturnValue(true)
    })

    it('switches to register form when toggle is clicked', async () => {
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')
      expect(wrapper.find('#reg-username').exists()).toBe(true)
      expect(wrapper.find('#email').exists()).toBe(false)
    })

    it('shows all registration fields', async () => {
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')
      expect(wrapper.find('#reg-username').exists()).toBe(true)
      expect(wrapper.find('#reg-email').exists()).toBe(true)
      expect(wrapper.find('#reg-password').exists()).toBe(true)
      expect(wrapper.find('#reg-confirm-password').exists()).toBe(true)
      expect(wrapper.find('#tos-accepted').exists()).toBe(true)
      expect(wrapper.find('#privacy-accepted').exists()).toBe(true)
    })

    it('shows a back-to-login toggle in register mode', async () => {
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')
      expect(wrapper.find('[data-testid="switch-to-login"]').exists()).toBe(true)
    })

    it('returns to login form when switch-to-login is clicked', async () => {
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')
      await wrapper.find('[data-testid="switch-to-login"]').trigger('click')
      expect(wrapper.find('#email').exists()).toBe(true)
      expect(wrapper.find('#reg-username').exists()).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  describe('registration form submission', () => {
    beforeEach(() => {
      isEnabledMock.mockReturnValue(true)
    })

    it('calls register with all fields on valid submit', async () => {
      registerMock.mockImplementation((_payload: unknown, onSuccess: () => void) => {
        onSuccess()
      })
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')

      await wrapper.find('#reg-username').setValue('bob')
      await wrapper.find('#reg-email').setValue('bob@example.com')
      await wrapper.find('#reg-password').setValue('pass123')
      await wrapper.find('#reg-confirm-password').setValue('pass123')
      await wrapper.find('#tos-accepted').setValue(true)
      await wrapper.find('#privacy-accepted').setValue(true)

      await wrapper.find('form').trigger('submit')

      expect(registerMock).toHaveBeenCalledWith(
        expect.objectContaining({
          username: 'bob',
          email: 'bob@example.com',
          password: 'pass123',
          confirmPassword: 'pass123',
          tosAccepted: true,
          privacyAccepted: true,
        }),
        expect.any(Function),
        expect.any(Function),
      )
    })

    it('does not call register when passwords do not match', async () => {
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')

      await wrapper.find('#reg-username').setValue('bob')
      await wrapper.find('#reg-email').setValue('bob@example.com')
      await wrapper.find('#reg-password').setValue('pass123')
      await wrapper.find('#reg-confirm-password').setValue('other456')
      await wrapper.find('#tos-accepted').setValue(true)
      await wrapper.find('#privacy-accepted').setValue(true)

      await wrapper.find('form').trigger('submit')

      expect(registerMock).not.toHaveBeenCalled()
      expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    })

    it('switches back to login mode after successful registration', async () => {
      registerMock.mockImplementation((_payload: unknown, onSuccess: () => void) => {
        onSuccess()
      })
      const wrapper = mountPage()
      await wrapper.find('[data-testid="switch-to-register"]').trigger('click')

      await wrapper.find('#reg-username').setValue('bob')
      await wrapper.find('#reg-email').setValue('bob@example.com')
      await wrapper.find('#reg-password').setValue('pass123')
      await wrapper.find('#reg-confirm-password').setValue('pass123')
      await wrapper.find('#tos-accepted').setValue(true)
      await wrapper.find('#privacy-accepted').setValue(true)

      await wrapper.find('form').trigger('submit')

      expect(wrapper.find('#email').exists()).toBe(true)
      expect(toastSuccessMock).toHaveBeenCalledTimes(1)
    })
  })
})
