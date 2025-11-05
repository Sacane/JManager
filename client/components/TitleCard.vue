<script setup lang="ts">
export interface CardProps {
  title: string
  description: string
  onClick?: () => void
}

const { title, description, onClick } = defineProps<CardProps>()

function click() {
  if (onClick) onClick()
}
</script>

<template>
  <div class="rounded-lg shadow-lg bg-white p-6 mb-8 text-center transition-transform duration-300 ease hover:-translate-y-1.5 cursor-pointer" @click="click">
    <h2 class="text-2xl mb-4">
      {{ title }}
    </h2>
    <p class="text-center">
      {{ description }}
    </p>
  </div>
</template>
