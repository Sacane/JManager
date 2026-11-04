import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import useBookletOrder, { LEGACY_BOOKLET_ORDER_KEY } from '../../composables/useBookletOrder'

interface Item { id: string, displayPosition?: number | null }

const reorderBooklets = vi.fn()
const errorAxios = vi.fn()

function mockDragEvent(overrides: Record<string, any> = {}): DragEvent {
  return { preventDefault: vi.fn(), dataTransfer: { effectAllowed: '', dropEffect: '' }, ...overrides } as any
}

function threeBooklets(displayPosition: number | null = null) {
  return ref<Item[]>([{ id: '1', displayPosition }, { id: '2', displayPosition }, { id: '3', displayPosition }])
}

const ids = (items: Item[]) => items.map(item => item.id)

describe('composables/useBookletOrder', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    reorderBooklets.mockResolvedValue(undefined)
    vi.stubGlobal('useBooklet', () => ({ reorderBooklets }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), warn: vi.fn(), errorAxios }))
    window.localStorage.clear()
  })

  // The server lists the booklets in the order the user chose.
  it('shows the booklets in the order they come in', () => {
    const { orderedItems } = useBookletOrder(threeBooklets())

    expect(ids(orderedItems.value)).toEqual(['1', '2', '3'])
  })

  it('follows the booklets once they are loaded again', async () => {
    const items = threeBooklets()
    const { orderedItems } = useBookletOrder(items)

    items.value = [{ id: '3' }, { id: '1' }]
    await nextTick()

    expect(ids(orderedItems.value)).toEqual(['3', '1'])
  })

  describe('moving one step', () => {
    it('moves a booklet after the next one and keeps the order with the account', async () => {
      const { orderedItems, moveBooklet } = useBookletOrder(threeBooklets())

      await moveBooklet(0, 1)

      expect(ids(orderedItems.value)).toEqual(['2', '1', '3'])
      expect(reorderBooklets).toHaveBeenCalledWith(['2', '1', '3'])
    })

    it('moves a booklet before the previous one', async () => {
      const { orderedItems, moveBooklet } = useBookletOrder(threeBooklets())

      await moveBooklet(2, -1)

      expect(ids(orderedItems.value)).toEqual(['1', '3', '2'])
    })

    it('does not move the first booklet before itself, nor the last one after', async () => {
      const { orderedItems, moveBooklet } = useBookletOrder(threeBooklets())

      await moveBooklet(0, -1)
      await moveBooklet(2, 1)

      expect(ids(orderedItems.value)).toEqual(['1', '2', '3'])
      expect(reorderBooklets).not.toHaveBeenCalled()
    })

    it('shows the move at once, before the account answers', async () => {
      let answer: () => void = () => {}
      reorderBooklets.mockReturnValue(new Promise<void>((resolve) => {
        answer = resolve
      }))
      const { orderedItems, moveBooklet } = useBookletOrder(threeBooklets())

      const saving = moveBooklet(0, 1)

      expect(ids(orderedItems.value)).toEqual(['2', '1', '3'])
      answer()
      await saving
    })

    it('goes back to the last saved order, and says so, when the account cannot take it', async () => {
      const { orderedItems, moveBooklet } = useBookletOrder(threeBooklets())
      await moveBooklet(0, 1)
      reorderBooklets.mockRejectedValueOnce(new Error('down'))

      await moveBooklet(2, -1)

      expect(ids(orderedItems.value)).toEqual(['2', '1', '3'])
      expect(errorAxios).toHaveBeenCalledWith(expect.any(Error), 'L\'ordre des livrets n\'a pas pu être enregistré')
    })

    // Two quick moves: the account must end with the last one, whatever answers first.
    it('saves one order at a time, in the order of the moves', async () => {
      const answers: Array<() => void> = []
      reorderBooklets.mockImplementation(() => new Promise<void>((resolve) => {
        answers.push(resolve)
      }))
      const { moveBooklet } = useBookletOrder(threeBooklets())

      const first = moveBooklet(0, 1)
      const second = moveBooklet(1, 1)
      await flushPromises()

      expect(reorderBooklets).toHaveBeenCalledTimes(1)
      answers[0]!()
      await first
      await flushPromises()
      expect(reorderBooklets).toHaveBeenLastCalledWith(['2', '3', '1'])
      answers[1]!()
      await second
    })
  })

  describe('drag and drop', () => {
    it('onDragStart sets draggedIndex', () => {
      const { draggedIndex, onDragStart } = useBookletOrder(threeBooklets())

      onDragStart(mockDragEvent(), 0)

      expect(draggedIndex.value).toBe(0)
    })

    it('onDragOver updates dragOverIndex', () => {
      const { dragOverIndex, onDragOver } = useBookletOrder(threeBooklets())

      onDragOver(mockDragEvent(), 1)

      expect(dragOverIndex.value).toBe(1)
    })

    it('onDragEnd resets drag state', () => {
      const { draggedIndex, dragOverIndex, onDragStart, onDragEnd } = useBookletOrder(threeBooklets())
      onDragStart(mockDragEvent(), 0)

      onDragEnd()

      expect(draggedIndex.value).toBeNull()
      expect(dragOverIndex.value).toBeNull()
    })

    it('drops a booklet at its new place and keeps the order with the account', async () => {
      const { orderedItems, onDragStart, onDrop } = useBookletOrder(threeBooklets())

      onDragStart(mockDragEvent(), 0)
      await onDrop(mockDragEvent(), 2)

      expect(ids(orderedItems.value)).toEqual(['2', '3', '1'])
      expect(reorderBooklets).toHaveBeenCalledWith(['2', '3', '1'])
    })

    it('does nothing when a booklet is dropped where it was', async () => {
      const { orderedItems, draggedIndex, onDragStart, onDrop } = useBookletOrder(threeBooklets())

      onDragStart(mockDragEvent(), 1)
      await onDrop(mockDragEvent(), 1)

      expect(ids(orderedItems.value)).toEqual(['1', '2', '3'])
      expect(draggedIndex.value).toBeNull()
      expect(reorderBooklets).not.toHaveBeenCalled()
    })

    it('resets the drag state after a drop', async () => {
      const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop } = useBookletOrder(threeBooklets())

      onDragStart(mockDragEvent(), 0)
      onDragOver(mockDragEvent(), 1)
      await onDrop(mockDragEvent(), 1)

      expect(draggedIndex.value).toBeNull()
      expect(dragOverIndex.value).toBeNull()
    })
  })

  // An earlier version kept the order in this browser only.
  describe('the order kept in this browser', () => {
    function givenBrowserOrder(order: string[]) {
      window.localStorage.setItem(LEGACY_BOOKLET_ORDER_KEY, JSON.stringify(order))
    }

    it('is taken over once when the account holds no order yet', async () => {
      givenBrowserOrder(['3', '1', '2'])
      const { orderedItems, migrateLegacyOrder } = useBookletOrder(threeBooklets())

      await migrateLegacyOrder()

      expect(reorderBooklets).toHaveBeenCalledWith(['3', '1', '2'])
      expect(ids(orderedItems.value)).toEqual(['3', '1', '2'])
      expect(window.localStorage.getItem(LEGACY_BOOKLET_ORDER_KEY)).toBeNull()
    })

    it('places the booklets it does not know after the others, and drops the ones that are gone', async () => {
      givenBrowserOrder(['gone', '3', '1'])
      const { orderedItems, migrateLegacyOrder } = useBookletOrder(threeBooklets())

      await migrateLegacyOrder()

      expect(ids(orderedItems.value)).toEqual(['3', '1', '2'])
      expect(reorderBooklets).toHaveBeenCalledWith(['3', '1', '2'])
    })

    // Another device may have sent its own already: the account wins.
    it('is forgotten without being sent when the account already holds an order', async () => {
      givenBrowserOrder(['3', '1', '2'])
      const { orderedItems, migrateLegacyOrder } = useBookletOrder(threeBooklets(0))

      await migrateLegacyOrder()

      expect(reorderBooklets).not.toHaveBeenCalled()
      expect(ids(orderedItems.value)).toEqual(['1', '2', '3'])
      expect(window.localStorage.getItem(LEGACY_BOOKLET_ORDER_KEY)).toBeNull()
    })

    it('is forgotten without being sent when it is the order shown already', async () => {
      givenBrowserOrder(['1', '2', '3'])
      const { migrateLegacyOrder } = useBookletOrder(threeBooklets())

      await migrateLegacyOrder()

      expect(reorderBooklets).not.toHaveBeenCalled()
      expect(window.localStorage.getItem(LEGACY_BOOKLET_ORDER_KEY)).toBeNull()
    })

    it('is kept to try again on the next visit when the account cannot take it', async () => {
      givenBrowserOrder(['3', '1', '2'])
      reorderBooklets.mockRejectedValueOnce(new Error('down'))
      const { orderedItems, migrateLegacyOrder } = useBookletOrder(threeBooklets())

      await migrateLegacyOrder()

      expect(ids(orderedItems.value)).toEqual(['1', '2', '3'])
      expect(window.localStorage.getItem(LEGACY_BOOKLET_ORDER_KEY)).not.toBeNull()
    })

    it('leaves everything alone when there is none', async () => {
      const { migrateLegacyOrder } = useBookletOrder(threeBooklets())

      await migrateLegacyOrder()

      expect(reorderBooklets).not.toHaveBeenCalled()
    })
  })
})
