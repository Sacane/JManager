import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PageSkeleton from '../../components/PageSkeleton.vue'

describe('components/PageSkeleton', () => {
  // A skeleton is shapes without words. Without a status role and a text alternative, a screen
  // reader user is told nothing at all while the page loads.
  it('announces the loading state to assistive technology', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'list', label: 'Chargement des tags…' } })

    const root = wrapper.find('[data-test="page-skeleton"]')
    expect(root.attributes('role')).toBe('status')
    expect(root.attributes('aria-busy')).toBe('true')
    expect(root.text()).toContain('Chargement des tags…')
  })

  it('hides the placeholder shapes from assistive technology', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'list', label: 'Chargement…' } })

    const shapes = wrapper.find('[data-test="skeleton-shapes"]')
    expect(shapes.attributes('aria-hidden')).toBe('true')
  })

  it('renders the requested number of list rows', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'list', label: 'Chargement…', count: 7 } })

    expect(wrapper.findAll('[data-test="skeleton-row"]')).toHaveLength(7)
  })

  it('renders the requested number of cards', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'cards', label: 'Chargement…', count: 3 } })

    expect(wrapper.findAll('[data-test="skeleton-card"]')).toHaveLength(3)
  })

  // The dashboard skeleton mirrors the real layout so the content settles in place instead of
  // appearing as fifteen blocks at once.
  it('mirrors the dashboard layout: indicators then charts', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'dashboard', label: 'Chargement…' } })

    expect(wrapper.findAll('[data-test="skeleton-kpi"]').length).toBeGreaterThan(0)
    expect(wrapper.findAll('[data-test="skeleton-chart"]').length).toBeGreaterThan(0)
  })

  it('renders labelled field placeholders for a form', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'form', label: 'Chargement…', count: 4 } })

    expect(wrapper.findAll('[data-test="skeleton-field"]')).toHaveLength(4)
  })

  it('falls back to a sensible number of items', () => {
    const wrapper = mount(PageSkeleton, { props: { variant: 'list', label: 'Chargement…' } })

    expect(wrapper.findAll('[data-test="skeleton-row"]').length).toBeGreaterThan(0)
  })
})
