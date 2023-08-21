<script setup lang="ts">
definePageMeta({
    layout: 'sidebar-layout'
})

const newAccount = reactive({
  label: '',
  amount: 0.0
})

const amount = reactive({
  integerPart: 0,
  decimalPart: 0
})

const toAccount = () => {
  navigateTo('/account')
}
</script>

<template>
  <div class="flex items-center mt3">
    
    <form class="b-3 p-8 mt-5 b-rounded-2 b-#7F52FF">
      <h1 class="text-4xl font-bold c-#7F52FF text-center">Créer un nouveau compte</h1>
      <PFieldset>
        <label for="label">Libelle du compte</label>
        <PInputText v-model="newAccount.label" id="label"/>
        <label for="labelAmount">Montant</label>
        <div class="flex-row space-x-2" id="labelAmount">
          <PInputNumber placeholder="Partie entière" v-model="amount.integerPart" />
          <div class="p-2">.</div>
          <PInputNumber placeholder="Partie décimal" v-model="amount.decimalPart"/>
        </div>
        <PButton label="Créer" class="mt-3 bg-#7F52FF" @click="toAccount" />
      </PFieldset>
    </form>
  </div>
</template>