<script setup lang="ts">
const emits = defineEmits(['update:modelValue'])

const date = useDate()

const selectedMonth = ref('')
const currentMonth = ref(date.translate(date.monthFromNumber(new Date().getMonth() + 1) as string))

watchEffect(() => {
  emits('update:modelValue', selectedMonth.value)
})

function onMonthChange() {
  emits('update:modelValue', selectedMonth.value)
}
</script>

<template>
  <div class="flex flex-col items-start">
    <label for="monthSelect" class="mb-2 text-sm font-medium text-gray-700">Choisissez un mois :</label>
    <div class="relative w-full">
      <select id="monthSelect" v-model="selectedMonth" class="block w-full p-3 rounded-md bg-white border border-gray-300 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 focus:outline-none transition duration-300 ease-in-out" @change="onMonthChange">
        <option :value="currentMonth" disabled hidden>
          Choisissez un mois
        </option>
        <option v-for="(month, index) in date.months" :key="index" :value="month" class="py-2">
          {{ month }}
        </option>
      </select>
      <i class="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400 fal fa-angle-down" />
    </div>
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
