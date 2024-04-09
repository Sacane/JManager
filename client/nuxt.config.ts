// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  app: {
    head: {
      title: 'Jmanager',
      link: [{
        rel: 'icon',
        type: 'image/x-icon',
        href: '/favicon.ico',
      }],
    },
  },
  ssr: false,
  css: [
    '@unocss/reset/tailwind.css',
    'primevue/resources/themes/lara-light-blue/theme.css',
    'primevue/resources/primevue.css',
  ],
  sourcemap: {
    server: true,
    client: false,
  },
  imports: {
    dirs: ['stores'],
  },
  components: [{
    path: '~/components',
    pathPrefix: false,
  }],

  modules: ['@vueuse/nuxt', '@unocss/nuxt', '@pinia/nuxt'],
  pinia: {
    autoImports: ['defineStore', ['defineStore', 'definePiniaStore'], 'storeToRefs'],
  },
})
