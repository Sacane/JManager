<script setup lang="ts">
import useAccounts, { AccountFormatted } from '../composables/useAccounts'
import { AccountDTO } from '../types/index';
definePageMeta({
  layout: 'sidebar-layout',
})

const { accounts, fetch, deleteAccount} = useAccounts()
const isAccountFilled = reactive({ ok: false })
const toAdd = () => {
  navigateTo('/addAccount')
}

const data = reactive({
  render: [] as AccountFormatted[]
})

onMounted(async () => {
  await fetch().then(accountArray => {
    format(accountArray)
    console.log(accounts)
  })
  isAccountFilled.ok = accounts.value.length > 0
})

function format(accounts: Array<AccountDTO>) {
  data.render = accounts.map(account => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount} €`,
    };
  });
}

function onRowClick(event: any) {
  console.log(event.data.amount)
  navigateTo({
    name: 'transaction',
    query: {
      id: event.data.id,
      labelAccount: event.data.labelAccount,
      amount: event.data.amount
    }
  })
}

const applyEdit = () => {
  navigateTo({
    name: 'updateAccount',
    query: {
      id: row.value?.id,
      labelAccount: row.value?.labelAccount,
      amount: row.value?.amount
    }
  })
}

const applyDelete = () => {
  deleteAccount(row.value?.id as number)
  .finally(() => {
    fetch().then(accountArray => format(accountArray))
  })
}

const row = ref<AccountDTO | undefined>(undefined)
const actionSelection = ref<AccountDTO | undefined>(undefined)
</script>

<template>
  <div w-full h-full flex items-center>
    <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px container">
      <PDataTable :value="data.render" table-style="min-width: 50rem" @row-click="onRowClick" v-model:selection="row">
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
    <div v-else min-w-500px>
      Ce profil n'a pas encore de compte enregistrés
    </div>
    <div class="w-full mt-2 flex items-center" >
      <PButton label="Ajouter un nouveau compte" class="bg-purple w-250px" @click="toAdd" />
    </div>
  </div>
</template>

<style scoped>
.container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
}

</style>