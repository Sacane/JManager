
<script setup lang="ts">
import useJToast from '../composables/useJToast';
definePageMeta({
    layout: 'sidebar-layout'
})

const {success} = useJToast()

const {createAccount, fetch} = useAccounts()

const newAccount = reactive({
  label: '',
  amount: 0.0
})

const amount = reactive({
  integerPart: 0,
  decimalPart: 0
})


const toAccount = async () => {
  const integer = parseFloat(`${amount.integerPart}`)
  await createAccount(newAccount.label, integer + parseFloat(`0.${amount.decimalPart}`))
  .then(async () => {
    await fetch()
    await success('Le compte a bien été créé')
  }).finally(async () => await navigateTo('/account'))
}
</script>

<template>
  <div class="flex items-center mt3">
    <form class="p-8 mt-5 bg-white form-container">
      <h1 class="text-4xl font-bold c-#7F52FF text-center">Créer un nouveau compte</h1>
      <PFieldset>
        <label for="label">Libelle du compte</label>
        <PInputText v-model="newAccount.label" id="label"/>
        <label for="labelAmount">Montant</label>
        <div class="flex-row space-x-2" id="labelAmount">
          <PInputNumber placeholder="Partie entière" v-model="amount.integerPart" />
          <div class="p-2">.</div>
          <PInputNumber placeholder="Partie décimal" v-model="amount.decimalPart" maxlength="2"/>
        </div>
        <PButton label="Créer" class="mt-3 bg-#7F52FF" @click="toAccount" />
      </PFieldset>
    </form>
  </div>
</template>

<style scoped>

.form-container{
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
}
</style>