// frontend/plugins/auth.js
export default defineNuxtPlugin((nuxtApp) => {
    const authStore = useAuthStore()

    // 创建一个自带 Token 拦截器的自定义 fetch 实例
    const apiFetch = $fetch.create({
        onRequest({ options }) {
            // 1. 获取 Token
            let currentToken = authStore.token
            if (!currentToken && typeof window !== 'undefined') {
                currentToken = localStorage.getItem('token')
            }

            // 2. 💡 暴力注入 Header（解决 ofetch 吞 Header 的 BUG）
            if (currentToken) {
                // 放弃直接修改原对象，强制重写整个 headers 对象！
                options.headers = {
                    ...options.headers,
                    Authorization: `Bearer ${currentToken}`
                }
            }
        },
        onResponseError({ response }) {
            if (response.status === 401) {
                console.warn('Token 无效或过期 (401)，正在退出...')
                authStore.logout()
            } else if (response.status === 403) {
                // 💡 遇到 403 先不要退出，只打印错误，方便我们排查！
                console.error('权限不足 (403)，后端拦截了请求！请检查 F12 Network 面板！')
            }
        },
    })

    // 💡 关键魔法：把它挂载到全局，这样你在任何地方直接写 $apiFetch('/api/...') 都不会报错！
    globalThis.$apiFetch = apiFetch

    // 同时保留 Nuxt 的标准 provide 写法，做到双重保险
    return {
        provide: {
            apiFetch,
        },
    }
})