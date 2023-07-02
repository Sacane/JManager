export default defineNuxtRouteMiddleware((to) => {
  if (to.query.id !== 'admin') {
    return navigateTo('403', {
      replace: true,
    })
  }
})
