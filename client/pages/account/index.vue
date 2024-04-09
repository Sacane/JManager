<script setup lang="ts">
import useAccounts from '../../composables/useAccounts'
import {AccountDTO} from '../../types/index';
import useJToast from "~/composables/useJToast";

definePageMeta({
  layout: 'sidebar-layout',
})

// toast

const {success, error} = useJToast()

//
const {fetch, deleteAccount, createAccount} = useAccounts()
const isAccountFilled = reactive({ ok: false })
const toAdd = () => {
  navigateTo('/account/persist')
}


const data = ref()


onMounted(async () => {
  await fetch().then((accountArray) => {
    console.log(accountArray)
    format(accountArray)
    isAccountFilled.ok = data.value.length > 0
    console.log(data.value)
  })
})

function format(accounts: Array<AccountDTO>) {
  data.value = accounts.map(account => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount}`,
    };
  });
}

function onRowClick(event: any) {
  navigateTo({
    path: '/account/' + event.data.id,
    query: {
      labelAccount: event.data.labelAccount,
      amount: event.data.amount
    }
  })
}

const applyEdit = () => {
  if(row.value === undefined) {
    error('Il faut sélectionner un compte pour le modifier')
    return
  }
  navigateTo({
    path: '/account/edit',
    query: {
      id: row.value?.id,
      labelAccount: row.value?.labelAccount,
      amount: row.value?.amount
    }
  })
}

const applyDelete = () => {
  if(row.value === undefined) {
    error('Il faut sélectionner un compte pour le supprimer')
    return
  }
  deleteAccount(row.value?.id as number)
  .finally(() => {
    fetch().then(accountArray => format(accountArray))
    .finally(() => {
      success('Le compte a bien été supprimé')
    })
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
    decimalPart: ''
  }
})

const toAccount = async () => {
  const { label, amount } = newAccount;
  createAccount(label, `${amount.integerPart}.${amount.decimalPart} €`)
    .then(() => {
      fetch().then(accountArray => format(accountArray)).finally(() => {
        isAddAccountDialogOpen.value = false;
        success('Votre nouveau compte a bien été créé');
      });
    })
    .catch((e: Error) => {
      error(e.message)
    });
};

</script>

<template>
  <div class="w-full h-full flex items-center mt-10">
    <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px container">
      <h2 class="info-text">
        Cliquez sur un compte pour visualiser ses transactions
      </h2>
      <PDataTable :value="data" table-style="min-width: 50rem" @row-click="onRowClick" v-model:selection="row">
        <template #header>
          <div class="flex flex-row hauto pl10px">
            <PButton class="button" w-auto b mr2 label="Modifier le compte" icon="pi pi-file-edit" @click="applyEdit"/>
            <PButton w-auto b mr2 label="Supprimer le compte" icon="pi pi-trash" severity="danger" @click="applyDelete"/>
          </div>
        </template>
        <PColumn selectionMode="single" style="width: 3rem" :exportable="false" v-model="actionSelection"></PColumn>
        <PColumn field="labelAccount" header="Libellé du compte" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="amount" header="Montant actuel" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
      </PDataTable>
    </div>
    <div v-else class="text-center justify-center align-center">
      <div class="mb-4">
        <p class="text-xl font-semibold text-gray-600">Vous n'avez pas encore de compte enregistré.</p>
      </div>
      <div class="mb-4">
        <p class="text-lg text-gray-500">Commencez par ajouter un compte pour gérer vos finances.</p>
      </div>
    </div>
    <div class="w-full mt-2 flex items-center" >
      <PButton label="Ajouter un nouveau compte" class="bg-purple w-250px" @click="isAddAccountDialogOpen = true"/>
    </div>
  </div>
  <PDialog v-model:visible="isAddAccountDialogOpen" modal header="Ajouter un nouveau compte" :style="{width: '25rem'}">

    <div class="mt-6">
      <label for="label" class="block text-sm font-medium text-gray-700">Libellé du compte</label>
      <PInputText v-model="newAccount.label" id="label" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50"/>

      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div class="flex-row" id="labelAmount">
        <PInputText placeholder="Partie entière" v-model="newAccount.amount.integerPart" class="w-1/2"/>
        <PInputText placeholder="Partie décimale" v-model="newAccount.amount.decimalPart" maxlength="2" class="w-1/2"/>
      </div>

      <PButton label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="toAccount" />
      <PButton label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="isAddAccountDialogOpen = false"/>
    </div>

  </PDialog>
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
