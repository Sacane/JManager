<script setup lang="ts">
import {useRouter} from 'vue-router';
import { SheetAverageDTO, SheetDTO } from '../types/index';
definePageMeta({
  layout: 'sidebar-layout',
})

const {months, translate, monthFromNumber} = useDate()

const years = reactive([2023, 2024, 2025])

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

const selectedSheets = ref()

const confirmDelete = () => {
  console.log(selectedSheets.value)
}

</script>


<template>
  <div class="w-full h-full flex flex-col container-all">
    <div class="pl10px flex flex-row hauto">
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
      <PButton ml5px @click="addYear" class="year-btn">  
        +
      </PButton>
    </div>
    <div class="pl10px flex flex-row buttons ">
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
    <div p-8  bg-white class="form-container" mt2px>
      <div flex-row justify-between>
        <h2 class="text-2xl font-bold mb-4">Mes transactions sur le compte {{ dateSelected.labelAccount }}</h2>
        <h2 class="text-2xl font-bold mb-4">Solde du compte : {{ dateSelected.accountAmount }} €</h2>

      </div>
      <PDataTable v-if="dateSelected.currentSheets.length > 0" :value="dateSelected.currentSheets.map(sheet => {
        return {
          ...sheet,
          expensesRepresentation: (sheet.expenses > 0.0) ? `${sheet.expenses.toFixed(2)}€` : '/',
          incomeRepresenttation: (sheet.income > 0.0) ? `${sheet.income.toFixed(2)}€` : '/',
          date: formatDateToFrench(sheet.date),
          accountAmount: `${sheet.accountAmount.toFixed(2)}€`
        }
      })" table-style="min-width: 50rem" v-model:selection="selectedSheets">
        <PColumn selectionMode="multiple" style="width: 3rem" :exportable="false"></PColumn>
        <PColumn sortable field="date" header="Date" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="label" header="Libellé" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="expensesRepresentation" header="Dépenses" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }"/>
        <PColumn field="incomeRepresenttation" header="Recettes" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="accountAmount" header="Solde" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
      </PDataTable>
    </div>
    <div  pt5px flex-row justify-between>
      <PButton w-auto @click="gotoTransaction">Ajouter une transaction</PButton>
      <PButton @click="confirmDelete" label="Supprimer" icon="pi pi-trash" severity="danger"/>
    </div>
  </div>
</template>

<style scoped lang="scss">
.container-all{
  .year-btn {
    width: auto;
    height: 5%;
  }
  .form-container{
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
  }
  .buttons {
    margin-top: -15px;
    .btn-small{
      padding: 6px 12px;
      margin-right: 10px;
    }
  }

}





</style>