import type { RegenerableTransactionDTO } from '../../composables/useBooklet'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletRegenerateTransactionsDialog from '../../components/booklet/BookletRegenerateTransactionsDialog.vue'

const DialogStub = {
  name: 'Dialog',
  props: ['visible', 'header'],
  template: '<div v-if="visible" class="dialog"><h2>{{ header }}</h2><slot /></div>',
}

const CheckboxStub = {
  name: 'Checkbox',
  props: ['modelValue', 'binary', 'inputId'],
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', !modelValue)">',
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'disabled', 'loading', 'severity'],
  template: '<button :disabled="disabled">{{ label }}</button>',
}

function aCandidate(overrides: Partial<RegenerableTransactionDTO> = {}): RegenerableTransactionDTO {
  return {
    regularTransactionId: 'rt-1',
    label: 'Loyer',
    value: '800.00',
    currency: '€',
    isIncome: false,
    date: '2026-08-05',
    tagDTO: null,
    ...overrides,
  }
}

function mountDialog(candidates: RegenerableTransactionDTO[], props: Record<string, unknown> = {}) {
  return mount(BookletRegenerateTransactionsDialog, {
    props: { visible: true, candidates, ...props },
    global: { stubs: { Dialog: DialogStub, Checkbox: CheckboxStub, Button: ButtonStub } },
  })
}

function checkboxes(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.findAll('input[type="checkbox"]')
}

function confirmButton(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.findAll('button').find(b => b.text() === 'Restaurer')!
}

describe('components/BookletRegenerateTransactionsDialog', () => {
  describe('when candidates are available', () => {
    it('lists each deleted occurrence with its label, date and amount', () => {
      const wrapper = mountDialog([
        aCandidate(),
        aCandidate({ regularTransactionId: 'rt-2', label: 'Salaire', value: '2500.00', isIncome: true, date: '2026-08-28' }),
      ])

      expect(wrapper.findAll('li')).toHaveLength(2)
      expect(wrapper.text()).toContain('Loyer')
      expect(wrapper.text()).toContain('05/08/2026')
      expect(wrapper.text()).toContain('- 800.00 €')
      expect(wrapper.text()).toContain('Salaire')
      expect(wrapper.text()).toContain('+ 2500.00 €')
    })

    it('starts with every row unchecked', () => {
      const wrapper = mountDialog([aCandidate()])

      expect(checkboxes(wrapper).every(c => (c.element as HTMLInputElement).checked)).toBe(false)
    })
  })

  describe('select all', () => {
    it('checks every candidate then clears them', async () => {
      const wrapper = mountDialog([
        aCandidate(),
        aCandidate({ regularTransactionId: 'rt-2', label: 'Salaire' }),
      ])
      const selectAll = checkboxes(wrapper)[0]

      await selectAll.trigger('change')
      expect(checkboxes(wrapper).every(c => (c.element as HTMLInputElement).checked)).toBe(true)

      await checkboxes(wrapper)[0].trigger('change')
      expect(checkboxes(wrapper).slice(1).every(c => (c.element as HTMLInputElement).checked)).toBe(false)
    })
  })

  describe('confirmation', () => {
    it('is disabled while nothing is selected', () => {
      const wrapper = mountDialog([aCandidate()])

      expect(confirmButton(wrapper).attributes('disabled')).toBeDefined()
    })

    it('emits only the selected identifiers', async () => {
      const wrapper = mountDialog([
        aCandidate(),
        aCandidate({ regularTransactionId: 'rt-2', label: 'Salaire' }),
      ])

      // index 0 is the "select all" row, so index 1 is the first candidate
      await checkboxes(wrapper)[1].trigger('change')
      await confirmButton(wrapper).trigger('click')

      expect(wrapper.emitted('confirm')).toEqual([[['rt-1']]])
    })

    it('closes on cancel without emitting a confirmation', async () => {
      const wrapper = mountDialog([aCandidate()])

      await wrapper.findAll('button').find(b => b.text() === 'Annuler')!.trigger('click')

      expect(wrapper.emitted('update:visible')).toEqual([[false]])
      expect(wrapper.emitted('confirm')).toBeUndefined()
    })
  })

  describe('when a recurrence produces several occurrences in the month', () => {
    it('toggles the whole group together and warns about it', async () => {
      const wrapper = mountDialog([
        aCandidate({ label: 'Courses', date: '2026-08-03' }),
        aCandidate({ label: 'Courses', date: '2026-08-10' }),
      ])

      expect(wrapper.text()).toContain('restaure les 2 occurrences du mois')

      await checkboxes(wrapper)[1].trigger('change')

      const rowCheckboxes = checkboxes(wrapper).slice(1)
      expect(rowCheckboxes.every(c => (c.element as HTMLInputElement).checked)).toBe(true)

      await confirmButton(wrapper).trigger('click')
      expect(wrapper.emitted('confirm')).toEqual([[['rt-1']]])
    })
  })

  describe('empty and loading states', () => {
    it('shows a meaningful message when there is nothing to restore', () => {
      const wrapper = mountDialog([])

      expect(wrapper.text()).toContain('Aucune transaction supprimée à restaurer')
      expect(wrapper.findAll('li')).toHaveLength(0)
    })

    it('shows a loading message while candidates are being fetched', () => {
      const wrapper = mountDialog([], { loadingCandidates: true })

      expect(wrapper.text()).toContain('Chargement des transactions supprimées')
    })
  })

  describe('when a confirmation fails and the dialog stays open', () => {
    it('keeps the selection intact', async () => {
      const wrapper = mountDialog([aCandidate()])

      await checkboxes(wrapper)[1].trigger('change')
      await wrapper.setProps({ loading: true })
      await wrapper.setProps({ loading: false })

      expect((checkboxes(wrapper)[1].element as HTMLInputElement).checked).toBe(true)
      expect(confirmButton(wrapper).attributes('disabled')).toBeUndefined()
    })
  })
})
