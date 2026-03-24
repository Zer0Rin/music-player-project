export default defineNuxtRouteMiddleware((to) => {
    if (to.path === '/login') return

    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
        return navigateTo('/login')
    }
})