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
  <div class="card rounded-lg shadow-lg bg-white p-6 mb-8 text-center" @click="click">
    <h2 class="text-2xl mb-4">
      {{ title }}
    </h2>
    <p class="text-center">
      {{ description }}
    </p>
  </div>
</template>

<style scoped>
.card {
  transition: transform 0.3s ease;
}
.create-container{
  &:hover{
    cursor: pointer;
  }
}

.card:hover {
  transform: translateY(-5px);
}
</style>
