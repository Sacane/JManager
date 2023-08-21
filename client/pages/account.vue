import useAccounts from '../composables/useAccounts';
import { AccountDTO } from '../types/index';
<script setup lang="ts">
definePageMeta({
  layout: 'sidebar-layout',
})

const {accounts, fetch} = useAccounts()
const isAccountFilled = ref(false)

const toAdd = () => {
  navigateTo('/addAccount')
}

onMounted(async () => {
  await fetch();
  isAccountFilled.value = true
  console.log(accounts.value)
})

const formatCurrency = (value: string) => {
  const formattedValue = parseFloat(value).toFixed(2);
  return `${formattedValue} €`;
};

</script>

<template>
  <div w-full h-full flex>
    <div v-if="isAccountFilled">
      <PDataTable :value="accounts" table-style="min-width: 50rem">
        <PColumn field="labelAccount" header="Label" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }" />
        <PColumn field="amount" header="Amount" :body-style="{ textAlign: 'center' }" :header-style="{ textAlign: 'center' }"/>
      </PDataTable>
    </div>
    <div v-else min-w-500px>
      Ce profil n'a pas encore de compte enregistrés
    </div>
    <div class="w-200px" mt-2>
      <PButton label="Ajouter un nouveau compte" @click="toAdd" class="bg-purple"/>
    </div>
    
  </div>

</template>
