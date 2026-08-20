<script setup lang="ts">
interface Props {
  isMobile: boolean
  modelValue: boolean
  hasRegenerableTransactions: boolean
  isAnyActionLoading: boolean
  isRegenerateLoading: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  'importCsv': []
  'exportCsv': []
  'newTransaction': []
  'newPreview': []
  'regenerate': []
}>()
</script>

<template>
  <Transition name="fab">
    <button
      v-if="isMobile"
      aria-label="Ouvrir les actions"
      class="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-lg flex items-center justify-center z-40 transition-all duration-300 hover:shadow-xl hover:scale-110 active:scale-95"
      @click="emit('update:modelValue', !modelValue)"
    >
      <i :class="modelValue ? 'pi pi-times text-xl' : 'pi pi-plus text-xl'" />
    </button>
  </Transition>

  <Transition name="overlay">
    <div
      v-if="isMobile && modelValue"
      class="fixed inset-0 z-50 flex items-end justify-center backdrop-blur-md bg-black/30"
      @click="emit('update:modelValue', false)"
    >
      <Transition name="menu-slide">
        <div
          v-if="modelValue"
          class="w-full max-w-md bg-[var(--card-bg)] rounded-t-3xl shadow-2xl px-5 pt-5 pb-8"
          @click.stop
        >
          <!-- Handle + Header -->
          <div class="w-10 h-1 rounded-full bg-[var(--border-color)] mx-auto mb-4" />
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-bold text-[var(--text-primary)] m-0">
              Actions
            </h3>
            <button
              class="w-7 h-7 rounded-full flex items-center justify-center text-[var(--text-secondary)] hover:bg-[var(--card-hover-bg)] transition-colors"
              @click="emit('update:modelValue', false)"
            >
              <i class="pi pi-times text-xs" />
            </button>
          </div>

          <!-- Transactions section -->
          <p class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)] mb-2">
            Transactions
          </p>
          <div class="flex flex-col gap-2 mb-4">
            <button
              class="flex items-center gap-3 p-3 rounded-xl border text-left transition-all active:scale-[0.98] disabled:opacity-50 bg-[var(--primary)]/5 border-[var(--primary)]/20 hover:border-[var(--primary)]/40"

              :disabled="isAnyActionLoading"
              @click="emit('newTransaction'); emit('update:modelValue', false)"
            >
              <div class="w-8 h-8 rounded-full bg-[var(--primary)]/15 flex items-center justify-center shrink-0">
                <i class="pi pi-plus text-[var(--primary)] text-sm" />
              </div>
              <span class="font-semibold text-sm text-[var(--text-primary)]">Nouvelle transaction</span>
            </button>

            <button
              class="flex items-center gap-3 p-3 rounded-xl border text-left transition-all active:scale-[0.98] disabled:opacity-50 bg-amber-500/5 border-amber-500/20 hover:border-amber-500/40"
              :disabled="isAnyActionLoading"
              @click="emit('newPreview'); emit('update:modelValue', false)"
            >
              <div class="w-8 h-8 rounded-full bg-amber-500/15 flex items-center justify-center shrink-0">
                <i class="pi pi-clock text-amber-600 text-sm" />
              </div>
              <span class="font-semibold text-sm text-[var(--text-primary)]">Transaction prévisionnelle</span>
            </button>

            <button
              v-if="hasRegenerableTransactions"
              class="flex items-center gap-3 p-3 rounded-xl border text-left transition-all active:scale-[0.98] disabled:opacity-50 bg-amber-500/5 border-amber-500/20 hover:border-amber-500/40"
              :disabled="isAnyActionLoading || isRegenerateLoading"
              @click="emit('regenerate'); emit('update:modelValue', false)"
            >
              <div class="w-8 h-8 rounded-full bg-amber-500/15 flex items-center justify-center shrink-0">
                <i class="pi pi-refresh text-amber-600 text-sm" :class="isRegenerateLoading ? 'pi-spin' : ''" />
              </div>
              <span class="font-semibold text-sm text-[var(--text-primary)]">Régénérer les transactions</span>
            </button>
          </div>

          <!-- Divider -->
          <div class="h-px bg-[var(--border-color)] mb-4" />

          <!-- CSV section -->
          <p class="text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--text-tertiary)] mb-2">
            CSV
          </p>
          <div class="flex flex-col gap-2">
            <button
              class="flex items-center gap-3 p-3 rounded-xl border text-left transition-all active:scale-[0.98] bg-cyan-500/5 border-cyan-500/20 hover:border-cyan-500/40"
              @click="emit('importCsv'); emit('update:modelValue', false)"
            >
              <div class="w-8 h-8 rounded-full bg-cyan-500/15 flex items-center justify-center shrink-0">
                <i class="pi pi-file-import text-cyan-600 text-sm" />
              </div>
              <span class="font-semibold text-sm text-[var(--text-primary)]">Importer CSV</span>
            </button>

            <button
              class="flex items-center gap-3 p-3 rounded-xl border text-left transition-all active:scale-[0.98] bg-emerald-500/5 border-emerald-500/20 hover:border-emerald-500/40"
              @click="emit('exportCsv'); emit('update:modelValue', false)"
            >
              <div class="w-8 h-8 rounded-full bg-emerald-500/15 flex items-center justify-center shrink-0">
                <i class="pi pi-file-export text-emerald-600 text-sm" />
              </div>
              <span class="font-semibold text-sm text-[var(--text-primary)]">Exporter CSV</span>
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.fab-enter-active,
.fab-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fab-enter-from,
.fab-leave-to {
  opacity: 0;
  transform: scale(0.5) rotate(180deg);
}

.overlay-enter-active,
.overlay-leave-active {
  transition: all 0.3s ease;
}
.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}

.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.menu-slide-enter-from,
.menu-slide-leave-to {
  opacity: 0;
  transform: translateY(100%);
}
</style>
