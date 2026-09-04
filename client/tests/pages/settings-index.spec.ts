import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import PasswordField from '../../components/PasswordField.vue'
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

vi.mock('~/composables/useChangePassword', () => ({
  default: () => ({
    currentPassword: ref(''),
    newPassword: ref(''),
    confirmPassword: ref(''),
    confirmPasswordError: ref(null),
    isSubmitting: ref(false),
    changePassword: vi.fn(),
  }),
}))

const confirmRequireMock = vi.fn()

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

function mountSettingsPage(activeScopes: string[] = [], emailVerified = true) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require: confirmRequireMock }))
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
    mustChangePassword: ref(false),
    checkConsentStatus: vi.fn(),
    submitConsent: vi.fn(),
    clearConsentCache: vi.fn(),
    clearMustChangePassword: vi.fn(),
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

  return shallowMount(SettingsPage, {
    global: {
      // Rendered for real: the password assertions target the actual input, not the stub.
      stubs: { PasswordField: false },
      components: { PasswordField },
    },
  })
}

describe('pages/settings/index unsaved changes', () => {
  let leaveGuard: (() => boolean) | null

  beforeEach(() => {
    getSettingsMock.mockClear()
    updateSettingsMock.mockClear()
    leaveGuard = null
    confirmRequireMock.mockClear()
    vi.stubGlobal('onBeforeRouteLeave', (guard: () => boolean) => {
      leaveGuard = guard
    })
  })

  it('names the scope of the settings save action', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-test="save-settings-btn"]').text()).toContain('la projection et les cycles')
  })

  it('shows nothing pending right after loading', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-test="unsaved-settings-badge"]').exists()).toBe(false)
  })

  it('flags pending changes once a setting is edited', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    ;(wrapper.vm as any).projectionWindowDays = 42
    await nextTick()

    expect(wrapper.find('[data-test="unsaved-settings-badge"]').exists()).toBe(true)
  })

  it('clears the pending flag once the settings are saved', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    ;(wrapper.vm as any).projectionWindowDays = 30
    await nextTick()
    expect(wrapper.find('[data-test="unsaved-settings-badge"]').exists()).toBe(true)

    await (wrapper.vm as any).saveUserSettings()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('[data-test="unsaved-settings-badge"]').exists()).toBe(false)
  })

  it('lets the user leave when nothing is pending', async () => {
    mountSettingsPage()
    await flushPromises()
    await flushPromises()

    expect(leaveGuard).not.toBeNull()
    expect(leaveGuard!()).toBe(true)
  })

  it('asks for confirmation before leaving with pending changes', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    ;(wrapper.vm as any).projectionWindowDays = 42
    await nextTick()

    const decision = leaveGuard!() as unknown as Promise<boolean>
    expect(confirmRequireMock).toHaveBeenCalledTimes(1)

    // Staying on the page must cancel the navigation.
    confirmRequireMock.mock.calls[0]![0].reject()
    await expect(decision).resolves.toBe(false)
  })

  it('lets the navigation through when the user accepts losing the changes', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    ;(wrapper.vm as any).projectionWindowDays = 42
    await nextTick()

    const decision = leaveGuard!() as unknown as Promise<boolean>
    confirmRequireMock.mock.calls[0]![0].accept()
    await expect(decision).resolves.toBe(true)
  })

  // The password form has its own scope and never touched the settings refs. This pins that
  // behaviour so a future refactor cannot turn it into the silent data loss UX-06 feared.
  it('keeps a pending settings edit when the password form is submitted', async () => {
    const wrapper = mountSettingsPage()
    await flushPromises()
    await flushPromises()

    ;(wrapper.vm as any).projectionWindowDays = 42
    await nextTick()

    await (wrapper.vm as any).changePassword()
    await flushPromises()

    expect((wrapper.vm as any).projectionWindowDays).toBe(42)
    expect(wrapper.find('[data-test="unsaved-settings-badge"]').exists()).toBe(true)
  })
})

describe('pages/settings/index', () => {
  beforeEach(() => {
    getSettingsMock.mockClear()
    updateSettingsMock.mockClear()
  })

  it('spells the user-facing French labels correctly', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    const text = wrapper.text()

    expect(text).toContain('Paramètres utilisateur')
    expect(text).toContain('Définis le nombre de jours')
    expect(text).toContain('prévisions')
    expect(text).toContain('Fenêtre de projection (7 à 60 jours)')
    expect(text).toContain('Enregistrer la projection et les cycles')
    expect(text).not.toMatch(/Parametres|parametres|Definis|previsions|Fenetre/)
  })

  it('renders settings sections and loads API data', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    expect(getSettingsMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Paramètres utilisateur')
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

  it('always shows the change password section', async () => {
    const wrapper = mountSettingsPage()
    expect(wrapper.find('[data-test="change-password-section"]').exists()).toBe(true)
  })

  it('renders all three password fields in the change password section', async () => {
    const wrapper = mountSettingsPage()
    expect(wrapper.find('[data-test="current-password-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="new-password-input"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="confirm-password-input"]').exists()).toBe(true)
  })

  it('renders the change password button', async () => {
    const wrapper = mountSettingsPage()
    expect(wrapper.find('[data-test="change-password-btn"]').exists()).toBe(true)
  })

  it('does not show confirm password error when null', async () => {
    const wrapper = mountSettingsPage()
    expect(wrapper.find('[data-test="confirm-password-error"]').exists()).toBe(false)
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
