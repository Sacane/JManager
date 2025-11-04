<script setup lang="ts">
const props = defineProps<{
  title: string
  amount: string
  isPreview?: boolean
}>()

const amountClass = computed(() => {
  return Number.parseFloat(props.amount) >= 0 ? 'text-green-500' : 'text-pink-500'
})
</script>

<template>
  <div class="bg-white rounded-lg shadow-sm text-center w-62.5" :class="{ 'bg-gray-100': isPreview }">
    <h3 class="m-0 text-xl text-purple-600">
      {{ title }}
    </h3>
    <p class="mt-2 mb-0 text-2xl font-bold" :class="amountClass">
      {{ amount }} €
    </p>
  </div>
</template>
