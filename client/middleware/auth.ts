export default defineNuxtRouteMiddleware((to, from) => {
    const {user} = useAuth()
    if(user === null) {
        return navigateTo('/login')
    }
})