<script setup lang="ts">
import 'primeicons/primeicons.css'

const router = useRouter()
const switchLocalePath = useSwitchLocalePath()
const locale = useLocale()

tryOnBeforeMount(() => {
  router.replace(switchLocalePath(locale.value))
})
</script>

<template>
  <div class="w-full h-full">
    <NuxtLoadingIndicator />
    <NuxtLayout>
      <AppToast />
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<style lang="scss">
/* CSS variables and base typography are in assets/css/variables.css and assets/css/typography.css */

/* --- Layout reset ------------------------------------------------ */
body,
html {
  margin: 0;
  padding: 0;
  overflow: hidden;
  overscroll-behavior: none;
}

/* --- PrimeVue button overrides ----------------------------------- */
.btn-primary {
  background-color: var(--primary-2);
  border-color: var(--primary-2);

  &:hover {
    background-color: var(--primary-3);
  }
}

.p-button.btn-primary {
  background-color: var(--primary-2);
  border-color: var(--primary-2);
}

.p-button.btn-primary:hover {
  background-color: var(--primary-3);
  border-color: var(--primary-3);
}

/* --- Utilities --------------------------------------------------- */
.bg-primary {
  background-color: var(--primary);
}
</style>
