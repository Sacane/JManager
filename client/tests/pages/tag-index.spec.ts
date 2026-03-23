import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import TagPage from '../../pages/tag/index.vue'

const addPersonalTagMock = vi.fn()
const getAllTagsMock = vi.fn().mockResolvedValue([])
const deleteTagMock = vi.fn()
const editTagMock = vi.fn()

vi.mock('~/composables/useTag', () => ({
  default: () => ({
    addPersonalTag: addPersonalTagMock,
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
