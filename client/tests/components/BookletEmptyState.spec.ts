import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BookletEmptyState from '../../components/booklet/BookletEmptyState.vue'

const ButtonStub = { props: ['label'], emits: ['click'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' }

function mountState(props: { filtered: boolean, search?: string, createLabel?: string }) {
  return mount(BookletEmptyState, { props, global: { stubs: { Button: ButtonStub } } })
}

describe('components/booklet/BookletEmptyState', () => {
  it('invites to create a first transaction when the period holds none', async () => {
    const wrapper = mountState({ filtered: false, createLabel: 'Créer une transaction' })

    expect(wrapper.text()).toContain('Aucune transaction')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('create')).toHaveLength(1)
  })

  // Inviting to create a transaction would suggest the period is empty, when filters hide its rows.
  it('says nothing matches, and offers to clear the filters, when filters hide every row', async () => {
    const wrapper = mountState({ filtered: true, search: 'loyer' })

    expect(wrapper.text()).toContain('Aucune transaction ne correspond')
    expect(wrapper.text()).toContain('« loyer »')
    expect(wrapper.text()).not.toContain('première transaction')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('clearFilters')).toHaveLength(1)
  })
})
