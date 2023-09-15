<script setup lang="ts">
import { ref } from 'vue';

// Liste des mois
const months = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];

// Variable pour stocker le mois sélectionné
const selectedMonth = ref('');

// Initialisez la variable avec le premier mois
selectedMonth.value = months[0];
</script>

<template>
  <div class="relative month-picker w40% h10%">
    <label for="monthDropdown" class="block text-sm font-medium text-gray-700">Sélectionnez un mois :</label>
    <select
      v-model="selectedMonth"
      id="monthDropdown"
      class="mt-1 block w-full py-2 px-3 border border-gray-300 bg-white rounded-md focus:border-indigo-300 sm:text-sm"
    >
      <option
        v-for="(month, index) in months"
        :key="index"
        :value="month"
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