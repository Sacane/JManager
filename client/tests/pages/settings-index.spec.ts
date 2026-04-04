import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import SettingsPage from '../../pages/settings/index.vue'

const getSettingsMock = vi.fn().mockResolvedValue({
  projectionWindowDays: 21,
  bookletCycles: [
    {
      accountId: 'acc-1',
      label: 'Compte principal',
      monthlyPeriodStartDay: 28,
      monthlyPeriodEndDay: null,
    },
  ],
})

const updateSettingsMock = vi.fn().mockResolvedValue({
  projectionWindowDays: 30,
  bookletCycles: [
    {
      accountId: 'acc-1',
      label: 'Compte principal',
      monthlyPeriodStartDay: 27,
      monthlyPeriodEndDay: 26,
    },
  ],
})

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({
    getSettings: getSettingsMock,
    updateSettings: updateSettingsMock,
  }),
}))

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

function mountSettingsPage(activeScopes: string[] = []) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))

  return shallowMount(SettingsPage)
}

describe('pages/settings/index', () => {
  it('renders settings sections and loads API data', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    expect(getSettingsMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Parametres utilisateur')
    expect(wrapper.text()).toContain('Projection')
    expect(wrapper.text()).toContain('Cycle mensuel par compte')
    expect(wrapper.text()).toContain('Compte principal')
  })

  it('submits updated settings payload', async () => {
    const wrapper = mountSettingsPage()

    await flushPromises()
    await flushPromises()

    const projectionInput = wrapper.find('[data-test="projection-window-input"]')
    await projectionInput.setValue('30')

    const cycleSelect = wrapper.find('[data-test="cycle-select-acc-1"]')
    await cycleSelect.setValue('27')

    const cycleEndSelect = wrapper.find('[data-test="cycle-end-select-acc-1"]')
    await cycleEndSelect.setValue('26')

    await wrapper.find('[data-test="save-settings-btn"]').trigger('click')

    expect(updateSettingsMock).toHaveBeenCalledWith({
      projectionWindowDays: 30,
      bookletCycles: [
        {
          accountId: 'acc-1',
          monthlyPeriodStartDay: 27,
          monthlyPeriodEndDay: 26,
        },
      ],
    })
  })
})
