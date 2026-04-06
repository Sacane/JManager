import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useBookletOrder from '../../composables/useBookletOrder'

interface Item { id: string | number }

function mockDragEvent(overrides: Record<string, any> = {}): DragEvent {
  return { preventDefault: vi.fn(), dataTransfer: { effectAllowed: '', dropEffect: '' }, ...overrides } as any
}

describe('composables/useBookletOrder', () => {
  it('returns items in original order when no saved order exists', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }, { id: '3' }])
    const { orderedItems } = useBookletOrder(items)

    expect(orderedItems.value.map(i => i.id)).toEqual(['1', '2', '3'])
  })

  it('onDragStart sets draggedIndex', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }])
    const { draggedIndex, onDragStart } = useBookletOrder(items)

    onDragStart(mockDragEvent(), 0)

    expect(draggedIndex.value).toBe(0)
  })

  it('onDragOver updates dragOverIndex', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }])
    const { dragOverIndex, onDragOver } = useBookletOrder(items)

    onDragOver(mockDragEvent(), 1)

    expect(dragOverIndex.value).toBe(1)
  })

  it('onDragEnd resets drag state', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }])
    const { draggedIndex, dragOverIndex, onDragStart, onDragEnd } = useBookletOrder(items)

    onDragStart(mockDragEvent(), 0)
    expect(draggedIndex.value).toBe(0)

    onDragEnd()

    expect(draggedIndex.value).toBeNull()
    expect(dragOverIndex.value).toBeNull()
  })

  it('onDrop reorders items and persists new order via savedOrder', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }, { id: '3' }])
    const { orderedItems, onDragStart, onDrop } = useBookletOrder(items)

    // drag item at index 0 to index 2
    onDragStart(mockDragEvent(), 0)
    onDrop(mockDragEvent(), 2)

    expect(orderedItems.value.map(i => i.id)).toEqual(['2', '3', '1'])
  })

  it('onDrop does nothing when source and target index are identical', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }, { id: '3' }])
    const { orderedItems, draggedIndex, onDragStart, onDrop } = useBookletOrder(items)

    onDragStart(mockDragEvent(), 1)
    onDrop(mockDragEvent(), 1)

    expect(orderedItems.value.map(i => i.id)).toEqual(['1', '2', '3'])
    expect(draggedIndex.value).toBeNull()
  })

  it('onDrop resets drag state after reorder', () => {
    const items = ref<Item[]>([{ id: '1' }, { id: '2' }])
    const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop } = useBookletOrder(items)

    onDragStart(mockDragEvent(), 0)
    onDragOver(mockDragEvent(), 1)
    onDrop(mockDragEvent(), 1)

    expect(draggedIndex.value).toBeNull()
    expect(dragOverIndex.value).toBeNull()
  })
})
