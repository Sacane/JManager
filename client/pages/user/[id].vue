<script setup lang="ts">
const route = useRoute()

definePageMeta({
  layout: 'sidebar-layout',
})
</script>

<template>
  <h1>User {{ route.params.id }}</h1>
</template>
