import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletFilterActionBar from '../../components/booklet/BookletFilterActionBar.vue'

const BookletActionButtonsStub = {
  name: 'BookletActionButtons',
  props: ['orientation', 'hasSelection', 'selectedCount', 'hasRegenerableTransactions', 'isAnyActionLoading', 'isDeleteLoading', 'isExportCsvLoading', 'isRegenerateLoading', 'selectedAmount', 'selectedAmountLabel'],
  emits: ['newTransaction', 'newPreview', 'importCsv', 'exportCsv', 'regenerate', 'delete'],
  template: '<div class="action-buttons-stub" />',
}

const SelectStub = {
  name: 'Select',
  props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'size'],
  emits: ['update:modelValue'],
  template: '<select />',
}

const ButtonStub = {
  name: 'Button',
  props: ['icon', 'loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" />',
}

const baseProps = {
  isMobile: false,
  globalFilter: 'none' as const,
  transactionsCount: 5,
  previewTransactionsCount: 2,
  isGlobalFilterLoading: false,
  hasSelection: false,
  selectedCount: 0,
  selectedAmount: 0,
  selectedAmountLabel: '0,00 €',
  hasRegenerableTransactions: false,
  isAnyActionLoading: false,
  isDeleteLoading: false,
  isExportCsvLoading: false,
  isRegenerateLoading: false,
}

const globalStubs = {
  BookletActionButtons: BookletActionButtonsStub,
  Select: SelectStub,
  Button: ButtonStub,
  Transition: false,
}

describe('components/booklet/BookletFilterActionBar', () => {
  describe('desktop — global filter buttons', () => {
    it('renders global filter buttons "Tout le mois" and "Prév. du mois"', () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: baseProps,
        global: { stubs: globalStubs },
      })
      const labels = wrapper.findAll('button').map(b => b.text())
      expect(labels.some(t => t.includes('Tout le mois'))).toBe(true)
      expect(labels.some(t => t.includes('Prév. du mois'))).toBe(true)
    })

    it('emits update:globalFilter all when "Tout le mois" is clicked while inactive', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, globalFilter: 'none' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.text().includes('Tout le mois'))
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['all'])
    })

    it('emits update:globalFilter none when "Tout le mois" is clicked while active (toggle off)', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, globalFilter: 'all' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.text().includes('Tout le mois'))
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['none'])
    })

    it('emits update:globalFilter preview when "Prév. du mois" is clicked while inactive', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, globalFilter: 'none' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.text().includes('Prév. du mois'))
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['preview'])
    })

    it('emits update:globalFilter none when "Prév. du mois" is clicked while active (toggle off)', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, globalFilter: 'preview' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.text().includes('Prév. du mois'))
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['none'])
    })

    it('disables global buttons when isGlobalFilterLoading is true', () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, isGlobalFilterLoading: true },
        global: { stubs: globalStubs },
      })
      const toutLeMois = wrapper.findAll('button').find(b => b.text().includes('Tout le mois'))
      expect(toutLeMois!.attributes('disabled')).toBeDefined()
    })
  })

  describe('mobile — global filter buttons', () => {
    it('renders compact global filter buttons on mobile', () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, isMobile: true },
        global: { stubs: globalStubs },
      })
      const buttons = wrapper.findAll('button')
      const ariaLabels = buttons.map(b => b.attributes('aria-label') ?? '')
      expect(ariaLabels).toContain('Tout le mois')
      expect(ariaLabels).toContain('Prévisionnelles du mois')
    })

    it('emits update:globalFilter all when mobile "Tout le mois" button is clicked', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, isMobile: true, globalFilter: 'none' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.attributes('aria-label') === 'Tout le mois')
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['all'])
    })

    it('emits update:globalFilter preview when mobile "Prévisionnelles du mois" button is clicked', async () => {
      const wrapper = mount(BookletFilterActionBar, {
        props: { ...baseProps, isMobile: true, globalFilter: 'none' },
        global: { stubs: globalStubs },
      })
      const btn = wrapper.findAll('button').find(b => b.attributes('aria-label') === 'Prévisionnelles du mois')
      await btn!.trigger('click')
      expect(wrapper.emitted('update:globalFilter')![0]).toEqual(['preview'])
    })
  })
})
