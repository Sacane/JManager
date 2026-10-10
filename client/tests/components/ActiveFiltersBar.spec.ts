import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ActiveFiltersBar from '../../components/booklet/ActiveFiltersBar.vue'

const FILTERS = [
  { key: 'tag', label: 'Tag : Logement' },
  { key: 'subTag', label: 'Sous-tag : Loyer' },
]

describe('components/booklet/ActiveFiltersBar', () => {
  it('shows nothing while no filter is active', () => {
    expect(mount(ActiveFiltersBar, { props: { filters: [] } }).find('[data-test="active-filters"]').exists()).toBe(false)
  })

  it('shows every filter it is given', () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: FILTERS } })

    expect(wrapper.findAll('[data-test="active-filter"]').map(chip => chip.text())).toEqual(['Tag : Logement', 'Sous-tag : Loyer'])
  })

  it('removes one filter from its chip', async () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: FILTERS } })

    await wrapper.findAll('[data-test="remove-filter"]')[1].trigger('click')

    expect(wrapper.emitted('remove')).toEqual([['subTag']])
    expect(wrapper.findAll('[data-test="remove-filter"]')[1].attributes('aria-label')).toBe('Retirer le filtre Sous-tag : Loyer')
  })

  // A single filter already has its own cross: a second way to remove it is noise.
  it('offers to clear everything only when asked to', async () => {
    const single = mount(ActiveFiltersBar, { props: { filters: [FILTERS[0]] } })
    expect(single.find('[data-test="clear-filters"]').exists()).toBe(false)

    const several = mount(ActiveFiltersBar, { props: { filters: [FILTERS[0]], showClearAll: true } })
    await several.find('[data-test="clear-filters"]').trigger('click')
    expect(several.emitted('clearAll')).toHaveLength(1)
  })

  // The search field shows the search: with only it and another filter, the bar still offers to clear both.
  it('shows the clear action alone when no chip is left to show', () => {
    const wrapper = mount(ActiveFiltersBar, { props: { filters: [], showClearAll: true } })

    expect(wrapper.find('[data-test="clear-filters"]').exists()).toBe(true)
  })
})
