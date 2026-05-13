import { config } from '@vue/test-utils'
import { afterEach, beforeEach, vi } from 'vitest'
import { computed, onMounted, onUnmounted, reactive, ref, watch, watchEffect } from 'vue'

// Simulate common Nuxt auto-imported Vue APIs used directly in components.
vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('reactive', reactive)
vi.stubGlobal('watch', watch)
vi.stubGlobal('watchEffect', watchEffect)
vi.stubGlobal('onMounted', onMounted)
vi.stubGlobal('onUnmounted', onUnmounted)
vi.stubGlobal('defineNuxtRouteMiddleware', (guard: any) => guard)

// Stub auto-imported composables that require Nuxt runtime context.
vi.stubGlobal('useLocalStorage', (key: string, defaultValue: any) => ref(defaultValue))
vi.stubGlobal('useDark', () => ({
  isDark: ref(false),
  value: ref('light'),
  preference: computed(() => 'system'),
  toggle: vi.fn(),
  setPreference: vi.fn(),
}))
vi.stubGlobal('useTransaction', () => ({
  findByDate: vi.fn(),
  saveTransaction: vi.fn(),
  deleteTransaction: vi.fn(),
  editTransaction: vi.fn(),
  findTransactionById: vi.fn(),
  confirmPreviewTransaction: vi.fn(),
  confirmVirtualTransaction: vi.fn(),
}))
vi.stubGlobal('useCsvImport', () => ({
  validateCsvFile: vi.fn(),
  importTransactionsFromCsv: vi.fn(),
  downloadCsvExport: vi.fn(),
}))
vi.stubGlobal('useBookletOrder', (items: any) => ({
  orderedItems: computed(() => items.value),
  draggedIndex: ref(null),
  dragOverIndex: ref(null),
  onDragStart: vi.fn(),
  onDragOver: vi.fn(),
  onDrop: vi.fn(),
  onDragEnd: vi.fn(),
}))
vi.stubGlobal('useSubTagOrder', (_parentId: string, items: any) => ({
  orderedItems: computed(() => items.value),
  draggedIndex: ref(null),
  dragOverIndex: ref(null),
  onDragStart: vi.fn(),
  onDragOver: vi.fn(),
  onDrop: vi.fn(),
  onDragEnd: vi.fn(),
}))

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
