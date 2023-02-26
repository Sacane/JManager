<script setup lang="ts">

</script>

<template>
  <div class="flex-column border-r-1 h-100% mr3 decoration-none">
    <NuxtLink to="/" class="hover:bg-gray-200 px-4 py-2 rounded-md" active-class="bg-#7F52FF color-#fff">
      Accueil
    </NuxtLink>
    <NuxtLink to="/account" class="hover:bg-gray-200 px-4 py-2 rounded-md">
      Mes comptes
    </NuxtLink>
    <NuxtLink to="/home" class="hover:bg-gray-200 px-4 py-2 rounded-md" active-class="bg-#7F52FF">
      Mes transactions
    </NuxtLink>
  </div>
</template>
