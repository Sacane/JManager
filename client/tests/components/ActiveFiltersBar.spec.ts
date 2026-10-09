import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ActiveFiltersBar from '../../components/booklet/ActiveFiltersBar.vue'

const FILTERS = [
  { key: 'search', label: 'Recherche : « loyer »' },
  { key: 'tag', label: 'Tag : Logement' },
]

describe('components/booklet/ActiveFiltersBar', () => {
  it('shows nothing while no filter is active', () => {
    expect(mount(ActiveFiltersBar, { props: { filters: [] } }).find('[data-test="active-filters"]').exists()).toBe(false)
  })

  it('shows every active filter', () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: FILTERS } })

    expect(wrapper.findAll('[data-test="active-filter"]').map(chip => chip.text())).toEqual(['Recherche : « loyer »', 'Tag : Logement'])
  })

  it('removes one filter from its chip', async () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: FILTERS } })

    await wrapper.findAll('[data-test="remove-filter"]')[1].trigger('click')

    expect(wrapper.emitted('remove')).toEqual([['tag']])
    expect(wrapper.findAll('[data-test="remove-filter"]')[1].attributes('aria-label')).toBe('Retirer le filtre Tag : Logement')
  })

  it('clears every filter in one action', async () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: FILTERS } })

    await wrapper.find('[data-test="clear-filters"]').trigger('click')

    expect(wrapper.emitted('clearAll')).toHaveLength(1)
  })
})
