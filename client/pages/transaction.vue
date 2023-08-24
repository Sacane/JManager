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
  currentSheets: [] as SheetDTO[],
  currentAccountId: '',
  accountAmount: 0.0
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
  dateSelected.currentAccountId = route.currentRoute.value.query.id as string
  dateSelected.accountAmount = parseFloat(route.currentRoute.value.query.amount as string)
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

function gotoTransaction() {
  navigateTo({
    name: 'newSheet',
    query: {
      id: dateSelected.currentAccountId,
      label: dateSelected.labelAccount,
      amount: dateSelected.accountAmount
    }
  })
}

</script>


<template>
  <div class="w-full h-full flex flex-col container-all">
    <div class="pl10px pb2px flex flex-row header-btn">
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
      <h2 class="text-2xl font-bold mb-4 text-center">Mes transactions sur le compte {{ dateSelected.labelAccount }}</h2>
      <PDataTable v-if="dateSelected.currentSheets.length > 0" :value="dateSelected.currentSheets.map(sheet => {
        return {
          ...sheet,
          expenses: `${sheet.expenses.toFixed(2)}€`,
          income: `${sheet.income.toFixed(2)}€`,
          date: formatDateToFrench(sheet.date),
          accountAmount: `${sheet.accountAmount.toFixed(2)}€`
        }
      })" table-style="min-width: 50rem">
        <PColumn field="date" header="Date" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="label" header="Libellé" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="expenses" header="Dépenses" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="income" header="Recettes" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="accountAmount" header="Solde" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
      </PDataTable>
    </div>
    <div w200px pt5px>
      <PButton w-auto @click="gotoTransaction">Ajouter une transaction</PButton>
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