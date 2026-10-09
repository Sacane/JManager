<script setup lang="ts">
/**
 * What a booklet shows when no row is displayed: an invitation to create the first transaction when
 * the period holds none, or "nothing matches" when filters hide every row.
 */
interface Props {
  /** Whether a search or a tag filter is active. */
  filtered: boolean
  search?: string
  createLabel?: string
}

withDefaults(defineProps<Props>(), {
  search: '',
  createLabel: 'Créer une transaction',
})

const emit = defineEmits<{
  create: []
  clearFilters: []
}>()
</script>

<template>
  <div v-if="filtered" class="text-center py-12 px-4" data-test="booklet-no-match" role="status">
    <i class="pi pi-search text-4xl text-[var(--text-muted)]" aria-hidden="true" />
    <h3 class="text-xl font-bold text-[var(--text-primary)] mt-4 mb-2">
      Aucune transaction ne correspond
    </h3>
    <p class="text-[var(--text-secondary)] mb-4 break-words">
      <template v-if="search">
        Aucune transaction de cette période ne correspond à « {{ search }} ».
      </template>
      <template v-else>
        Aucune transaction de cette période ne correspond aux filtres actifs.
      </template>
    </p>
    <Button class="btn-outline-primary" icon="pi pi-filter-slash" label="Effacer les filtres" @click="emit('clearFilters')" />
  </div>
  <div v-else class="text-center py-12 px-4" data-test="booklet-empty">
    <i class="pi pi-inbox text-4xl text-[var(--text-muted)]" aria-hidden="true" />
    <h3 class="text-xl font-bold text-[var(--text-primary)] mt-4 mb-2">
      Aucune transaction
    </h3>
    <p class="text-[var(--text-secondary)] mb-4">
      Commencez par créer votre première transaction
    </p>
    <Button class="btn-primary" icon="pi pi-plus" :label="createLabel" @click="emit('create')" />
  </div>
</template>
