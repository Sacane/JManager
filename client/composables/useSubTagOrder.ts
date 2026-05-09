import type { Ref } from 'vue'

const SUB_TAG_ORDER_KEY_PREFIX = 'jmanager-subtag-order'

/**
 * Provides drag-and-drop reordering for a list of sub-tags scoped to a parent tag,
 * with localStorage persistence keyed by parentId.
 */
export function useSubTagOrder(parentId: string, items: Ref<TagDisplayItem[]>) {
  const savedOrder = useLocalStorage<string[]>(`${SUB_TAG_ORDER_KEY_PREFIX}-${parentId}`, [])
  const draggedIndex = ref<number | null>(null)
  const dragOverIndex = ref<number | null>(null)

  const orderedItems = computed(() => {
    if (!savedOrder.value.length) return items.value
    const orderMap = new Map(savedOrder.value.map((id, i) => [id, i]))
    return [...items.value].sort((a, b) => {
      const ai = orderMap.has(a.id) ? orderMap.get(a.id)! : Infinity
      const bi = orderMap.has(b.id) ? orderMap.get(b.id)! : Infinity
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
    savedOrder.value = reordered.map(t => t.id)
    draggedIndex.value = null
    dragOverIndex.value = null
  }

  function onDragEnd() {
    draggedIndex.value = null
    dragOverIndex.value = null
  }

  return { orderedItems, draggedIndex, dragOverIndex, onDragStart, onDragOver, onDrop, onDragEnd }
}

export default useSubTagOrder
