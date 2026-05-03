<script setup lang="ts">
interface Props {
  isMobile: boolean
  modelValue: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  'import-csv': []
  'export-csv': []
}>()
</script>

<template>
  <Transition name="fab">
    <button
      v-if="isMobile"
      class="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white shadow-lg flex items-center justify-center z-40 transition-all duration-300 hover:shadow-xl hover:scale-110 active:scale-95"
      @click="emit('update:modelValue', !modelValue)"
    >
      <i :class="modelValue ? 'pi pi-times text-xl' : 'pi pi-ellipsis-h text-xl'" />
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
          class="w-full max-w-md bg-[var(--card-bg)] rounded-t-3xl shadow-2xl p-6 mb-0"
          @click.stop
        >
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-xl font-bold text-[var(--text-primary)] m-0">
              Actions CSV
            </h3>
            <button
              class="w-8 h-8 rounded-full flex items-center justify-center text-[var(--text-secondary)] hover:bg-[var(--card-hover-bg)] transition-colors"
              @click="emit('update:modelValue', false)"
            >
              <i class="pi pi-times" />
            </button>
          </div>

          <div class="flex flex-col gap-3">
            <button
              class="flex items-center gap-4 p-4 rounded-xl bg-gradient-to-br from-cyan-500/10 to-cyan-600/5 border-2 border-cyan-500/20 text-left transition-all hover:border-cyan-500/40 hover:shadow-lg active:scale-[0.98]"
              @click="emit('import-csv'); emit('update:modelValue', false)"
            >
              <div class="w-12 h-12 rounded-full bg-cyan-500/20 flex items-center justify-center shrink-0">
                <i class="pi pi-file-import text-xl text-cyan-600" />
              </div>
              <div class="flex-1">
                <div class="font-semibold text-[var(--text-primary)] mb-1">
                  Importer CSV
                </div>
                <div class="text-sm text-[var(--text-secondary)]">
                  Importer des transactions depuis un fichier
                </div>
              </div>
              <i class="pi pi-chevron-right text-[var(--text-secondary)]" />
            </button>

            <button
              class="flex items-center gap-4 p-4 rounded-xl bg-gradient-to-br from-emerald-500/10 to-emerald-600/5 border-2 border-emerald-500/20 text-left transition-all hover:border-emerald-500/40 hover:shadow-lg active:scale-[0.98]"
              @click="emit('export-csv'); emit('update:modelValue', false)"
            >
              <div class="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center shrink-0">
                <i class="pi pi-file-export text-xl text-emerald-600" />
              </div>
              <div class="flex-1">
                <div class="font-semibold text-[var(--text-primary)] mb-1">
                  Exporter CSV
                </div>
                <div class="text-sm text-[var(--text-secondary)]">
                  Télécharger vos transactions en CSV
                </div>
              </div>
              <i class="pi pi-chevron-right text-[var(--text-secondary)]" />
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
  backdrop-filter: blur(0);
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
