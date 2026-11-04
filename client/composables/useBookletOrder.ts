import type { AxiosError } from 'axios'
import type { Ref } from 'vue'

/** Where an earlier version kept the order of the booklets, in this browser only. */
export const LEGACY_BOOKLET_ORDER_KEY = 'jmanager-booklet-order'

interface OrderableBooklet {
  id: string | number | null | undefined
  /** Where the user placed the booklet; null until they order their booklets. */
  displayPosition?: number | null
}

/**
 * The booklets in the order the user chose, kept with the account (the server lists them in that order),
 * and the ways to change it: one step at a time, or by drag and drop. A move shows at once and is undone
 * when the account cannot take it.
 */
export function useBookletOrder<T extends OrderableBooklet>(items: Ref<T[]>) {
  const { reorderBooklets } = useBooklet()
  const toast = useJToast()

  const orderedItems = ref([]) as Ref<T[]>
  const draggedIndex = ref<number | null>(null)
  const dragOverIndex = ref<number | null>(null)

  // The last order the account took: where a failed save goes back to.
  let savedOrder: T[] = []
  // One save at a time, in the order of the moves: the account ends with the last one.
  let pendingSave: Promise<void> = Promise.resolve()

  // Synchronous: a caller that loads the booklets and then migrates must see them at once.
  watch(items, (next) => {
    orderedItems.value = [...next]
    savedOrder = [...next]
  }, { immediate: true, flush: 'sync' })

  const idsOf = (booklets: T[]) => booklets.map(booklet => String(booklet.id))

  function save(next: T[]): Promise<void> {
    orderedItems.value = next
    pendingSave = pendingSave.then(async () => {
      try {
        await reorderBooklets(idsOf(next))
        savedOrder = next
      } catch (err) {
        orderedItems.value = savedOrder
        toast.errorAxios(err as AxiosError, 'L\'ordre des livrets n\'a pas pu être enregistré')
      }
    })
    return pendingSave
  }

  function moved(from: number, to: number): T[] {
    const next = [...orderedItems.value]
    const [booklet] = next.splice(from, 1)
    next.splice(to, 0, booklet!)
    return next
  }

  /** Moves the booklet at [index] one step before (-1) or after (1); nothing past either end. */
  function moveBooklet(index: number, offset: -1 | 1): Promise<void> {
    const target = index + offset
    if (index < 0 || index >= orderedItems.value.length || target < 0 || target >= orderedItems.value.length) {
      return pendingSave
    }
    return save(moved(index, target))
  }

  function onDragStart(event: DragEvent, index: number) {
    draggedIndex.value = index
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
  }

  function onDragOver(event: DragEvent, index: number) {
    event.preventDefault()
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
    dragOverIndex.value = index
  }

  function onDrop(event: DragEvent, targetIndex: number): Promise<void> {
    event.preventDefault()
    const from = draggedIndex.value
    draggedIndex.value = null
    dragOverIndex.value = null
    if (from === null || from === targetIndex) return pendingSave
    return save(moved(from, targetIndex))
  }

  function onDragEnd() {
    draggedIndex.value = null
    dragOverIndex.value = null
  }

  function readLegacyOrder(): string[] | null {
    try {
      const raw = window.localStorage.getItem(LEGACY_BOOKLET_ORDER_KEY)
      if (raw === null) return null
      const parsed: unknown = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed.map(String) : []
    } catch {
      return []
    }
  }

  function forgetLegacyOrder() {
    try {
      window.localStorage.removeItem(LEGACY_BOOKLET_ORDER_KEY)
    } catch {
      // Storage unavailable: nothing to forget.
    }
  }

  /**
   * Sends the order an earlier version kept in this browser to the account, once. The account wins when it
   * already holds an order, as another device may have sent its own. When the account cannot take it, the
   * browser copy is kept to try again on the next visit.
   */
  async function migrateLegacyOrder(): Promise<void> {
    const legacy = readLegacyOrder()
    if (legacy === null) return
    if (orderedItems.value.some(booklet => booklet.displayPosition != null)) {
      forgetLegacyOrder()
      return
    }

    const rank = new Map(legacy.map((id, index) => [id, index]))
    const rankOf = (booklet: T) => rank.get(String(booklet.id)) ?? Number.MAX_SAFE_INTEGER
    const current = orderedItems.value
    const next = [...current].sort((a, b) => rankOf(a) - rankOf(b))
    if (next.every((booklet, index) => booklet === current[index])) {
      forgetLegacyOrder()
      return
    }

    orderedItems.value = next
    try {
      await reorderBooklets(idsOf(next))
      savedOrder = next
      forgetLegacyOrder()
    } catch {
      orderedItems.value = current
    }
  }

  return { orderedItems, draggedIndex, dragOverIndex, moveBooklet, onDragStart, onDragOver, onDrop, onDragEnd, migrateLegacyOrder }
}

export default useBookletOrder
