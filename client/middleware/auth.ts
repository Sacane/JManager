import useAuth from '@/composables/useAuth'

const authMiddleware = defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) {
    return
  }

  const { initializeSession, isAuthenticated } = useAuth()
  const { checkConsentStatus, clearConsentCache } = useConsent()

  await initializeSession()

  if (!isAuthenticated.value) {
    // Clear stale consent cache so the next user always gets a fresh check
    clearConsentCache()
    return navigateTo('/login')
  }

  // Don't intercept navigation to /consent itself — avoids infinite redirect loop
  if (to.path === '/consent') return

  const required = await checkConsentStatus()
  if (required) {
    return navigateTo('/consent')
  }

  // Don't intercept navigation to /force-password-change itself — avoids infinite redirect loop
  if (to.path === '/force-password-change') return

  const { mustChangePassword } = useConsent()
  if (mustChangePassword.value) {
    return navigateTo('/force-password-change')
  }
})

export default authMiddleware
