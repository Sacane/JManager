<script setup lang="ts">

import {useToast} from 'primevue/usetoast'
definePageMeta({
  layout: 'sidebar-layout',
})

const toast = useToast()
const months = [
  'JANUARY', 
  'FEBRUARY',
  'MARCH',
  'APRIL',
  'MAY',
  'JUNE',
  'JULY',
  'AUGUST',
  'SEPTEMBER',
  'OCTOBER',
  'NOVEMBER',
  'DECEMBER'
] 

function translate(month: string): string{
  let result = ''
  switch(month){
    case 'JANUARY': result = 'JANVIER'; break;
    case 'FEBRUARY': result = 'FEVRIER'; break;
    case 'MARCH': result = 'MARS'; break;
    case 'APRIL': result = 'AVRIL'; break;
    case 'MAY': result = 'MAI'; break;
    case 'JUNE': result = 'JUIN'; break;
    case 'JULY': result = 'JUILLET'; break;
    case 'AUGUST': result = 'AOUT'; break;
    case 'SEPTEMBER': result = 'SEPTEMBRE'; break;
    case 'OCTOBER': result = 'OCTOBRE'; break;
    case 'NOVEMBER': result = 'NOVEMBRE'; break;
    case 'DECEMBER': result = 'DECEMBRE'; break;
  }
  return result;
}

const years = reactive([2023])

const addYear = () => {
  years.push(years.at(years.length - 1)!! + 1)
}

const {accounts, fetch} = useAccounts()
const {findByDate} = useSheets()
const dateSelected = reactive({
  year: 0,
  month: '',
  labelAccount: '',
  isRangeSelected: false
})

const isSelectionOk = () => dateSelected.year !== 0 && dateSelected.month !== '' && dateSelected.labelAccount !== ''

function selectYear(year: number) {
  dateSelected.year = year
  console.log(year)
}

function selectMonth(month: string) {
  dateSelected.month = month
  console.log(dateSelected.month)
}

async function retrieveSheets() {
  console.log('test!!')
  if(!isSelectionOk()) {
    console.log(dateSelected, isSelectionOk)
    return
  }
  console.log('test??')
  await findByDate(dateSelected.month, dateSelected.year, dateSelected.labelAccount)
}

onMounted(async () => {
  await fetch();
})


</script>


<template>
  <div class="w-full h-full flex flex-col container-all">
    <div class="p10px flex flex-row header-btn">
      <PButton v-for="year in years" :key="year" w-auto b mr2 @click="selectYear(year)" id="month" class="year-btn">
      {{ year }}
      </PButton>
      <PButton ml5px @click="addYear">  
        +
      </PButton>
    </div>
    <div class="pl10px flex flex-row buttons justify-between">
      <PButton v-for="(month, key) in months" :key="key" w-auto b @click="selectMonth(month.toString())" class="btn-small">
      {{ translate(month) }}
      </PButton>
    </div>
    <div p-8 mt-5 bg-white class="form-container">
      <PFieldset>
        <div mt2>
          <div class="flex flex-row" v-for="account of accounts.map(p => p.labelAccount)">
            <PRadioButton :value="account" v-model="dateSelected.labelAccount"/>
            <label ml-2>{{ account }}</label>
          </div>
        </div>
        <div items-start mt4 >
          <PButton @click="retrieveSheets">Selectionner</PButton>
        </div>
      </PFieldset>
    </div>
  </div>
</template>

<style scoped lang="scss">
.container-all{
  .year-btn {
    padding: 5px 7px;
  }
  .form-container{
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
  }
  .btn-small{
    font-size: 12px;
    padding: 6px 12px;
  }
}
.buttons {
  align-content: space-evenly;
}




</style>