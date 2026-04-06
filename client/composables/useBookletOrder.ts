import type { Ref } from 'vue'

const BOOKLET_ORDER_KEY = 'jmanager-booklet-order'

/**
 * Provides drag-and-drop reordering for a list of booklets with localStorage persistence.
 * The saved order is shared across all views that use this composable.
 */
export function useBookletOrder<T extends { id: string | number | null | undefined }>(items: Ref<T[]>) {
  const savedOrder = useLocalStorage<string[]>(BOOKLET_ORDER_KEY, [])
  const draggedIndex = ref<number | null>(null)
  const dragOverIndex = ref<number | null>(null)

  const orderedItems = computed(() => {
    if (!savedOrder.value.length) return items.value
    const orderMap = new Map(savedOrder.value.map((id, i) => [id, i]))
    return [...items.value].sort((a, b) => {
      const ai = orderMap.has(String(a.id)) ? orderMap.get(String(a.id))! : Infinity
      const bi = orderMap.has(String(b.id)) ? orderMap.get(String(b.id))! : Infinity
      return ai - bi
    })
  })

  function onDragStart(event: DragEvent, index: number) {
    draggedIndex.value = index
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
  }

  function onDragOver(event: DragEvent, index: number) {
    event.preventDefault()
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
    dragOverIndex.value = index
  }

  function onDrop(event: DragEvent, targetIndex: number) {
    event.preventDefault()
    if (draggedIndex.value === null || draggedIndex.value === targetIndex) {
      draggedIndex.value = null
      dragOverIndex.value = null
      return
    }
    const reordered = [...orderedItems.value]
    const [moved] = reordered.splice(draggedIndex.value, 1)
    reordered.splice(targetIndex, 0, moved)
    savedOrder.value = reordered.map(b => String(b.id))
    draggedIndex.value = null
    dragOverIndex.value = null
  }

  function onDragEnd() {
    draggedIndex.value = null
    dragOverIndex.value = null
  }

  return { orderedItems, draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop, onDragEnd }
}

export default useBookletOrder
