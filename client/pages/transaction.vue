<script setup lang="ts">
definePageMeta({
  layout: 'sidebar-layout',
})

const months = [
    'JANVIER',
    'FEVRIER',
    'MARS',
    'AVRIL',
    'MAI',
    'JUIN',
    'JUILLET',
    'AOUT',
    'SEPTEMBRE',
    'OCTOBRE',
    'NOVEMBRE',
    'DECEMBRE'
]

const representationMonth = new Map<string, string>()

representationMonth.set('JANVIER', 'JANUARY');
representationMonth.set('FEVRIER', 'JANUARY');
representationMonth.set('MARS', 'JANUARY');
representationMonth.set('AVRIL', 'JANUARY');
representationMonth.set('MAI', 'JANUARY');
representationMonth.set('JUIN', 'JANUARY');
representationMonth.set('JUILLET', 'JANUARY');
representationMonth.set('AOUT', 'JANUARY');
representationMonth.set('SEPTEMBRE', 'JANUARY');
representationMonth.set('JANVIER', 'JANUARY');
representationMonth.set('JANVIER', 'JANUARY');

const years = reactive([2023])

const addYear = () => {
  years.push(years.at(years.length - 1) ?? + 1)
}

</script>


<template>
  <div class="w-full h-full flex flex-col">
    <div class="bg-red p10px flex flex-row overflow-hidden">
      <PButton v-for="year in years" :key="year" w-auto b mr2>
      {{ year }}
      </PButton>
      <PButton ml5px @click="addYear">  
        +
      </PButton>
    </div>
    <div class="bg-blue p10px flex flex-row flex-justify-between">
      <PButton v-for="month in months" :key="month" w-auto b>
      {{ month }}
      </PButton>
    </div>
  </div>
</template>