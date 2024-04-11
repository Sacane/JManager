<script setup lang="ts">
// switch to preferred locale on load
import 'primeicons/primeicons.css'
/*
const router = useRouter()
const switchLocalePath = useSwitchLocalePath()
const locale = useLocale()

tryOnBeforeMount(() => {
  router.replace(switchLocalePath(locale.value))
}) */
</script>

<template>
  <div font="text">
    <NuxtLoadingIndicator />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<style lang="scss">
:root{
  --primary: #7F52FF;
}
body, html {
  margin: 0;
  padding: 0;
}
.button {
  background-color: var(--primary);
}
pi {
  width: 100px;
}
.bg-primary{
  background-color: var(--primary)
}
</style>
