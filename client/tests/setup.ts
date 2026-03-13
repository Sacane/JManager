import { config } from '@vue/test-utils'
import { computed, onMounted, onUnmounted, reactive, ref, watch, watchEffect } from 'vue'
import { afterEach, beforeEach, vi } from 'vitest'

// Simulate common Nuxt auto-imported Vue APIs used directly in components.
vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('reactive', reactive)
vi.stubGlobal('watch', watch)
vi.stubGlobal('watchEffect', watchEffect)
vi.stubGlobal('onMounted', onMounted)
vi.stubGlobal('onUnmounted', onUnmounted)

config.global.stubs = {
  Transition: false,
}

beforeEach(() => {
  vi.spyOn(console, 'warn').mockImplementation(() => {})
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})
