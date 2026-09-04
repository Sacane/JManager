<script lang="ts" setup>
const { isDark, toggle } = useDark()

const themeToggleLabel = computed(() =>
  isDark.value ? 'Passer au thème clair' : 'Passer au thème sombre',
)
</script>

<template>
  <header class="p-5 w-full flex items-center flex-row justify-end gap-2 absolute">
    <LocaleSwitch />
    <button
      class="icon-btn text-5"
      type="button"
      :aria-label="themeToggleLabel"
      @click="toggle()"
    >
      <div i="tabler-sun dark:tabler-moon" />
    </button>
  </header>
</template>
