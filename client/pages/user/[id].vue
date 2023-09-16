
<script setup lang="ts">
import useJToast from '../../composables/useJToast';
const route = useRoute()
const toast = useJToast()

definePageMeta({
  layout: 'sidebar-layout',
})
</script>

<template>
  <h1>User {{ route.params.id }}</h1>
</template>
