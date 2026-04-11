<script setup lang="ts">
import authMiddleware from '~/middleware/auth'

const route = useRoute()

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})
</script>

<template>
  <h1>User {{ route.params.id }}</h1>
</template>
