<script setup lang="ts">
import { useConfirm } from 'primevue/useconfirm'
import useSheet from '~/composables/useSheets'

definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()

const { translate, monthFromNumber } = useDate()

const { findById } = useAccounts()
const { findByDate, deleteSheet } = useSheets()
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
  dateMonth: translate(monthFromNumber(new Date().getMonth() + 1) as string),
})
const actualSheets = ref()

function asDisplayableTransaction(transaction: SheetDTO): any {
  return {
    ...transaction,
    expensesRepresentation: (transaction.expenses !== '') ? `${transaction.expenses}` : '/',
    incomeRepresenttation: (transaction.income !== '') ? `${transaction.income}` : '/',
    date: transaction.date,
    accountAmount: transaction.accountAmount,
  }
}
function retrieveSheets() {
  findByDate(data.month, data.year, data.labelAccount)
    .then((value: SheetAverageDTO) => {
      actualSheets.value = value.sheets.map((sheet: SheetDTO) => {
        return asDisplayableTransaction(sheet)
      })
    })
}

function initAccount() {
  findById(Number.parseFloat(route.params?.id as string))
    .then((account: AccountDTO) => {
      data.accountAmount = account.amount
      data.labelAccount = account.labelAccount as string
      data.currentAccountId = route.params?.id as string
    })
  retrieveSheets()
}

onMounted(() => {
  data.month = monthFromNumber(new Date().getMonth() + 1) as string
  initAccount()
})

function gotoTransaction() {
  navigateTo({
    path: '/sheet/persist',
    query: {
      id: data.currentAccountId,
      label: data.labelAccount,
      amount: data.accountAmount,
    },
  })
}

async function confirmDelete() {
  deleteSheet(Number.parseInt(data.currentAccountId), selectedSheets.value.map(sheet => sheet.id))
    .then(() => initAccount())
    .finally(() => {
      findById(Number.parseInt(data.currentAccountId)).then((account) => {
        data.accountAmount = account.amount
      })
    })
}

const confirm = useConfirm()

function confirmDeleteButton() {
  if (selectedSheets.value.length === 0) {
    return
  }
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ces éléments ?',
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    accept: () => confirmDelete(),
  })
}

function onEditPage(event: any) {
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
      currentAccountAmount: data.accountAmount,
    },
  })
}

function onYearChange() {
  data.year = data.dateYear.getFullYear()
  retrieveSheets()
}

function isSelected(event: any) {
  return true
}

const uDate = useDate()

// dialog

const isNewTransactionDialogOpen = ref<boolean>(false)

// transaction persistance

const { saveSheet } = useSheet()

const values = reactive({
  accountId: data.currentAccountId,
  accountLabel: data.labelAccount,
  accountAmount: data.accountAmount as string,
  amount: 0.0,
  selectedMode: 'expenses',
  sheetLabel: '',
  date: new Date(),
  integerPart: '0',
  decimalPart: '0',
})
async function onConfirm() {
  if ((values.integerPart === '0' && values.decimalPart === '0') || values.sheetLabel === '') {
    return
  }
  const amount = `${values.integerPart}.${values.decimalPart} €`
  await saveSheet(values.accountLabel, {
    id: 0,
    label: values.sheetLabel,
    expenses: (values.selectedMode === 'expenses') ? amount : '0 €',
    income: (values.selectedMode === 'income') ? amount : '0 €',
    date: values.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    accountAmount: `${data.accountAmount}`,
  }).then((sheet: SheetDTO) => {
    // actualSheets.value.push(asDisplayableTransaction(sheet))
    initAccount()
  }).finally(() => isNewTransactionDialogOpen.value = false)
}
</script>

<template>
  <ConfirmDialog />
  <div class="w-full h-full flex flex-row container-all">
    <div class="mr10px form-container p-8  bg-white mt2px">
      <div class="flex-row justify-between">
        <h2 class="text-2xl font-bold mb-4 info-text">
          Les transactions sur le compte {{ data.labelAccount }}
        </h2>
        <h2 class="text-2xl mb-4">
          Solde du compte : {{ data.accountAmount }}
        </h2>
      </div>
      <DataTable v-model:selection="selectedSheets" :value="actualSheets" scrollable scroll-height="450px" selection-mode="multiple" table-style="min-width: 60rem" @row-dblclick="onEditPage">
        <template #header>
          <div style="text-align: left" class="w-full">
            <div class="flex flex-row hauto justify-between">
              <Dropdown v-model="data.month" :options="uDate.months" placeholder="Selectionner un mois" class="w-full md:w-14rem" @change="retrieveSheets()" />
              <div class="w26% flex flex-row items-center">
                <div class="flex justify-center mr2">
                  <label
                    for="yearPicker"
                    class="block text-sm font-medium text-gray-700"
                    style="font-family: Arial, sans-serif;"
                  >
                    Sélectionnez une année :
                  </label>
                </div>
                <Calendar id="yearPicker" v-model="data.dateYear" class="h10 text-center" view="year" date-format="yy" @date-select="onYearChange" />
              </div>
            </div>
          </div>
        </template>
        <Column sortable field="date" header="Date" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <Column field="label" header="Libellé" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <Column field="expensesRepresentation" header="Dépenses" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <Column field="incomeRepresenttation" header="Recettes" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <Column field="accountAmount" header="Solde" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
      </DataTable>
    </div>
    <div class="pt5px flex flex-col gap-3 mr2">
      <Button icon="pi pi-plus" @click="isNewTransactionDialogOpen = true" />
      <Button icon="pi pi-trash" severity="danger" @click="confirmDeleteButton" />
    </div>
  </div>
  <Dialog v-model:visible="isNewTransactionDialogOpen" modal header="Ajouter une nouvelle transaction">
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
        <InputText id="label" v-model="values.sheetLabel" type="text" autocomplete="off" />
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="values.integerPart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="values.decimalPart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="values.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="onConfirm" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isNewTransactionDialogOpen = false" />
    </div>
  </Dialog>
</template>

<style scoped lang="scss">
.container-all{
  .year-btn {
    width: auto;
    height: 5%;
  }
  .form-container{
    background-color: white;
    width: 100%;
    border-radius: 8px;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }
  .buttons {
    margin-top: 15px;
    .btn-small{
      padding: 6px 12px;
      margin-right: 10px;
    }
  }

}

.info-text{
  text-align: center;
  color: #555;
  margin-bottom: 20px;
}

.selected-row{
  color: blue;
}
</style>
