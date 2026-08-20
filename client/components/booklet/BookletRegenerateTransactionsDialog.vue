<script setup lang="ts">
import type { RegenerableTransactionDTO } from '~/composables/useBooklet'

interface Props {
  visible: boolean
  candidates: RegenerableTransactionDTO[]
  loading?: boolean
  loadingCandidates?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  loadingCandidates: false,
})

const emit = defineEmits<{
  'update:visible': [val: boolean]
  'confirm': [regularTransactionIds: string[]]
}>()

/**
 * Selection is keyed by `regularTransactionId`, not by occurrence: the backend excludes a whole
 * (regular transaction, month) pair, so occurrences of the same recurrence can only be restored
 * together. Rows sharing an identifier therefore toggle as one.
 */
const selectedIds = ref<string[]>([])

const distinctIds = computed(() => [...new Set(props.candidates.map(c => c.regularTransactionId))])

const occurrenceCountById = computed(() => {
  const counts = new Map<string, number>()
  for (const candidate of props.candidates) {
    counts.set(candidate.regularTransactionId, (counts.get(candidate.regularTransactionId) ?? 0) + 1)
  }
  return counts
})

const hasCandidates = computed(() => props.candidates.length > 0)
const allSelected = computed(() => distinctIds.value.length > 0 && selectedIds.value.length === distinctIds.value.length)
const canConfirm = computed(() => selectedIds.value.length > 0 && !props.loading)

function isSelected(regularTransactionId: string): boolean {
  return selectedIds.value.includes(regularTransactionId)
}

function toggle(regularTransactionId: string) {
  selectedIds.value = isSelected(regularTransactionId)
    ? selectedIds.value.filter(id => id !== regularTransactionId)
    : [...selectedIds.value, regularTransactionId]
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : [...distinctIds.value]
}

function groupedOccurrenceCount(regularTransactionId: string): number {
  return occurrenceCountById.value.get(regularTransactionId) ?? 0
}

function formatDate(isoDate: string): string {
  const parsed = new Date(isoDate)
  return Number.isNaN(parsed.getTime()) ? isoDate : parsed.toLocaleDateString('fr-FR')
}

function close() {
  emit('update:visible', false)
}

// The selection is reset only when the dialog opens: a failed confirmation keeps the dialog open
// with the user's choices intact so they can retry without redoing the work.
watch(() => props.visible, (isVisible) => {
  if (isVisible) selectedIds.value = []
})
</script>

<template>
  <Dialog
    :visible="visible"
    modal
    header="Restaurer des transactions supprimées"
    :style="{ width: '32rem' }"
    :breakpoints="{ '960px': '90vw', '640px': '95vw' }"
    :pt="{
      root: { class: 'regen-dialog-root' },
      mask: { class: 'regen-dialog-mask' },
      header: { class: 'regen-dialog-header' },
      title: { class: 'regen-dialog-title' },
      closeButton: { class: 'regen-dialog-close-btn' },
      content: { class: 'regen-dialog-content' },
    }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <div v-if="loadingCandidates" class="body-base py-6 text-center">
        Chargement des transactions supprimées…
      </div>

      <div v-else-if="!hasCandidates" class="body-base py-6 text-center">
        Aucune transaction supprimée à restaurer pour cette période.
      </div>

      <template v-else>
        <p class="body-base">
          Sélectionnez les transactions prévisionnelles que vous souhaitez restaurer.
        </p>

        <div class="regen-select-all flex items-center gap-3 px-3 py-2 rounded-lg border">
          <Checkbox
            :binary="true"
            :model-value="allSelected"
            input-id="regen-select-all"
            aria-label="Tout sélectionner"
            @update:model-value="toggleAll"
          />
          <label for="regen-select-all" class="text-label cursor-pointer">Tout sélectionner</label>
        </div>

        <ul class="flex flex-col gap-2 max-h-80 overflow-y-auto pr-1">
          <li
            v-for="(candidate, index) in candidates"
            :key="`${candidate.regularTransactionId}-${candidate.date}-${index}`"
            class="regen-row flex items-center gap-3 px-3 py-2 rounded-lg border"
          >
            <Checkbox
              :binary="true"
              :model-value="isSelected(candidate.regularTransactionId)"
              :input-id="`regen-row-${index}`"
              :aria-label="`Restaurer ${candidate.label} du ${formatDate(candidate.date)}`"
              @update:model-value="toggle(candidate.regularTransactionId)"
            />
            <label :for="`regen-row-${index}`" class="flex-1 min-w-0 cursor-pointer">
              <span class="block text-label truncate">{{ candidate.label }}</span>
              <span class="block text-muted">
                {{ formatDate(candidate.date) }}
                <template v-if="groupedOccurrenceCount(candidate.regularTransactionId) > 1">
                  · restaure les {{ groupedOccurrenceCount(candidate.regularTransactionId) }} occurrences du mois
                </template>
              </span>
            </label>
            <span
              class="text-sm font-bold whitespace-nowrap"
              :class="candidate.isIncome ? 'text-emerald-500' : 'text-red-500'"
            >
              {{ candidate.isIncome ? '+' : '-' }} {{ candidate.value }} {{ candidate.currency }}
            </span>
          </li>
        </ul>
      </template>

      <div class="flex justify-end gap-2 mt-2">
        <Button
          type="button"
          label="Annuler"
          severity="secondary"
          :disabled="loading"
          @click="close"
        />
        <Button
          type="button"
          label="Restaurer"
          :loading="loading"
          :disabled="!canConfirm"
          @click="emit('confirm', [...selectedIds])"
        />
      </div>
    </div>
  </Dialog>
</template>

<style scoped lang="scss">
// Colours come exclusively from the design-system tokens in assets/css/variables.css, which already
// flip under `.dark` — no theme-specific override is needed here.
.regen-select-all,
.regen-row {
  background: var(--bg-tertiary);
  border-color: var(--card-border);
  color: var(--text-primary);
}

.regen-row {
  transition: border-color 0.2s ease;
}

.regen-row:hover {
  border-color: var(--primary);
}
</style>

<!-- Global styles for PrimeVue Dialog passthrough classes -->
<style lang="scss">
.regen-dialog-root {
  background: var(--card-bg) !important;
  border: 1px solid var(--card-border) !important;
  color: var(--text-primary) !important;
  overflow: hidden;
  box-shadow: 0 18px 42px var(--shadow-lg), 0 8px 22px var(--shadow-purple) !important;
}

.regen-dialog-mask {
  backdrop-filter: blur(2px);
  background: rgba(15, 23, 42, 0.32) !important;
}

.regen-dialog-header {
  background: var(--bg-tertiary) !important;
  border-bottom: 1px solid var(--card-border) !important;
}

.regen-dialog-header,
.regen-dialog-content,
.regen-dialog-title {
  color: var(--text-primary) !important;
}

.regen-dialog-content {
  background: transparent !important;
}

.regen-dialog-close-btn {
  color: var(--text-secondary) !important;
  border: 1px solid var(--card-border) !important;
  background: var(--bg-secondary) !important;
  transition: all 0.2s ease;
}

.regen-dialog-close-btn:hover {
  color: var(--primary) !important;
  border-color: var(--primary) !important;
  background: var(--card-hover-bg) !important;
}

.dark .regen-dialog-mask {
  background: rgba(2, 6, 23, 0.58) !important;
}
</style>
