// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },

  app: {
    head: {
      viewport: 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no',
      meta: [
        { name: 'theme-color', content: '#050505' },
        { name: 'apple-mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-status-bar-style', content: 'black-translucent' },
      ],
    },
  },

  modules: [
    ['@pinia/nuxt', {
      autoImports: ['defineStore'],
      storesDirs: ['~/stores'],
    }],
    '@nuxtjs/google-fonts',
  ],

  imports: {
    dirs: ['stores'],
  },

  css: ['~/assets/css/main.css'],

  googleFonts: {
    families: {
      'Noto+Sans+SC': [300, 400, 500, 600, 700],
    },
    display: 'swap',
  },

  // 开发时代理后端 API，避免 CORS 问题
  nitro: {
    devProxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true,
      },
    },
  },

  compatibilityDate: '2024-04-03',
})
