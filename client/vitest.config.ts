import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [
    vue({
      template: {
        // Disable asset URL transformation in the test environment.
        // Without this, <img src="/favicon.ico"> is transformed into a Vite
        // import that Vite cannot resolve (public/ assets are not imported,
        // they are served as-is). The Nuxt production build is unaffected —
        // it uses its own plugin pipeline via nuxt.config.ts.
        transformAssetUrls: false,
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./', import.meta.url)),
      '~': fileURLToPath(new URL('./', import.meta.url)),
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    include: ['tests/**/*.spec.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
    },
  },
})
