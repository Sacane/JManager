<script setup lang="ts">
import useAccounts from '../composables/useAccounts'

definePageMeta({
  layout: 'sidebar-layout',
})
interface AccountFormatted{
  labelAccount: string,
  amount: string
}
const { accounts, fetch } = useAccounts()
const isAccountFilled = reactive({ ok: false })
const accountFormatted = ref<AccountFormatted[]>([])
const toAdd = () => {
  navigateTo('/addAccount')
}
onMounted(async () => {
  await fetch()
  isAccountFilled.ok = accounts.value.length > 0
  accountFormatted.value = accounts.value.map(account => {
    return {
      id: account.id,
      labelAccount: account.labelAccount,
      amount: `${account.amount} €`,
    };
  });
})

function onRowClick(event: any) {
  console.log(event.data.amount)
  navigateTo({
    path: '/transaction',
    query: {
      id: event.data.id,
      labelAccount: event.data.labelAccount
    }
  })
}
</script>

<template>
  <div w-full h-full flex>
    <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px container">
      <PDataTable :value="accountFormatted" table-style="min-width: 50rem" @row-click="navigateToTransaction()">
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