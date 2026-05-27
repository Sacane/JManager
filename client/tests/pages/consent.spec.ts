import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, ref } from 'vue'
import ConsentPage from '../../pages/consent.vue'

// ---------------------------------------------------------------------------
// PrimeVue stubs
// ---------------------------------------------------------------------------
const CheckboxStub = {
  name: 'Checkbox',
  props: ['modelValue', 'inputId', 'binary'],
  emits: ['update:modelValue'],
  template: '<input :id="inputId" type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" data-testid="submit-btn" @click="$emit(\'click\')">{{ label }}</button>',
}

// ---------------------------------------------------------------------------
// Composable mocks
// ---------------------------------------------------------------------------
const checkConsentStatusMock = vi.fn().mockResolvedValue(true)
const submitConsentMock = vi.fn()
const toastSuccessMock = vi.fn()
const toastErrorAxiosMock = vi.fn()

function makeConsentMock(overrides: {
  tosAccepted?: boolean
  privacyAccepted?: boolean
  canSubmit?: boolean
  isSubmitting?: boolean
  consentRequired?: boolean
} = {}) {
  const tosAccepted = ref(overrides.tosAccepted ?? false)
  const privacyAccepted = ref(overrides.privacyAccepted ?? false)
  return {
    tosAccepted,
    privacyAccepted,
    canSubmit: computed(() => overrides.canSubmit ?? (tosAccepted.value && privacyAccepted.value)),
    isSubmitting: computed(() => overrides.isSubmitting ?? false),
    tosVersion: '1.0',
    consentRequired: ref(overrides.consentRequired ?? true),
    consentChecked: ref(false),
    checkConsentStatus: checkConsentStatusMock,
    submitConsent: submitConsentMock,
    clearConsentCache: vi.fn(),
  }
}

// ---------------------------------------------------------------------------
describe('pages/consent', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    checkConsentStatusMock.mockResolvedValue(true)
    submitConsentMock.mockResolvedValue(undefined)

    vi.stubGlobal('useConsent', () => makeConsentMock())
    vi.stubGlobal('useJToast', () => ({
      success: toastSuccessMock,
      error: vi.fn(),
      errorAxios: toastErrorAxiosMock,
    }))
    vi.stubGlobal('navigateTo', vi.fn())
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('onMounted', (fn: () => void) => fn())
  })

  function mountPage(consentOverrides = {}) {
    vi.stubGlobal('useConsent', () => makeConsentMock(consentOverrides))
    return shallowMount(ConsentPage, {
      global: {
        stubs: {
          Checkbox: CheckboxStub,
          Button: ButtonStub,
        },
      },
    })
  }

  // -------------------------------------------------------------------------
  it('renders both consent checkboxes', () => {
    const wrapper = mountPage()
    expect(wrapper.find('#consent-tos').exists()).toBe(true)
    expect(wrapper.find('#consent-privacy').exists()).toBe(true)
  })

  it('submit button is disabled when neither checkbox is checked', () => {
    const wrapper = mountPage({ tosAccepted: false, privacyAccepted: false })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button is disabled when only tos is checked', () => {
    const wrapper = mountPage({ tosAccepted: true, privacyAccepted: false })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button is disabled when only privacy is checked', () => {
    const wrapper = mountPage({ tosAccepted: false, privacyAccepted: true })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('submit button is enabled when both checkboxes are checked', () => {
    const wrapper = mountPage({ tosAccepted: true, privacyAccepted: true, canSubmit: true })
    const btn = wrapper.find('[data-testid="submit-btn"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('calls submitConsent when the enabled button is clicked', async () => {
    const wrapper = mountPage({ tosAccepted: true, privacyAccepted: true, canSubmit: true })
    await wrapper.find('[data-testid="submit-btn"]').trigger('click')
    expect(submitConsentMock).toHaveBeenCalledTimes(1)
  })

  it('calls checkConsentStatus on mount and redirects away if consent is not required', async () => {
    checkConsentStatusMock.mockResolvedValue(false)
    const navigateTo = vi.fn()
    vi.stubGlobal('navigateTo', navigateTo)

    mountPage({ consentRequired: false })
    await flushPromises()

    expect(checkConsentStatusMock).toHaveBeenCalledTimes(1)
    expect(navigateTo).toHaveBeenCalledWith('/')
  })

  it('shows the TOS version in the label', () => {
    const wrapper = mountPage()
    expect(wrapper.text()).toContain('v1.0')
  })
})
