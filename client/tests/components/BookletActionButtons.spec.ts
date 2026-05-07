import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletActionButtons from '../../components/booklet/BookletActionButtons.vue'

const ButtonStub = {
  name: 'Button',
  props: ['icon', 'loading', 'disabled', 'outlined'],
  template: '<button :data-icon="icon" :data-loading="loading" :disabled="disabled" />',
}

const baseProps = {
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

describe('components/booklet/BookletActionButtons', () => {
  describe('vertical mode — selection chip', () => {
    it('shows selectedAmountLabel in vertical mode when transactions are selected', () => {
      const wrapper = mount(BookletActionButtons, {
        props: {
          ...baseProps,
          orientation: 'vertical',
          hasSelection: true,
          selectedCount: 2,
          selectedAmount: -850.50,
          selectedAmountLabel: '-850,50 €',
        },
        global: { stubs: { Button: ButtonStub } },
      })

      expect(wrapper.text()).toContain('-850,50 €')
    })

    it('shows selectedAmountLabel with positive colour class for income selection', () => {
      const wrapper = mount(BookletActionButtons, {
        props: {
          ...baseProps,
          orientation: 'vertical',
          hasSelection: true,
          selectedCount: 1,
          selectedAmount: 2000,
          selectedAmountLabel: '+2 000,00 €',
        },
        global: { stubs: { Button: ButtonStub } },
      })

      const amountSpan = wrapper.find('.text-emerald-600')
      expect(amountSpan.exists()).toBe(true)
      expect(amountSpan.text()).toBe('+2 000,00 €')
    })

    it('shows selectedAmountLabel with negative colour class for expense selection', () => {
      const wrapper = mount(BookletActionButtons, {
        props: {
          ...baseProps,
          orientation: 'vertical',
          hasSelection: true,
          selectedCount: 1,
          selectedAmount: -50,
          selectedAmountLabel: '-50,00 €',
        },
        global: { stubs: { Button: ButtonStub } },
      })

      const amountSpan = wrapper.find('.text-red-500')
      expect(amountSpan.exists()).toBe(true)
      expect(amountSpan.text()).toBe('-50,00 €')
    })

    it('does not show selection chip when hasSelection is false', () => {
      const wrapper = mount(BookletActionButtons, {
        props: {
          ...baseProps,
          orientation: 'vertical',
          hasSelection: false,
        },
        global: { stubs: { Button: ButtonStub } },
      })

      expect(wrapper.find('.pi-check-square').exists()).toBe(false)
    })
  })

  describe('horizontal mode — selection badge', () => {
    it('shows selectedAmountLabel in horizontal mode when transactions are selected', () => {
      const wrapper = mount(BookletActionButtons, {
        props: {
          ...baseProps,
          orientation: 'horizontal',
          hasSelection: true,
          selectedCount: 3,
          selectedAmount: -200,
          selectedAmountLabel: '-200,00 €',
        },
        global: { stubs: { Button: ButtonStub } },
      })

      expect(wrapper.text()).toContain('-200,00 €')
    })
  })
})
