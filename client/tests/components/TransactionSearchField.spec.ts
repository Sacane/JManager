import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TransactionSearchField from '../../components/booklet/TransactionSearchField.vue'

describe('components/booklet/TransactionSearchField', () => {
  it('reports what is typed', async () => {
    const wrapper = mount(TransactionSearchField, { props: { modelValue: '' } })

    await wrapper.find('input').setValue('loyer')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['loyer'])
  })

  it('is labelled and bounded like a transaction label', () => {
    const input = mount(TransactionSearchField, { props: { modelValue: '' } }).find('input')

    expect(input.attributes('aria-label')).toBe('Rechercher une transaction par son libellé')
    expect(input.attributes('maxlength')).toBe('100')
    expect(input.attributes('type')).toBe('search')
  })

  it('offers to clear the search only once something is typed', async () => {
    const wrapper = mount(TransactionSearchField, { props: { modelValue: '' } })
    expect(wrapper.find('[data-test="clear-search"]').exists()).toBe(false)

    await wrapper.setProps({ modelValue: 'loyer' })
    await wrapper.find('[data-test="clear-search"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([''])
  })
})
