import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { computed, ref } from 'vue'
import VerifyEmailPage from '../../pages/verify-email.vue'

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

function mountPage(options: {
  token?: string | null
  verifyResult?: string | null
  verifyEmail?: ReturnType<typeof vi.fn>
  emailVerifiedRef?: ReturnType<typeof ref<boolean>>
} = {}) {
  const {
    token = 'test-token',
    verifyResult = null,
    verifyEmail = vi.fn().mockResolvedValue('success'),
    emailVerifiedRef = ref(false),
  } = options

  vi.stubGlobal('useRoute', () => ({
    params: {},
    query: token ? { token } : {},
  }))

  vi.stubGlobal('useEmailVerification', () => ({
    verifyResult: ref(verifyResult),
    isVerifying: ref(false),
    isResending: ref(false),
    isResendOnCooldown: ref(false),
    resendCooldown: ref(0),
    verifyEmail,
    resendVerificationEmail: vi.fn(),
  }))

  vi.stubGlobal('useConsent', () => ({
    emailVerified: emailVerifiedRef,
    userEmail: ref(null),
    consentRequired: ref(false),
    consentChecked: ref(false),
    tosAccepted: ref(false),
    privacyAccepted: ref(false),
    canSubmit: computed(() => false),
    isSubmitting: computed(() => false),
    tosVersion: '1.0',
    checkConsentStatus: vi.fn(),
    submitConsent: vi.fn(),
    clearConsentCache: vi.fn(),
  }))

  return shallowMount(VerifyEmailPage, {
    global: {
      stubs: {
        NuxtLink: { template: '<a><slot /></a>' },
      },
    },
  })
}

describe('pages/verify-email', () => {
  it('calls verifyEmail with the token on mount', async () => {
    const verifyEmail = vi.fn().mockResolvedValue('success')
    mountPage({ verifyEmail })
    await flushPromises()
    expect(verifyEmail).toHaveBeenCalledWith('test-token')
  })

  it('shows invalid link when no token is in the query', async () => {
    const wrapper = mountPage({ token: null, verifyResult: null })
    await flushPromises()
    expect(wrapper.text()).toContain('Lien invalide')
  })

  it('shows success state when verifyResult is success', async () => {
    const wrapper = mountPage({ verifyResult: 'success' })
    await flushPromises()
    expect(wrapper.text()).toContain('E-mail vérifié')
  })

  it('shows expired state when verifyResult is expired', async () => {
    const wrapper = mountPage({ verifyResult: 'expired' })
    await flushPromises()
    expect(wrapper.text()).toContain('Lien expiré')
  })

  it('shows invalid link state when verifyResult is not_found', async () => {
    const wrapper = mountPage({ verifyResult: 'not_found' })
    await flushPromises()
    expect(wrapper.text()).toContain('Lien invalide')
  })

  it('sets emailVerified to true on successful verification', async () => {
    const emailVerifiedRef = ref(false)
    mountPage({ emailVerifiedRef, verifyEmail: vi.fn().mockResolvedValue('success') })
    await flushPromises()
    expect(emailVerifiedRef.value).toBe(true)
  })

  it('does not set emailVerified when verification fails', async () => {
    const emailVerifiedRef = ref(false)
    mountPage({ emailVerifiedRef, verifyEmail: vi.fn().mockResolvedValue('expired') })
    await flushPromises()
    expect(emailVerifiedRef.value).toBe(false)
  })
})
