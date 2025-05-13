<script setup lang="ts">
// switch to preferred locale on load
import 'primeicons/primeicons.css'

const router = useRouter()
const switchLocalePath = useSwitchLocalePath()
const locale = useLocale()

tryOnBeforeMount(() => {
  router.replace(switchLocalePath(locale.value))
})
</script>

<template>
  <div font="text">
    <NuxtLoadingIndicator />
    <NuxtLayout>
      <Toast />
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<style lang="scss">
:root {
  --primary: #822acc;
  --primary-2: #722aaa;
  --primary-3: #511e78;
  --primary-4: #391555;
  --yellow: #e0d824;
  --pink: #f13c69;
  --grey: #b1aeae;
  --grey-2: #a7a7a7;
  --text-color: var(--grey-1);
  --primary-background: #8863eb;
  --primary-light: #66ccff;
  --secondary: #27282C

}
body, html {
  margin: 0;
  padding: 0;
}
.btn-primary {
  background-color: var(--primary-2);
  border-color: var(--primary-2);
  &:hover {
    background-color: var(--primary-3);
  }
}
pi {
  width: 100px;
}
.bg-primary{
  background-color: var(--primary);
}
p {
  color: var(--grey-2);
}
</style>
