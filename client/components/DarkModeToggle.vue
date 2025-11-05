<script setup lang="ts">
const { isDark, toggle } = useDark()
</script>

<template>
  <button
    class="dark-mode-toggle"
    :aria-label="isDark ? 'Activer le mode clair' : 'Activer le mode sombre'"
    @click="toggle()"
  >
    <Transition name="fade" mode="out-in">
      <i v-if="isDark" key="sun" class="pi pi-sun" />
      <i v-else key="moon" class="pi pi-moon" />
    </Transition>
  </button>
</template>

<style scoped lang="scss">
.dark-mode-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1.1rem;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    border-color: rgba(255, 255, 255, 0.3);
    transform: scale(1.05);
  }

  &:active {
    transform: scale(0.95);
  }

  i {
    transition: all 0.3s ease;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: rotate(-90deg) scale(0.5);
}

.fade-leave-to {
  opacity: 0;
  transform: rotate(90deg) scale(0.5);
}
</style>
