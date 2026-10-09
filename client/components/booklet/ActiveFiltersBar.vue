<script setup lang="ts">
export interface ActiveFilter {
  key: string
  label: string
}

interface Props {
  filters: ActiveFilter[]
}

defineProps<Props>()

const emit = defineEmits<{
  remove: [key: string]
  clearAll: []
}>()
</script>

<template>
  <div v-if="filters.length > 0" class="flex flex-wrap items-center gap-2" data-test="active-filters" aria-label="Filtres actifs">
    <span
      v-for="filter in filters"
      :key="filter.key"
      class="inline-flex items-center gap-1 pl-2.5 pr-1 py-0.5 rounded-full border border-[var(--primary)] bg-[var(--primary)]/7 text-xs font-semibold text-[var(--primary)] max-w-full"
    >
      <span class="truncate" data-test="active-filter">{{ filter.label }}</span>
      <button
        type="button"
        class="w-5 h-5 flex items-center justify-center rounded-full hover:bg-[var(--primary)]/15 focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
        :aria-label="`Retirer le filtre ${filter.label}`"
        data-test="remove-filter"
        @click="emit('remove', filter.key)"
      >
        <i class="pi pi-times text-[0.65rem]" aria-hidden="true" />
      </button>
    </span>
    <button
      type="button"
      class="text-xs font-semibold text-[var(--text-secondary)] underline underline-offset-2 hover:text-[var(--primary)]"
      data-test="clear-filters"
      @click="emit('clearAll')"
    >
      Tout effacer
    </button>
  </div>
</template>
