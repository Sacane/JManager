import useAuth from '@/composables/useAuth'

const adminMiddleware = defineNuxtRouteMiddleware(async () => {
  if (import.meta.server) {
    return
  }

  const { initializeSession, isAuthenticated, isAdmin } = useAuth()
  await initializeSession()

  if (!isAuthenticated.value) {
    return navigateTo('/login')
  }

  if (!isAdmin.value) {
    return navigateTo('/')
  }
})

export default adminMiddleware
