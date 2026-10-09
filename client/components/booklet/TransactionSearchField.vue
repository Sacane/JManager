<script setup lang="ts">
/** A free-text search over the labels of the transactions displayed. */
const model = defineModel<string>({ required: true })

function clear() {
  model.value = ''
}
</script>

<template>
  <div class="relative w-full">
    <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-sm text-[var(--text-secondary)] pointer-events-none" aria-hidden="true" />
    <input
      v-model="model"
      type="search"
      maxlength="100"
      autocomplete="off"
      placeholder="Rechercher une transaction…"
      aria-label="Rechercher une transaction par son libellé"
      class="w-full h-9 pl-9 pr-9 rounded-lg border border-[var(--card-border)] bg-[var(--card-bg)] text-sm text-[var(--text-primary)] placeholder:text-[var(--text-secondary)] outline-none focus-visible:border-[var(--primary)] focus-visible:ring-2 focus-visible:ring-[var(--primary)]/20"
      data-test="transaction-search-input"
    >
    <button
      v-if="model"
      type="button"
      class="absolute right-2 top-1/2 -translate-y-1/2 w-6 h-6 flex items-center justify-center rounded-md text-[var(--text-secondary)] hover:text-[var(--text-primary)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
      aria-label="Effacer la recherche"
      data-test="clear-search"
      @click="clear"
    >
      <i class="pi pi-times text-xs" aria-hidden="true" />
    </button>
  </div>
</template>

<style scoped>
/* The field brings its own clear button: hide the browser's, which would make two. */
input[type='search']::-webkit-search-cancel-button {
  appearance: none;
}
</style>
