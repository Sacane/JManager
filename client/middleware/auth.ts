import useAuth from '@/composables/useAuth'

const authMiddleware = defineNuxtRouteMiddleware(async () => {
  if (import.meta.server) {
    return
  }

  const { initializeSession, isAuthenticated } = useAuth()
  await initializeSession()

  if (!isAuthenticated.value) {
    return navigateTo('/login')
  }
})

export default authMiddleware