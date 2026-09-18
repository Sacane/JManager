import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import SettingsPage from '../../pages/settings/index.vue'
import { resolveMonthlyCycleRangeForTargetMonth } from '../../utils/monthlyCycleRange'

const getSettingsMock = vi.fn()

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({ getSettings: getSettingsMock, updateSettings: vi.fn() }),
}))

vi.mock('~/composables/useChangePassword', () => ({
  default: () => ({
    currentPassword: ref(''),
    newPassword: ref(''),
    confirmPassword: ref(''),
    fieldErrors: { currentPassword: null, newPassword: null, confirmPassword: null },
    isSubmitting: ref(false),
    changePassword: vi.fn(),
  }),
}))

interface Cycle { bookletId: string, label: string, monthlyPeriodStartDay: number, monthlyPeriodEndDay: number | null }

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

async function mountWith(cycles: Cycle[]) {
  getSettingsMock.mockResolvedValue({ projectionWindowDays: 15, bookletCycles: cycles })
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useConsent', () => ({ emailVerified: ref(true), userEmail: ref('u@example.com') }))
  vi.stubGlobal('useEmailVerification', () => ({
    isResending: ref(false),
    isResendOnCooldown: ref(false),
    resendCooldown: ref(0),
    resendVerificationEmail: vi.fn(),
  }))

  const wrapper = shallowMount(SettingsPage)
  await flushPromises()
  await flushPromises()
  await nextTick()
  return wrapper
}

function preview(wrapper: Awaited<ReturnType<typeof mountWith>>, bookletId: string) {
  return wrapper.find(`[data-test="cycle-preview-${bookletId}"]`)
}

function formatted(date: Date): string {
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`
}

describe('pages/settings/index monthly cycle preview', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Only Date is faked: faking the timers too would freeze the setTimeout based flush above.
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 18, 12, 0, 0))
  })

  afterEach(() => vi.useRealTimers())

  // The card this item merges with: "on ne sait pas quel cycle correspond à quel compte".
  it('groups each cycle under the full name of its account', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Compte courant professionnel partagé', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
      { bookletId: 'b-2', label: 'Compte courant personnel', monthlyPeriodStartDay: 1, monthlyPeriodEndDay: null },
    ])

    const groups = wrapper.findAll('fieldset[data-test="cycle-group"]')
    expect(groups).toHaveLength(2)

    // Looked up by name, not by position: the page sorts the accounts alphabetically.
    const professional = groups.find(group => group.find('legend').text() === 'Compte courant professionnel partagé')
    expect(professional).toBeDefined()
    // The controls live inside the group their account names, so nothing can be read against the
    // wrong account — visually or by a screen reader.
    expect(professional!.find('[data-test="cycle-select-b-1"]').exists()).toBe(true)
    expect(professional!.find('[data-test="cycle-select-b-2"]').exists()).toBe(false)
  })

  it('previews a cycle starting in the second half of the month', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Livret A', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
    ])

    const text = preview(wrapper, 'b-1').text()
    expect(text).toContain('25/08/2026')
    expect(text).toContain('24/09/2026')
    expect(text.toLowerCase()).toContain('septembre 2026')
  })

  // The case the help text got wrong: a start in the first half falls in the month itself.
  it('previews a cycle starting in the first half of the month', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Livret A', monthlyPeriodStartDay: 10, monthlyPeriodEndDay: null },
    ])

    const text = preview(wrapper, 'b-1').text()
    expect(text).toContain('10/09/2026')
    expect(text).toContain('09/10/2026')
  })

  it('reflects a custom end day', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Livret A', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: 20 },
    ])

    const text = preview(wrapper, 'b-1').text()
    expect(text).toContain('25/08/2026')
    expect(text).toContain('20/09/2026')
  })

  it('updates the edited account preview before saving, and only that one', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Livret A', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
      { bookletId: 'b-2', label: 'Livret B', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
    ])

    await wrapper.find('[data-test="cycle-select-b-1"]').setValue('10')
    await nextTick()

    expect(preview(wrapper, 'b-1').text()).toContain('10/09/2026')
    expect(preview(wrapper, 'b-2').text()).toContain('25/08/2026')
  })

  // The preview must not re-derive the rule: a second implementation is how a page and the
  // dashboard end up disagreeing, which is the defect this item removes.
  it('matches the dashboard computation for every start day', async () => {
    const days = Array.from({ length: 31 }, (_, index) => index + 1)
    const wrapper = await mountWith(days.map(day => ({
      bookletId: `b-${day}`,
      label: `Livret ${day}`,
      monthlyPeriodStartDay: day,
      monthlyPeriodEndDay: null,
    })))

    for (const day of days) {
      const expected = resolveMonthlyCycleRangeForTargetMonth(2026, 9, day)
      const text = preview(wrapper, `b-${day}`).text()
      expect(text, `start day ${day}`).toContain(formatted(expected.start))
      expect(text, `start day ${day}`).toContain(formatted(expected.end))
    }
  })

  it('no longer states that the start always falls in the previous month', async () => {
    const wrapper = await mountWith([
      { bookletId: 'b-1', label: 'Livret A', monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
    ])

    expect(wrapper.text()).not.toContain('mois précédent du mois affiché')
    expect(wrapper.text()).not.toContain('Démarre le mois précédent')
  })
})
