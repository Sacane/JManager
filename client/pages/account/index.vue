<script setup lang="ts">
import useAccounts, { AccountFormatted } from '../../composables/useAccounts'
import { AccountDTO } from '../../types/index';
definePageMeta({
  layout: 'sidebar-layout',
})

// toast

const {success, error} = useJToast()

//

const {fetch, deleteAccount} = useAccounts()
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
  })
})

function format(accounts: Array<AccountDTO>) {
  data.value = accounts.map(account => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount} €`,
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


</script>

<template>
  <div w-full h-full flex items-center>
    <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px container">
      <p class="info-text">
        Cliquez sur un compte pour visualiser ses transactions
      </p>
      <PDataTable :value="data" table-style="min-width: 50rem" @row-click="onRowClick" v-model:selection="row">
        <template #header>
          <div class="flex flex-row hauto pl10px">
            <PButton w-auto b mr2 label="Modifier le compte" icon="pi pi-file-edit" @click="applyEdit"/>
            <PButton w-auto b mr2 label="Supprimer le compte" icon="pi pi-trash" severity="danger" @click="applyDelete"/>
          </div>
        </template>
        <PColumn selectionMode="single" style="width: 3rem" :exportable="false" v-model="actionSelection"></PColumn>
        <PColumn field="labelAccount" header="Libellé du compte" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="amount" header="Montant actuel" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
      </PDataTable>
    </div>
    <div v-else class="text-center">
      <div class="mb-4">
        <p class="text-xl font-semibold text-gray-600">Vous n'avez pas encore de compte enregistré.</p>
      </div>
      <div class="mb-4">
        <p class="text-lg text-gray-500">Commencez par ajouter un compte pour gérer vos finances.</p>
      </div>
    </div>
    <div class="w-full mt-2 flex items-center" >
      <PButton label="Ajouter un nouveau compte" class="bg-purple w-250px" @click="toAdd" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
  .info-text{
    text-align: center;
    font-size: 18px;
    color: #555;
    margin-bottom: 20px;
  }
}

</style>
