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
    integerPart: '',
    decimalPart: '',
  },
})

async function toAccount() {
  const { label, amount } = newAccount
  createAccount(label, `${amount.integerPart}.${amount.decimalPart} €`)
    .then(() => {
      fetch().then((accountArray) => {
        format(accountArray)
        isAccountFilled.value = accountArray.length > 0
      }).finally(() => {
        isAddAccountDialogOpen.value = false
      })
    })
}
</script>

<template>
  <div v-if="isAccountFilled" class="p20px container">
    <h2 class="info-text">
      Double cliquez sur un compte pour visualiser ses transactions
    </h2>
    <DataTable v-model:selection="row" :value="data" selection-mode="single" data-key="id" table-style="min-width: 50rem" @row-dblclick="onRowClick">
      <template #header>
        <div class="flex flex-row h-auto pl10px">
          <!-- <Button class="b mr2 w-350px h-50px" label="Modifier le compte" icon="pi pi-file-edit" @click="applyEdit" /> -->
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
  <Button label="Ajouter un nouveau compte" class="w-250px h-50px" @click="isAddAccountDialogOpen = true" />
  <Dialog v-model:visible="isAddAccountDialogOpen" class="bg-grey" modal header="Ajouter un nouveau compte">
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé du compte</label>
        <InputText id="label" v-model="newAccount.label" type="text" autocomplete="off" />
      </div>

      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="newAccount.amount.integerPart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="newAccount.amount.decimalPart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="toAccount" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isAddAccountDialogOpen = false" />
    </div>
  </Dialog>
</template>

<style scoped lang="scss">
.container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  .info-text{
    text-align: center;
    font-size: 18px;
    color: #555;
    margin-bottom: 20px;
  }
}
</style>
