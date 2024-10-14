<script setup lang="ts">
import useAccounts from '../../composables/useAccounts'

definePageMeta({
  layout: 'sidebar-layout',
})
const row = ref<AccountDTO | undefined>(undefined)
// toast
//
const { fetch, deleteAccount, createAccount } = useAccounts()
const isAccountFilled = ref<boolean>(false)

const data = ref()

onMounted(async () => {
  await fetch().then((accountArray) => {
    format(accountArray)
    isAccountFilled.value = accountArray.length > 0
  })
})

function format(accounts: Array<AccountDTO>) {
  data.value = accounts.map((account: AccountDTO) => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount}`,
    }
  })
}

function onRowClick(event: any) {
  navigateTo({
    path: `/account/${event.data.id}`,
  })
}

function applyDelete() {
  if (row.value === undefined) {
    return
  }
  deleteAccount(row.value?.id as number)
    .finally(() => {
      fetch().then((accountArray) => {
        format(accountArray)
        isAccountFilled.value = accountArray.length > 0
      })
    })
}

const actionSelection = ref<AccountDTO | undefined>(undefined)

// ==================== Dialog management ==================== //

const isAddAccountDialogOpen = ref<boolean>(false)
const newAccount = reactive({
  label: '',
  amount: {
    integerPart: '0',
    decimalPart: '0',
  },
})

function handleAccountCreation(account) {
  createAccount(account.label, `${account.integerpart}.${account.decimalpart} €`)
    .then(() => {
      fetch().then((accountArray) => {
        format(accountArray)
        isAccountFilled.value = accountArray.length > 0
      }).finally(() => {
        isAddAccountDialogOpen.value = false
      })
    })
}
function cancel() {
  isAddAccountDialogOpen.value = false
}
function openAccountDialog() {
  console.log(`openAccountDialog ${isAddAccountDialogOpen.value}`)
  isAddAccountDialogOpen.value = true
  console.log(`openAccountDialog ${isAddAccountDialogOpen.value}`)
}
</script>

<template>
  <div class="w-full h-full flex flex-col gap-5 items-center">
    <div v-if="isAccountFilled" class="p20px container">
      <h2 class="info-text">
        Double cliquez sur un compte pour visualiser ses transactions
      </h2>
      <DataTable v-model:selection="row" :value="data" selection-mode="single" data-key="id" table-style="min-width: 50rem" @row-dblclick="onRowClick">
        <template #header>
          <div class="flex flex-row h-auto pl10px">
            <Button class="b mr2 w-350px h-50px" label="Supprimer le compte" icon="pi pi-trash" severity="danger" @click="applyDelete" />
          </div>
        </template>
        <Column v-model="actionSelection" selection-mode="single" :exportable="false" />
        <Column field="labelAccount" header="Libellé du compte" />
        <Column field="amount" header="Montant actuel" />
      </DataTable>
    </div>
    <div v-else class="text-center justify-center align-center">
      <div class="mb-4">
        <p class="text-xl font-semibold text-gray-600">
          Vous n'avez pas encore de compte enregistré.
        </p>
      </div>
      <div class="mb-4">
        <p class="text-lg text-gray-500">
          Commencez par ajouter un compte pour gérer vos finances.
        </p>
      </div>
    </div>
    <Button label="Ajouter un nouveau compte" class="w-250px h-50px align-self-center" @click="openAccountDialog" />
    <AccountBookingDialog
      :label="newAccount.label"
      :integerpart="newAccount.amount.integerPart"
      :decimalpart="newAccount.amount.decimalPart"
      :visible="isAddAccountDialogOpen"
      @create-account="handleAccountCreation"
      @cancel="cancel"
    />
  </div>
</template>

<style scoped lang="scss">
.container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  .info-text{
    text-align: center;
    color: #555;
    margin-bottom: 20px;
    font-weight: 900;
    font-size: 2rem;
  }
}
</style>
