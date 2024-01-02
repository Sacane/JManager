<script setup lang="ts">
const date = useDate()

const selectedMonth = ref('')
const emits = defineEmits(['update:modelValue']);
const currentMonth = ref(date.translate(date.monthFromNumber(new Date().getMonth() + 1) as string))

watchEffect(() => {
  emits('update:modelValue', selectedMonth.value);
})

function onMonthChange() {
  emits('update:modelValue', selectedMonth.value);
}
</script>

<template>
  <div>
    <label for="fruit">Choisissez un mois :</label>
    <select v-model="selectedMonth" id="fruit" @change="onMonthChange">
      <option :value="currentMonth" disabled hidden>Choisissez un mois</option>
      <option v-for="(fruit, index) in date.months" :key="index" :value="fruit">{{ fruit }}</option>
    </select>
  </div>
</template>

<style scoped>
.month-picker {
  font-family: Arial, sans-serif;

}

option {
  padding: 0.5rem 1rem;
  background-color: #ffffff;
  color: #333333;
  transition: background-color 0.3s ease, color 0.3s ease;

  &:hover {
    background-color: #f0f0f0;
    color: #000000;
  }
}
  </style>
