<script setup lang="ts">
import {useRouter} from 'vue-router';
import { SheetAverageDTO, SheetDTO } from '../types/index';
definePageMeta({
  layout: 'sidebar-layout',
})

const {months, translate, monthFromNumber} = useDate()

const years = reactive([2023])

const addYear = () => {
  years.push(years.at(years.length - 1)!! + 1)
}

const {accounts, fetch} = useAccounts()
const {findByDate} = useSheets()
const date = new Date()
const dateSelected = reactive({
  year: date.getFullYear(),
  month: monthFromNumber(date.getMonth() + 1) as string,
  labelAccount: '',
  isRangeSelected: false,
  currentSheets: [] as SheetDTO[]
})

const isSelectionOk = () => dateSelected.year !== 0 && dateSelected.month !== '' && dateSelected.labelAccount !== ''

function selectYear(year: number) {
  dateSelected.year = year
  retrieveSheets()
}

function selectMonth(month: string) {
  dateSelected.month = month
  retrieveSheets()
}

function retrieveSheets() {
  if(!isSelectionOk()) {
    return
  }
  findByDate(dateSelected.month, dateSelected.year, dateSelected.labelAccount)
  .then((value: SheetAverageDTO) => dateSelected.currentSheets = value.sheets)
  .finally(() => console.log(dateSelected.currentSheets.map(p => p.date)))
}

const route = useRouter()

const initAccount = () => {
  dateSelected.labelAccount = route.currentRoute.value.query.labelAccount as string
  console.log(dateSelected.labelAccount)
}

onMounted(async () => {
  await initAccount()
  await fetch();
  await retrieveSheets()
})



// Fonction pour formater la date en format français (jour/mois/année)
function formatDateToFrench(numbers: number[]) {
  return new Date(numbers[0], numbers[1], numbers[2]).toLocaleDateString('fr-FR').replace(/\//g, '-');
}


</script>


<template>
  <div class="w-full h-full flex flex-col container-all">
    <div class="p10px flex flex-row header-btn">
      <PButton v-for="year in years" 
      :class="{ 'bg-gray-300': dateSelected.year === year }"
      :key="year" 
      w-auto b mr2 
      @click="selectYear(year)" 
      id="month" 
      class="year-btn"
      >
      {{ year }}
      </PButton>
      <PButton ml5px @click="addYear">  
        +
      </PButton>
    </div>
    <div class="pl10px flex flex-row buttons justify-between">
      <PButton v-for="(month, key) in months" 
      :key="key" 
      w-auto b 
      @click="selectMonth(month.toString())" 
      class="btn-small"
      :class="{ 'bg-gray-300': dateSelected.month === month }"
      >
      {{ translate(month) }}
      </PButton>
    </div>
    <div p-8 mt-5 bg-white class="form-container">
      <PFieldset>
        <!--<div mt2>
          <div class="flex flex-row" v-for="account of accounts.map(p => p.labelAccount)">
            <PRadioButton :value="account" v-model="dateSelected.labelAccount"/>
            <label ml-2>{{ account }}</label>
          </div>
        </div>
        <div items-start mt4 >
          <PButton @click="retrieveSheets">Selectionner</PButton>
        </div>-->
        <PDataTable v-if="dateSelected.currentSheets.length > 0" :value="dateSelected.currentSheets.map(current => {
          return {
            ...current,
            date: formatDateToFrench(current.date)
          }
        })" table-style="min-width: 50rem">
          <PColumn field="date" header="Date" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
          <PColumn field="label" header="Libellé de la transaction" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
          <PColumn field="amount" header="Montant actuel" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        </PDataTable>
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