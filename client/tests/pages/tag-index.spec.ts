import { flushPromises, shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TagPage from '../../pages/tag/index.vue'

const addPersonalTagMock = vi.fn()
const addSubTagMock = vi.fn()
const getAllTagsMock = vi.fn().mockResolvedValue([])
const deleteTagMock = vi.fn()
const editTagMock = vi.fn()

vi.mock('~/composables/useTag', () => ({
  default: () => ({
    addPersonalTag: addPersonalTagMock,
    addSubTag: addSubTagMock,
    getAllTags: getAllTagsMock,
    deleteTag: deleteTagMock,
    editTag: editTagMock,
  }),
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() }),
}))

function mountTagPage(activeScopes: string[] = []) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))

  const wrapper = shallowMount(TagPage, {
    global: {
      stubs: {
        ConfirmDialog: true,
        ProgressSpinner: { template: '<div class="spinner" />' },
        InputText: { template: '<input />' },
        SelectButton: { template: '<div />' },
        Select: { template: '<div />' },
        Dialog: { template: '<div><slot /></div>' },
        Tag: { template: '<span><slot /></span>' },
        Button: {
          props: ['label', 'loading', 'disabled'],
          template: '<button :data-label="label" :data-loading="String(loading)" :disabled="disabled"><slot /></button>',
        },
      },
    },
  })

  return { wrapper }
}

describe('pages/tag/index loading states', () => {
  it('shows list loading feedback when tag load scope is active', () => {
    const { wrapper } = mountTagPage(['tag.load'])

    expect(wrapper.text()).toContain('Chargement des tags...')
  })

  it('disables create action when an add action is running', () => {
    const { wrapper } = mountTagPage(['tag.add'])

    const createButton = wrapper.findAll('button').find(btn => btn.attributes('data-label') === 'Nouveau tag')
    expect(createButton).toBeDefined()
    expect(createButton?.attributes('disabled')).toBeDefined()
  })
})

const personalTagDTO = {
  tagId: '1',
  label: 'Food',
  colorDTO: { red: 255, green: 100, blue: 50 },
  isDefault: false,
  parentId: null,
}

const defaultTagDTO = {
  tagId: '2',
  label: 'None',
  colorDTO: { red: 100, green: 100, blue: 100 },
  isDefault: true,
  parentId: null,
}

const subTagDTO = {
  tagId: '3',
  label: 'FastFood',
  colorDTO: { red: 200, green: 50, blue: 50 },
  isDefault: false,
  parentId: '1',
}

describe('pages/tag/index multi-selection', () => {
  it('shows select-all checkbox when non-default tags are loaded', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    expect(wrapper.html()).toContain('Tout sélectionner')
  })

  it('does not show selection controls when only default tags exist', async () => {
    getAllTagsMock.mockResolvedValue([defaultTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    expect(wrapper.html()).not.toContain('Tout sélectionner')
  })

  it('hides selection count when nothing is selected', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    expect(wrapper.text()).not.toContain('sélectionné')
  })

  it('shows selection count after toggling select-all', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO, { ...personalTagDTO, tagId: '4', label: 'Travel' }])
    const { wrapper } = mountTagPage()
    await flushPromises()

    await (wrapper.vm as any).toggleSelectAll()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('2 sélectionné(s)')
  })

  it('shows bulk delete button when at least one item is selected', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    const tag: TagDisplayItem = { id: '1', label: 'Food', isDefault: false, color: 'rgb(255,100,50)', parentId: null }
    await (wrapper.vm as any).toggleSelectTag(tag)
    await wrapper.vm.$nextTick()

    const bulkDeleteBtn = wrapper.findAll('button').find(btn =>
      btn.attributes('aria-label')?.includes('Supprimer') && btn.attributes('aria-label')?.includes('tag'),
    )
    expect(bulkDeleteBtn).toBeDefined()
  })

  it('deselects all items when toggling select-all while all are selected', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    await (wrapper.vm as any).toggleSelectAll()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('1 sélectionné(s)')

    await (wrapper.vm as any).toggleSelectAll()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).not.toContain('sélectionné')
  })

  it('removes tag from selection when deleted', async () => {
    deleteTagMock.mockResolvedValue(undefined)
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    const tag: TagDisplayItem = { id: '1', label: 'Food', isDefault: false, color: 'rgb(255,100,50)', parentId: null }
    await (wrapper.vm as any).toggleSelectTag(tag)
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('1 sélectionné(s)')

    await (wrapper.vm as any).performDeleteTag(tag)
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).not.toContain('sélectionné')
  })

  it('does not show selection controls when no tags are loaded', async () => {
    getAllTagsMock.mockResolvedValue([])
    const { wrapper } = mountTagPage()
    await flushPromises()

    expect(wrapper.html()).not.toContain('Tout sélectionner')
  })
})

describe('pages/tag/index sub-tag promotion', () => {
  it('onEditClick sets parentId on tagToEdit for a sub-tag', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO, subTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    const subTag: TagDisplayItem = {
      id: '3',
      label: 'FastFood',
      isDefault: false,
      color: 'rgb(200,50,50)',
      parentId: '1',
    }
    await (wrapper.vm as any).onEditClick(subTag)
    await wrapper.vm.$nextTick()

    const tagToEdit = (wrapper.vm as any).tagToEdit
    expect(tagToEdit).not.toBeNull()
    expect(tagToEdit.parentId).toBe('1')
  })

  it('onEditClick sets parentId to null for a top-level tag', async () => {
    getAllTagsMock.mockResolvedValue([personalTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    const topTag: TagDisplayItem = {
      id: '1',
      label: 'Food',
      isDefault: false,
      color: 'rgb(255,100,50)',
      parentId: null,
    }
    await (wrapper.vm as any).onEditClick(topTag)
    await wrapper.vm.$nextTick()

    const tagToEdit = (wrapper.vm as any).tagToEdit
    expect(tagToEdit).not.toBeNull()
    expect(tagToEdit.parentId).toBeNull()
  })

  it('applyEdit calls editTag with parentId null when detaching', async () => {
    const updatedTag = { tagId: '3', label: 'FastFood', colorDTO: { red: 200, green: 50, blue: 50 }, isDefault: false, parentId: null }
    editTagMock.mockResolvedValue(updatedTag)
    getAllTagsMock.mockResolvedValue([personalTagDTO, subTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    await (wrapper.vm as any).applyEdit({ id: '3', label: 'FastFood', colorHex: '#c83232', parentId: null })
    await flushPromises()

    expect(editTagMock).toHaveBeenCalledWith(
      expect.objectContaining({ tagId: '3', parentId: undefined }),
    )
  })

  it('applyEdit updates tags list in-memory after successful promotion', async () => {
    const updatedTag = { tagId: '3', label: 'FastFood', colorDTO: { red: 200, green: 50, blue: 50 }, isDefault: false, parentId: null }
    editTagMock.mockResolvedValue(updatedTag)
    getAllTagsMock.mockResolvedValue([personalTagDTO, subTagDTO])
    const { wrapper } = mountTagPage()
    await flushPromises()

    await (wrapper.vm as any).applyEdit({ id: '3', label: 'FastFood', colorHex: '#c83232', parentId: null })
    await flushPromises()

    const tags = (wrapper.vm as any).tags as TagDisplayItem[]
    const promoted = tags.find(t => t.id === '3')
    expect(promoted?.parentId).toBeNull()
  })
})
