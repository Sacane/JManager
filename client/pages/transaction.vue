<script setup lang="ts">
import { SheetAverageDTO, SheetDTO } from '../types/index';
import { useConfirm } from "primevue/useconfirm";
definePageMeta({
  layout: 'sidebar-layout',
})

const {months, translate, monthFromNumber} = useDate()

const years = reactive([2023, 2024, 2025])

const addYear = () => {
  years.push(years.at(years.length - 1)!! + 1)
}

const {accounts, fetch, findById} = useAccounts()
const {findByDate, deleteSheet} = useSheets()
const date = new Date()
const data = reactive({
  year: date.getFullYear(),
  month: monthFromNumber(date.getMonth() + 1) as string,
  labelAccount: '',
  isRangeSelected: false,
  currentSheets: [] as SheetDTO[],
  currentAccountId: '',
  accountAmount: 0.0
})

const isSelectionOk = () => data.year !== 0 && data.month !== '' && data.labelAccount !== ''

function selectYear(year: number) {
  data.year = year
  retrieveSheets()
}

function selectMonth(month: string) {
  data.month = month
  retrieveSheets()
}

function retrieveSheets() {
  if(!isSelectionOk()) {
    return
  }
  findByDate(data.month, data.year, data.labelAccount)
  .then((value: SheetAverageDTO) => data.currentSheets = value.sheets)
  .finally(() => loadSheets())
}

const route = useRoute()

const initAccount = () => {
  findById(parseFloat(route.query.id as string))
  .then((account) => {
    data.accountAmount = account.amount
    data.labelAccount = account.labelAccount as string
    data.currentAccountId = route.query.id as string
  }).finally(() => {
    retrieveSheets()
  })
}


onMounted(async () => {
  initAccount()
  
})

const jtoast = useJToast()


// Fonction pour formater la date en format français (jour/mois/année)
function formatDateToFrench(numbers: number[]) {
  return new Date(numbers[0], numbers[1] - 1, numbers[2]).toLocaleDateString('fr-FR').replace(/\//g, '-');
}

function gotoTransaction() {
  navigateTo({
    name: 'newSheet',
    query: {
      id: data.currentAccountId,
      label: data.labelAccount,
      amount: data.accountAmount
    }
  })
}

const selectedSheets = ref<SheetDTO[]>([])
const actualSheets = ref()

const loadSheets = () => {
  fetch()
  actualSheets.value = data.currentSheets.map(sheet => {
    return {
      ...sheet,
      expensesRepresentation: (sheet.expenses > 0.0) ? `${sheet.expenses.toFixed(2)}€` : '/',
      incomeRepresenttation: (sheet.income > 0.0) ? `${sheet.income.toFixed(2)}€` : '/',
      date: formatDateToFrench(sheet.date),
      accountAmount: `${sheet.accountAmount.toFixed(2)}€`
    }
  })
}

const confirmDelete = async () => {
  deleteSheet(parseInt(data.currentAccountId), selectedSheets.value.map(sheet => sheet.id))
  .then(() => retrieveSheets())
  .finally(() =>{
    fetch().then(accs => {
      data.accountAmount = accounts.value.findLast(value => value.id === parseInt(data.currentAccountId))?.amount as number
      jtoast.success('La suppression de la transaction a été correctement effectué')
    })
  })
}

const confirm = useConfirm()

const confirmDeleteButton = () => {
  if(selectedSheets.value.length === 0){
    return;
  }
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ces éléments ?',
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    accept: () => confirmDelete()
  })
}

const onEditPage = (event: any) => {
  console.log(data.accountAmount)
  navigateTo({
    name: 'sheetEdit',
    query: {
      id: event.data.id,
      label: event.data.label,
      expenses: event.data.expenses,
      income: event.data.income,
      date: event.data.date,
      accountAmount: event.data.accountAmount,
      accountID: data.currentAccountId,
      accountLabel: data.labelAccount,
      currentAccountAmount: data.accountAmount
    }
  })
}

</script>


<template>
  <PConfirmDialog></PConfirmDialog>
  <div class="w-full h-full flex flex-col container-all">
    <div p-8  bg-white class="form-container" mt2px>
      <div flex-row justify-between>
        <h2 class="text-2xl font-bold mb-4">Les transactions sur le compte {{ data.labelAccount }}</h2>
        <h2 class="text-2xl font-bold mb-4">Solde du compte : {{ data.accountAmount }} €</h2>

      </div>
      <PDataTable :value="actualSheets" scrollable scrollHeight="450px" table-style="min-width: 50rem" v-model:selection="selectedSheets" @row-click="onEditPage">
        <template #header>
          <div style="text-align: left">
            <div class="pl10px flex flex-row hauto">
              <PButton v-for="year in years"
              :class="{ 'bg-gray-300': data.year === year }"
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
              :class="{ 'bg-gray-300': data.month === month }"
              >
              {{ translate(month) }}
              </PButton>
            </div>
          </div>
        </template>
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
      <PButton @click="confirmDeleteButton" label="Supprimer" icon="pi pi-trash" severity="danger"/>
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
    margin-top: 15px;
    .btn-small{
      padding: 6px 12px;
      margin-right: 10px;
    }
  }

}





</style>
