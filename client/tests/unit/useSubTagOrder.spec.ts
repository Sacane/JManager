import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useSubTagOrder from '../../composables/useSubTagOrder'

interface Item { id: string }

function mockDragEvent(overrides: Record<string, any> = {}): DragEvent {
  return { preventDefault: vi.fn(), dataTransfer: { effectAllowed: '', dropEffect: '' }, ...overrides } as any
}

describe('composables/useSubTagOrder', () => {
  it('returns items in original order when no saved order exists', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }, { id: 'c' }])
    const { orderedItems } = useSubTagOrder('parent-1', items as any)

    expect(orderedItems.value.map(i => i.id)).toEqual(['a', 'b', 'c'])
  })

  it('onDragStart sets draggedIndex', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }])
    const { draggedIndex, onDragStart } = useSubTagOrder('parent-1', items as any)

    onDragStart(mockDragEvent(), 0)

    expect(draggedIndex.value).toBe(0)
  })

  it('onDragOver updates dragOverIndex', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }])
    const { dragOverIndex, onDragOver } = useSubTagOrder('parent-1', items as any)

    onDragOver(mockDragEvent(), 1)

    expect(dragOverIndex.value).toBe(1)
  })

  it('onDragEnd resets drag state', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }])
    const { draggedIndex, dragOverIndex, onDragStart, onDragEnd } = useSubTagOrder('parent-1', items as any)

    onDragStart(mockDragEvent(), 0)
    expect(draggedIndex.value).toBe(0)

    onDragEnd()

    expect(draggedIndex.value).toBeNull()
    expect(dragOverIndex.value).toBeNull()
  })

  it('onDrop reorders items and persists new order via savedOrder', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }, { id: 'c' }])
    const { orderedItems, onDragStart, onDrop } = useSubTagOrder('parent-1', items as any)

    onDragStart(mockDragEvent(), 0)
    onDrop(mockDragEvent(), 2)

    expect(orderedItems.value.map(i => i.id)).toEqual(['b', 'c', 'a'])
  })

  it('onDrop does nothing when source and target index are identical', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }, { id: 'c' }])
    const { orderedItems, draggedIndex, onDragStart, onDrop } = useSubTagOrder('parent-1', items as any)

    onDragStart(mockDragEvent(), 1)
    onDrop(mockDragEvent(), 1)

    expect(orderedItems.value.map(i => i.id)).toEqual(['a', 'b', 'c'])
    expect(draggedIndex.value).toBeNull()
  })

  it('onDrop resets drag state after reorder', () => {
    const items = ref<Item[]>([{ id: 'a' }, { id: 'b' }])
    const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop } = useSubTagOrder('parent-1', items as any)

    onDragStart(mockDragEvent(), 0)
    onDragOver(mockDragEvent(), 1)
    onDrop(mockDragEvent(), 1)

    expect(draggedIndex.value).toBeNull()
    expect(dragOverIndex.value).toBeNull()
  })

  it('scopes storage independently per parentId', () => {
    const items1 = ref<Item[]>([{ id: 'a' }, { id: 'b' }])
    const items2 = ref<Item[]>([{ id: 'x' }, { id: 'y' }])

    const order1 = useSubTagOrder('parent-1', items1 as any)
    const order2 = useSubTagOrder('parent-2', items2 as any)

    order1.onDragStart(mockDragEvent(), 0)
    order1.onDrop(mockDragEvent(), 1)

    expect(order1.orderedItems.value.map(i => i.id)).toEqual(['b', 'a'])
    expect(order2.orderedItems.value.map(i => i.id)).toEqual(['x', 'y'])
  })
})
