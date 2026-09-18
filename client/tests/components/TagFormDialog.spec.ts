import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import TagFormDialog from '../../components/dialog/TagFormDialog.vue'

const PARENTS = [{ id: 'p-1', label: 'Maison', color: '#123456' }]

const InputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input class="tag-label" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
}

/** Toggles to "sous-tag" on click, standing in for the SelectButton. */
const SelectButtonStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<button type="button" class="make-subtag" @click="$emit('update:modelValue', true)" />`,
}

/** Picks the first parent on click, standing in for the Select. */
const SelectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: `<button type="button" class="pick-parent" @click="$emit('update:modelValue', options[0].id)" />`,
}

function mountDialog(props: { disabled?: boolean } = {}) {
  return mount(TagFormDialog, {
    props: {
      visible: true,
      parentTagOptions: PARENTS,
      loading: false,
      disabled: props.disabled ?? false,
    },
    global: {
      stubs: {
        Dialog: { template: '<div><slot /></div>' },
        InputText: InputTextStub,
        SelectButton: SelectButtonStub,
        Select: SelectStub,
        ColorPickerField: true,
        Button: {
          props: ['label', 'disabled'],
          emits: ['click'],
          template: `<button class="submit" :disabled="disabled" @click="!disabled && $emit('click')">{{ label }}</button>`,
        },
      },
    },
  })
}

async function submit(wrapper: ReturnType<typeof mountDialog>) {
  await wrapper.find('button.submit').trigger('click')
  await nextTick()
}

describe('components/dialog/TagFormDialog', () => {
  beforeEach(() => vi.clearAllMocks())

  // The button used to be disabled on an empty label without saying why; the red asterisk alone was
  // the only hint.
  it('keeps the create action usable and names the missing label', async () => {
    const wrapper = mountDialog()

    expect(wrapper.find('button.submit').attributes('disabled')).toBeUndefined()

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-tag-label"]').text()).toMatch(/libellé/i)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('names the missing parent of a sub-tag', async () => {
    const wrapper = mountDialog()
    await wrapper.find('.make-subtag').trigger('click')
    await wrapper.find('input.tag-label').setValue('Électricité')

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-tag-parent"]').text()).toMatch(/parent/i)
    expect(wrapper.find('[data-test="error-tag-label"]').exists()).toBe(false)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('asks for no parent on a top-level tag', async () => {
    const wrapper = mountDialog()
    await wrapper.find('input.tag-label').setValue('Courses')

    await submit(wrapper)

    expect(wrapper.find('[data-test="error-tag-parent"]').exists()).toBe(false)
    expect(wrapper.emitted('submit')).toHaveLength(1)
  })

  it('clears an error as soon as it is corrected', async () => {
    const wrapper = mountDialog()
    await submit(wrapper)
    // Without this, the test would pass against a dialog that never shows the error at all.
    expect(wrapper.find('[data-test="error-tag-label"]').exists()).toBe(true)

    await wrapper.find('input.tag-label').setValue('Courses')
    await nextTick()

    expect(wrapper.find('[data-test="error-tag-label"]').exists()).toBe(false)
  })

  it('submits a valid sub-tag', async () => {
    const wrapper = mountDialog()
    await wrapper.find('.make-subtag').trigger('click')
    await wrapper.find('.pick-parent').trigger('click')
    await wrapper.find('input.tag-label').setValue('Électricité')

    await submit(wrapper)

    expect(wrapper.emitted('submit')).toHaveLength(1)
    expect(wrapper.emitted('submit')![0][0]).toMatchObject({ tagLabel: 'Électricité', isSubTag: true, parentId: 'p-1' })
  })

  // An empty label is something the user can fix in the form. The `disabled` prop is a reason the
  // form cannot know about, so it still blocks the action outright.
  it('still honours the disabled prop set by the page', () => {
    const wrapper = mountDialog({ disabled: true })

    expect(wrapper.find('button.submit').attributes('disabled')).toBeDefined()
  })

  it('forgets its errors when the dialog is closed and reopened', async () => {
    const wrapper = mountDialog()
    await submit(wrapper)
    expect(wrapper.find('[data-test="error-tag-label"]').exists()).toBe(true)

    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })

    expect(wrapper.find('[data-test="error-tag-label"]').exists()).toBe(false)
  })
})
