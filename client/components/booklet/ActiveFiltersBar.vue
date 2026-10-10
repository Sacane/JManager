<script setup lang="ts">
export interface ActiveFilter {
  key: string
  label: string
}

interface Props {
  /** The filters to show as chips: those not visible elsewhere once picked. */
  filters: ActiveFilter[]
  /** Whether to offer clearing every filter at once: only worth it when several are active. */
  showClearAll?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showClearAll: false,
})

const emit = defineEmits<{
  remove: [key: string]
  clearAll: []
}>()
</script>

<template>
  <div
    v-if="props.filters.length > 0 || props.showClearAll"
    class="flex flex-wrap items-center gap-2"
    data-test="active-filters"
    aria-label="Filtres actifs"
  >
    <span
      v-for="filter in props.filters"
      :key="filter.key"
      class="inline-flex items-center gap-1 max-w-full pl-2.5 pr-1 py-0.5 rounded-full border border-[rgba(var(--primary-rgb),0.35)] bg-[rgba(var(--primary-rgb),0.1)] text-xs font-semibold text-[var(--primary)] dark:(border-[rgba(var(--primary-rgb),0.6)] bg-[rgba(var(--primary-rgb),0.25)] text-[var(--primary-lighter)])"
    >
      <span class="truncate" data-test="active-filter">{{ filter.label }}</span>
      <button
        type="button"
        class="w-5 h-5 flex items-center justify-center rounded-full border-0 bg-transparent text-inherit cursor-pointer hover:bg-[rgba(var(--primary-rgb),0.15)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
        :aria-label="`Retirer le filtre ${filter.label}`"
        data-test="remove-filter"
        @click="emit('remove', filter.key)"
      >
        <i class="pi pi-times text-[0.65rem]" aria-hidden="true" />
      </button>
    </span>
    <button
      v-if="props.showClearAll"
      type="button"
      class="p-0 border-0 bg-transparent cursor-pointer text-xs font-semibold text-[var(--text-secondary)] underline underline-offset-2 hover:text-[var(--primary)] dark:hover:text-[var(--primary-lighter)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
      data-test="clear-filters"
      @click="emit('clearAll')"
    >
      Tout effacer
    </button>
  </div>
</template>
