<script setup lang="ts">
const props = defineProps<{
  title: string
  amount: number
  isPreview?: boolean
}>()

const amountClass = computed(() => {
  return Number.parseFloat(props.amount) >= 0 ? 'positive' : 'negative'
})
</script>

<template>
  <div class="balance-card" :class="{ preview: isPreview }">
    <h3>{{ title }}</h3>
    <p :class="amountClass">
      {{ amount }} €
    </p>
  </div>
</template>

<style scoped>
.balance-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  text-align: center;
  width: 250px;
}

.balance-card h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--primary);
}

.balance-card p {
  margin: 8px 0 0;
  font-size: 1.5rem;
  font-weight: bold;
}

.balance-card.preview {
  background-color: #f0f0f0;
}
.positive {
  color: #1acf38;
}

.negative {
  color: #F44336;
}
</style>
