import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TagEditDialog from '../../components/dialog/TagEditDialog.vue'

const ButtonStub = {
  props: ['label', 'icon', 'loading', 'disabled', 'severity', 'size', 'outlined'],
  emits: ['click'],
  template: '<button :data-label="label" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}

const ColorPickerFieldStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input data-test="color-picker" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

function mountDialog(props: Record<string, any>, modelValue = true) {
  vi.stubGlobal('definePageMeta', vi.fn())
  return mount(TagEditDialog, {
    props: {
      'visible': modelValue,
      'onUpdate:visible': (val: boolean) => wrapper.setProps({ visible: val }),
      'loading': false,
      'disabled': false,
      ...props,
    },
    global: {
      stubs: {
        Dialog: { template: '<div><slot /></div>' },
        InputText: { template: '<input data-test="label-input" />' },
        ColorPickerField: ColorPickerFieldStub,
        Button: ButtonStub,
      },
    },
  })
}

// Needed for setProps inside the factory
let wrapper: any

describe('components/dialog/TagEditDialog', () => {
  describe('when editing a top-level tag (no parentTag)', () => {
    it('does not show the parent tag section', () => {
      wrapper = mountDialog({
        tag: { id: '1', label: 'Food', colorHex: '#ff0000' },
        parentTag: null,
      })
      expect(wrapper.find('.parent-tag-section').exists()).toBe(false)
    })

    it('emits submit with parentId null when saved', async () => {
      wrapper = mountDialog({
        tag: { id: '1', label: 'Food', colorHex: '#ff0000' },
        parentTag: null,
      })
      const saveBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label')?.includes('Enregistrer'))
      await saveBtn?.trigger('click')
      const emitted = wrapper.emitted('submit')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0].parentId).toBeNull()
    })
  })

  describe('when editing a sub-tag (parentTag provided)', () => {
    it('shows the parent tag section with the parent label', () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      expect(wrapper.find('.parent-tag-section').exists()).toBe(true)
      expect(wrapper.text()).toContain('Food')
    })

    it('shows a "Détacher" button when detach has not been triggered', () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      const detachBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      expect(detachBtn).toBeDefined()
    })

    it('emits submit with the original parentId when saved without detaching', async () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      const saveBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label')?.includes('Enregistrer'))
      await saveBtn?.trigger('click')
      const emitted = wrapper.emitted('submit')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0].parentId).toBe('1')
    })

    it('shows detach warning and changes button to "Annuler" after clicking Détacher', async () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      const detachBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      await detachBtn?.trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.detach-warning').exists()).toBe(true)
      const undoBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Annuler')
      expect(undoBtn).toBeDefined()
    })

    it('emits submit with parentId null after clicking Détacher and saving', async () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      const detachBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      await detachBtn?.trigger('click')
      await wrapper.vm.$nextTick()

      const saveBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label')?.includes('Enregistrer'))
      await saveBtn?.trigger('click')

      const emitted = wrapper.emitted('submit')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0].parentId).toBeNull()
    })

    it('resets detach state to false when dialog is reopened', async () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      // trigger detach
      const detachBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      await detachBtn?.trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.find('.detach-warning').exists()).toBe(true)

      // close and reopen
      await wrapper.setProps({ visible: false })
      await wrapper.vm.$nextTick()
      await wrapper.setProps({ visible: true })
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.detach-warning').exists()).toBe(false)
    })

    it('cancelling detach restores the Détacher button and hides the warning', async () => {
      wrapper = mountDialog({
        tag: { id: '3', label: 'Restaurants', colorHex: '#ff0000' },
        parentTag: { id: '1', label: 'Food' },
      })
      const detachBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      await detachBtn?.trigger('click')
      await wrapper.vm.$nextTick()

      const undoBtn = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Annuler')
      await undoBtn?.trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.detach-warning').exists()).toBe(false)
      const detachAgain = wrapper.findAll('button').find((b: any) => b.attributes('data-label') === 'Détacher')
      expect(detachAgain).toBeDefined()
    })
  })
})
