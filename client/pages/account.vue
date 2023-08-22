<script setup lang="ts">
import useAccounts from '../composables/useAccounts'

definePageMeta({
  layout: 'sidebar-layout',
})

const { accounts, fetch } = useAccounts()
const isAccountFilled = reactive({ ok: false })

const toAdd = () => {
  navigateTo('/addAccount')
}

onMounted(async () => {
  await fetch()
  isAccountFilled.ok = accounts.value.length > 0
})
</script>

<template>
  <div w-full h-full flex>
    <div v-if="isAccountFilled.ok" class=" bg-#f0f0f0 p20px">
      <PDataTable :value="accounts" table-style="min-width: 50rem">
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
