import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import SettingsPage from '../../pages/settings/index.vue'

const getSettingsMock = vi.fn().mockResolvedValue({
  projectionWindowDays: 21,
  bookletCycles: [
    {
      bookletId: 'acc-1',
      label: 'Compte principal',
      monthlyPeriodStartDay: 28,
      monthlyPeriodEndDay: null,
    },
  ],
})

const updateSettingsMock = vi.fn().mockResolvedValue({
  projectionWindowDays: 30,
  bookletCycles: [
    {
      bookletId: 'acc-1',
      label: 'Compte principal',
      monthlyPeriodStartDay: 27,
      monthlyPeriodEndDay: 26,
    },
  ],
})

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({
    getSettings: getSettingsMock,
    updateSettings: updateSettingsMock,
  }),
}))

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

function mountSettingsPage(activeScopes: string[] = [], emailVerified = true) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useConsent', () => ({
    emailVerified: ref(emailVerified),
    userEmail: ref('user@example.com'),
    consentRequired: ref(false),
    consentChecked: ref(false),
    tosAccepted: ref(false),
    privacyAccepted: ref(false),
    canSubmit: { value: false },
    isSubmitting: { value: false },
    tosVersion: '1.0',
    checkConsentStatus: vi.fn(),
    submitConsent: vi.fn(),
    clearConsentCache: vi.fn(),
  }))
  vi.stubGlobal('useEmailVerification', () => ({
    verifyResult: ref(null),
    isVerifying: ref(false),
    isResending: ref(false),
    isResendOnCooldown: ref(false),
    resendCooldown: ref(0),
    verifyEmail: vi.fn(),
    resendVerificationEmail: vi.fn(),
  }))

  return shallowMount(SettingsPage)
}

describe('pages/settings/index', () => {
  it('renders settings sections and loads API data', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    expect(getSettingsMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Parametres utilisateur')
    expect(wrapper.text()).toContain('Apparence')
    expect(wrapper.text()).toContain('Projection')
    expect(wrapper.text()).toContain('Cycle mensuel par compte')
    expect(wrapper.text()).toContain('Compte principal')
  })

  it('hides email verification section when emailVerified is true', async () => {
    const wrapper = mountSettingsPage([], true)
    await flushPromises()
    expect(wrapper.find('[data-test="email-verification-section"]').exists()).toBe(false)
  })

  it('shows email verification section when emailVerified is false', async () => {
    const wrapper = mountSettingsPage([], false)
    await flushPromises()
    expect(wrapper.find('[data-test="email-verification-section"]').exists()).toBe(true)
  })

  it('displays the user email in the verification section', async () => {
    const wrapper = mountSettingsPage([], false)
    await flushPromises()
    expect(wrapper.find('[data-test="email-display"]').text()).toContain('user@example.com')
  })

  it('submits updated settings payload', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    const projectionInput = wrapper.find('[data-test="projection-window-input"]')
    await projectionInput.setValue('30')

    const cycleSelect = wrapper.find('[data-test="cycle-select-acc-1"]')
    await cycleSelect.setValue('27')

    const cycleEndSelect = wrapper.find('[data-test="cycle-end-select-acc-1"]')
    await cycleEndSelect.setValue('26')

    await wrapper.find('[data-test="save-settings-btn"]').trigger('click')

    expect(updateSettingsMock).toHaveBeenCalledWith({
      projectionWindowDays: 30,
      bookletCycles: [
        {
          bookletId: 'acc-1',
          monthlyPeriodStartDay: 27,
          monthlyPeriodEndDay: 26,
        },
      ],
    })
  })
})
