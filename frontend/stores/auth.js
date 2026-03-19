import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: typeof window !== 'undefined' ? localStorage.getItem('token') : null,
        user: typeof window !== 'undefined' ? JSON.parse(localStorage.getItem('user') || 'null') : null,
    }),

    getters: {
        isLoggedIn: (state) => !!state.token,
        isAdmin: (state) => state.user?.role === 'ADMIN',
    },

    actions: {
        async login(username, password) {
            const data = await $fetch('/api/auth/login', {
                method: 'POST',
                body: { username, password },
            })
            this.setAuth(data)
            return data
        },

        async register(username, password, nickname) {
            const data = await $fetch('/api/auth/register', {
                method: 'POST',
                body: { username, password, nickname },
            })
            this.setAuth(data)
            return data
        },

        setAuth(data) {
            this.token = data.token
            this.user = data.user
            localStorage.setItem('token', data.token)
            localStorage.setItem('user', JSON.stringify(data.user))
        },

        logout() {
            this.token = null
            this.user = null
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            navigateTo('/login')
        },
    },
})