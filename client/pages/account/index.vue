<script setup lang="ts">
import useAccounts from '../../composables/useAccounts'

definePageMeta({
  layout: 'sidebar-layout',
})

// toast
//
const { fetch, deleteAccount, createAccount } = useAccounts()
const isAccountFilled = reactive({ ok: false })

const data = ref()

onMounted(async () => {
  await fetch().then((accountArray) => {
    format(accountArray)
    isAccountFilled.ok = data.value.length > 0
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
    query: {
      labelAccount: event.data.labelAccount,
      amount: event.data.amount,
    },
  })
}

function applyEdit() {
  if(row.value === undefined) {
    return
  }
  navigateTo({
    path: '/account/edit',
    query: {
      id: row.value?.id,
      labelAccount: row.value?.labelAccount,
      amount: row.value?.amount,
    },
  })
}

function applyDelete() {
  if(row.value === undefined) {
    return
  }
  deleteAccount(row.value?.id as number)
  .finally(() => {
    fetch().then(accountArray => format(accountArray))
  })
}

const row = ref<AccountDTO | undefined>(undefined)
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
      fetch().then(accountArray => format(accountArray)).finally(() => {
        isAddAccountDialogOpen.value = false
      })
    })
}
</script>

<template>
  <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px container">
    <h2 class="info-text">
      Cliquez sur un compte pour visualiser ses transactions
    </h2>
    <DataTable v-model:selection="row" :value="data" @row-click="onRowClick">
      <template #header>
        <div class="flex flex-row hauto pl10px">
          <Button class="b mr2 bg-purple w-350px h-50px" label="Modifier le compte" icon="pi pi-file-edit" @click="applyEdit" />
          <Button class="b mr2 bg-purple w-350px h-50px" label="Supprimer le compte" icon="pi pi-trash" severity="danger" @click="applyDelete" />
        </div>
      </template>
      <Column selection-mode="single" :exportable="false" v-model="actionSelection" />
      <Column field="labelAccount" header="Libellé du compte" />
      <Column field="amount" header="Montant actuel" />
    </DataTable>
  </div>
  <div v-else class="text-center justify-center align-center">
    <div class="mb-4">
      <p class="text-xl font-semibold text-gray-600">Vous n'avez pas encore de compte enregistré.</p>
    </div>
    <div class="mb-4">
      <p class="text-lg text-gray-500">Commencez par ajouter un compte pour gérer vos finances.</p>
    </div>
  </div>
  <Button label="Ajouter un nouveau compte" class="bg-purple w-250px h-50px" @click="isAddAccountDialogOpen = true" />
  <Dialog v-model:visible="isAddAccountDialogOpen" modal header="Ajouter un nouveau compte" :style="{ width: '25rem' }">
    <div class="mt-6">
      <label for="label" class="block text-sm font-medium text-gray-700">Libellé du compte</label>
      <InputText v-model="newAccount.label" id="label" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50"/>

      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div class="flex-row" id="labelAmount">
        <InputText placeholder="Partie entière" v-model="newAccount.amount.integerPart" class="w-1/2"/>
        <InputText placeholder="Partie décimale" v-model="newAccount.amount.decimalPart" maxlength="2" class="w-1/2"/>
      </div>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="toAccount" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isAddAccountDialogOpen = false"/>
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
