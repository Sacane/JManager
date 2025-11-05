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
  --primary-light: #9d5dd9;
  --primary-lighter: #b88ee0;
  --yellow: #e0d824;
  --pink: #f13c69;
  --bg-primary: #f5f7fa;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f8fafc;
  --bg-gradient-from: #f5f7fa;
  --bg-gradient-to: #e8ecf1;
  --card-bg: #ffffff;
  --card-hover-bg: #faf5ff;
  --card-border: rgba(130, 42, 204, 0.1);
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-tertiary: #94a3b8;
  --text-muted: #cbd5e1;
  --border-color: #e2e8f0;
  --border-light: #f1f5f9;
  --shadow-sm: rgba(0, 0, 0, 0.05);
  --shadow-md: rgba(0, 0, 0, 0.08);
  --shadow-lg: rgba(0, 0, 0, 0.12);
  --shadow-purple: rgba(130, 42, 204, 0.08);
  --grey: #b1aeae;
  --grey-2: #64748b;
  --text-color: var(--primary-3);
  --primary-background: #8863eb;
  --secondary: #27282C;
}

.dark {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-tertiary: #334155;
  --bg-gradient-from: #0f172a;
  --bg-gradient-to: #1e293b;
  --card-bg: #1e293b;
  --card-hover-bg: #2d3b52;
  --card-border: rgba(130, 42, 204, 0.2);
  --text-primary: #f1f5f9;
  --text-secondary: #cbd5e1;
  --text-tertiary: #94a3b8;
  --text-muted: #64748b;
  --border-color: #334155;
  --border-light: #475569;
  --shadow-sm: rgba(0, 0, 0, 0.3);
  --shadow-md: rgba(0, 0, 0, 0.4);
  --shadow-lg: rgba(0, 0, 0, 0.5);
  --shadow-purple: rgba(130, 42, 204, 0.3);

  --grey-2: #94a3b8;
}

body, html {
  margin: 0;
  padding: 0;
  font-family: 'Poppins', ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
  background-color: var(--bg-primary);
  color: var(--text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

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
