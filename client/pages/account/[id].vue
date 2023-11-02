<script setup lang="ts">
import { SheetAverageDTO, SheetDTO } from '../../types/index';
import { useConfirm } from "primevue/useconfirm";
definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()

const {translate, monthFromNumber} = useDate()

const {findById} = useAccounts()
const {findByDate, deleteSheet} = useSheets()
const date = new Date()
const data = reactive({
  year: date.getFullYear(),
  month: monthFromNumber(new Date().getMonth() + 1) as string,
  labelAccount: route.query.labelAccount as string,
  isRangeSelected: false,
  currentSheets: [] as SheetDTO[],
  currentAccountId: '',
  accountAmount: '',
  dateYear: new Date(),
  dateMonth: translate(monthFromNumber(new Date().getMonth() + 1) as string)
})

function retrieveSheets() {
  console.log('Trying to get sheets')
  findByDate(data.month, data.year, data.labelAccount)
  .then((value: SheetAverageDTO) => {
    actualSheets.value = value.sheets.map(sheet => {
      return {
      ...sheet,
      expensesRepresentation: (sheet.expenses != '') ? `${sheet.expenses}` : '/',
      incomeRepresenttation: (sheet.income != '') ? `${sheet.income}` : '/',
      date: sheet.date,
      accountAmount: sheet.accountAmount
      }
    })
  })
}


const initAccount = () => {
  findById(parseFloat(route.params.id as string))
  .then((account) => {
    data.accountAmount = account.amount
    data.labelAccount = account.labelAccount as string
    data.currentAccountId = route.params.id as string
    
  })
  retrieveSheets()
}


onMounted(() => {
  data.month = monthFromNumber(new Date().getMonth() + 1) as string
  initAccount()
})

const jtoast = useJToast()


function gotoTransaction() {
  navigateTo({
    path: '/sheet/persist',
    query: {
      id: data.currentAccountId,
      label: data.labelAccount,
      amount: data.accountAmount
    }
  })
}

const selectedSheets = ref<SheetDTO[]>([])
const actualSheets = ref()


const confirmDelete = async () => {
  deleteSheet(parseInt(data.currentAccountId), selectedSheets.value.map(sheet => sheet.id))
  .then(() => initAccount())
  .finally(() =>{
    findById(parseInt(data.currentAccountId)).then(account => {
      data.accountAmount = account.amount
    }).finally(() => jtoast.success('La suppression de la transaction a été correctement effectué'))
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
    path: '/sheet/edit',
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

const onYearChange = () => {
  data.year = data.dateYear.getFullYear()
  retrieveSheets()
}


</script>


<template>
  <PConfirmDialog></PConfirmDialog>
  <div class="w-full h-full flex flex-col container-all">
    <div p-8  bg-white class="form-container" mt2px>
      <div flex-row justify-between>
        <h2 class="text-2xl font-bold mb-4">Les transactions sur le compte {{ data.labelAccount }}</h2>
        <h2 class="text-2xl font-bold mb-4">Solde du compte : {{ data.accountAmount }}</h2>

      </div>
      <PDataTable :value="actualSheets" scrollable scrollHeight="450px" table-style="min-width: 50rem" v-model:selection="selectedSheets" @row-click="onEditPage">
        <template #header>
          <div style="text-align: left" class="w35%">
            <div class="pl10px flex flex-row hauto justify-around">

              <MonthPicker v-model="data.month" @update:model-value="retrieveSheets()" />

              <div class="w40% h10%">
                <label 
                for="yearPicker" 
                class="block text-sm font-medium text-gray-700"
                style="font-family: Arial, sans-serif;">
                Sélectionnez une année :
                </label>
                <PCalendar v-model="data.dateYear"  view="year" dateFormat="yy" @date-select="onYearChange" id="yearPicker"/>
              </div>
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
