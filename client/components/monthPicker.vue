<script setup lang="ts">
import { ref, defineProps, defineEmits} from 'vue';

const date = useDate()

// Variable pour stocker le mois sélectionné
const selectedMonth = ref('')
const { modelValue } = defineProps(['modelValue']);
const emits = defineEmits(['update:modelValue']);
const currentMonth = date.translate(date.monthFromNumber(new Date().getMonth() + 1) as string)

watchEffect(() => {
  emits('update:modelValue', selectedMonth.value);
});


</script>

<template>
  <div class="relative month-picker w40% h10%">
    <label for="monthDropdown" class="block text-sm font-medium text-gray-700">Sélectionnez un mois :</label>
    <select
      v-model="selectedMonth"
      id="monthDropdown"
      class="mt-1 block w-full py-2 px-3 border border-gray-300 bg-white rounded-md focus:border-indigo-300 sm:text-sm"
    >
          <!-- Utilisez une option par défaut pour le placeholder -->
        <option
        :value="''"
        :selected="selectedMonth === ''"
        disabled
        hidden
      >
      {{ currentMonth }}
      </option>
      <option
        v-for="(month, index) in date.months.map(s => date.translate(s))"
        :key="index"
        :value="date.months[index]"
        class="dropdown-option"
      >
        {{ month }}
      </option>
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