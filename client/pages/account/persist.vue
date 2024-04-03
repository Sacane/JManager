<script setup lang="ts">
import useJToast from '../../composables/useJToast';
import useAccounts from '../../composables/useAccounts';

definePageMeta({
  layout: 'sidebar-layout'
});

const { success } = useJToast();
const { createAccount, fetch } = useAccounts();

const newAccount = reactive({
  label: '',
  amount: {
    integerPart: '',
    decimalPart: ''
  }
});

const toAccount = async () => {
  const { label, amount } = newAccount;
  createAccount(label, `${amount.integerPart}.${amount.decimalPart} €`)
    .then(() => {
      fetch();
      success('Le compte a bien été créé');
      navigateTo('/account');
    })
    .catch((e: Error) => {
      console.error(e);
    });
};
</script>

<template>
  <div class="flex items-center justify-center w-full h-full">
    <form class="p-8 mt-5 bg-white rounded-lg shadow-md w-45%">
      <h1 class="text-4xl font-bold text-center text-purple-600">Créer un nouveau compte</h1>
      <div class="mt-6">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé du compte</label>
        <PInputText v-model="newAccount.label" id="label" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50"/>

        <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
        <div class="flex-row" id="labelAmount">
          <PInputText placeholder="Partie entière" v-model="newAccount.amount.integerPart" class="w-1/2"/>
          <PInputText placeholder="Partie décimale" v-model="newAccount.amount.decimalPart" maxlength="2" class="w-1/2"/>
        </div>

        <PButton label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="toAccount" />
      </div>
    </form>
  </div>
</template>

<style scoped>
</style>
