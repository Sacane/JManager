import process from 'node:process'
import fs from 'node:fs'
import runtimeConfig from './env.config.json'
import devRuntimeConfig from './env.dev.config.json'

const locales = fs.readdirSync('locales')
  .map(file => ({
    code: file.replace(/\.(yml|yaml|json)$/, ''),
    file,
  }))
const isDev = process.env.NODE_ENV === 'development'
// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  app: {
    head: {
      title: 'JManager',
      link: [{ rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }],
    },
  },
  runtimeConfig: isDev ? devRuntimeConfig : runtimeConfig,
  imports: { // add folders here to auto-import them in your application
    dirs: [
      'stores',
      'composables/**',
    ],
  },
  components: [{ path: '~/components', pathPrefix: false }],
  typescript: {
    tsConfig: {
      compilerOptions: {
        moduleResolution: 'bundler',
      },
    },
  },
  vite: {
    vue: {
      script: {
        defineModel: true,
        propsDestructure: true,
      },
    },
  },
  experimental: {
    typedPages: true,
  },

  // uncomment to disable SSR. This will basically make the app a SPA, like a normal Vue app, but with all the Nuxt goodies
  // ssr: false,

  // global CSS files
  css: [
    'primevue/resources/themes/lara-light-purple/theme.css',
  ],

  // plugin configurations
  modules: [
    '@nuxtjs/i18n',
    '@vueuse/nuxt',
    '@unocss/nuxt',
    '@nuxtjs/critters',
    '@nuxtjs/color-mode',
    '@pinia/nuxt',
    'nuxt-primevue',
  ],
  i18n: {
    langDir: 'locales',
    defaultLocale: 'en',
    locales,
  },
  colorMode: {
    preference: 'system',
    fallback: 'light',
    classPrefix: '',
    classSuffix: '',
    storageKey: 'color-scheme',
  },
  ssr: false,
})
