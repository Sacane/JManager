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
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { ref: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap' },
      ],
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

  },

  colorMode: {
    preference: 'system',
    fallback: 'light',
    classPrefix: '',
    classSuffix: '',
    storageKey: 'color-scheme',
  },

  ssr: false,
  compatibilityDate: '2024-09-09',
})
