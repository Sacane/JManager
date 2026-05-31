import { config } from '@vue/test-utils'
import { afterEach, beforeEach, vi } from 'vitest'
import { computed, onBeforeUnmount, onMounted, onUnmounted, reactive, readonly, ref, watch, watchEffect } from 'vue'

// Prevent happy-dom from throwing when it tries to load image resources via the filesystem.
// In tests the document base URL is file://, so /favicon.ico resolves to an unresolvable
// file:/// path. Silencing the src setter is the minimal, non-invasive fix.
Object.defineProperty(window.HTMLImageElement.prototype, 'src', {
  configurable: true,
  set() {},
  get() { return '' },
})

// Simulate common Nuxt auto-imported Vue APIs used directly in components.
vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('reactive', reactive)
vi.stubGlobal('readonly', readonly)
vi.stubGlobal('watch', watch)
vi.stubGlobal('watchEffect', watchEffect)
vi.stubGlobal('onMounted', onMounted)
vi.stubGlobal('onUnmounted', onUnmounted)
vi.stubGlobal('onBeforeUnmount', onBeforeUnmount)
vi.stubGlobal('defineNuxtRouteMiddleware', (guard: any) => guard)

// Nuxt runtime
vi.stubGlobal('useState', vi.fn((_key: string, init?: () => unknown) => ref(init ? init() : null)))
vi.stubGlobal('navigateTo', vi.fn())
vi.stubGlobal('useRuntimeConfig', vi.fn(() => ({ public: { apiUrl: 'http://localhost:8080/api/' } })))
vi.stubGlobal('definePageMeta', vi.fn())
vi.stubGlobal('useRoute', vi.fn(() => ({ params: {}, query: {} })))
vi.stubGlobal('useRouter', vi.fn(() => ({ push: vi.fn(), replace: vi.fn() })))

// PrimeVue composables
vi.stubGlobal('useToast', vi.fn(() => ({ add: vi.fn() })))

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

// App composables — default stubs; override per test file via vi.stubGlobal in beforeEach
vi.stubGlobal('useLoading', vi.fn(() => ({
  isLoading: ref(false),
  pendingScopes: ref<Record<string, number>>({}),
  isScopeLoading: vi.fn(() => false),
  startLoading: vi.fn(),
  stopLoading: vi.fn(),
  withLoading: vi.fn(async (action: () => Promise<unknown>) => action()),
})))

vi.stubGlobal('useJToast', vi.fn(() => ({
  success: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  errorAxios: vi.fn(),
})))

vi.stubGlobal('useAuth', vi.fn(() => ({
  user: ref(null),
  isAuthenticated: ref(false),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  isAdmin: ref(false),
  tryRefresh: vi.fn(),
  initializeSession: vi.fn(),
})))

vi.stubGlobal('useConsent', vi.fn(() => ({
  tosAccepted: ref(false),
  privacyAccepted: ref(false),
  canSubmit: computed(() => false),
  isSubmitting: computed(() => false),
  tosVersion: '1.0',
  consentRequired: ref(false),
  consentChecked: ref(false),
  emailVerified: ref(true),
  userEmail: ref(null),
  checkConsentStatus: vi.fn().mockResolvedValue(false),
  submitConsent: vi.fn(),
  clearConsentCache: vi.fn(),
})))

vi.stubGlobal('useEmailVerification', vi.fn(() => ({
  verifyResult: ref(null),
  isVerifying: ref(false),
  isResending: ref(false),
  isResendOnCooldown: ref(false),
  resendCooldown: ref(0),
  verifyEmail: vi.fn().mockResolvedValue('success'),
  resendVerificationEmail: vi.fn(),
})))

vi.stubGlobal('useAdmin', vi.fn(() => ({
  users: ref([]),
  totalUsers: ref(0),
  totalPages: ref(0),
  currentPage: ref(0),
  pageSize: ref(10),
  isLoading: ref(false),
  fetchUsers: vi.fn(),
})))

vi.stubGlobal('useAppVersion', vi.fn(() => ({
  version: ref(null),
  fetchVersion: vi.fn(),
})))

vi.stubGlobal('useFeatureFlags', vi.fn(() => ({
  flags: readonly(ref([])),
  isFetching: ref(false),
  isToggling: ref(false),
  isEnabled: vi.fn(() => false),
  fetchFlags: vi.fn(),
  toggleFlag: vi.fn(),
})))

vi.stubGlobal('useQuery', vi.fn(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
})))

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
