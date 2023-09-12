<script setup lang="ts">
import {useRouter} from 'vue-router';
import { AccountDTO } from '../types/index';
import useJToast from '../composables/useJToast';


definePageMeta({
  layout: 'sidebar-layout',
})

const jtoast = useJToast()

const {updateAccount, fetch} = useAccounts()

const route = useRouter()
const data = reactive({
  id: 0,
  label: '',
  amount: 0,
  integerPart: 0,
  decimalPart: 0,
  acc: undefined as AccountDTO | undefined
})

onMounted(() => {
  const value = Math.floor(parseFloat(route.currentRoute.value.query.amount as string))
  data.id = parseFloat(route.currentRoute.value.query.id as string)
  data.label = route.currentRoute.value.query.labelAccount as string
  data.amount = value
  data.integerPart = Math.floor(value)
  data.decimalPart = data.amount - data.integerPart
})

function onEditClick() {
  const amount = parseFloat((parseFloat(`${data.integerPart}`) + parseFloat(`0.${data.decimalPart}`)).toFixed(2))
  updateAccount({
    id: data.id,
    labelAccount: data.label,
    amount: amount,
    sheets: []
  }, async (sheet) => {
    jtoast.success('Le compte a bien été édité')
    navigateTo('/account')
  })
  
} 
</script>


<template>
  <div class="flex items-center mt3">
    <PToast/>
    <form class="p-8 mt-5 bg-white form-container">
      <h1 class="text-4xl font-bold c-#7F52FF text-center">Mettre à jour le compte</h1>
      <PFieldset>
        <label for="label">Libelle du compte</label>
        <PInputText v-model="data.label" id="label"/>
        <label for="labelAmount">Montant</label>
        <div class="flex-row space-x-2" id="labelAmount">
          <PInputNumber placeholder="Partie entière" v-model="data.integerPart" />
          <div class="p-2">.</div>
          <PInputNumber placeholder="Partie décimal" v-model="data.decimalPart" maxlength="2"/>
        </div>
        <PButton label="Mettre à jour" class="mt-3 bg-#7F52FF" @click="onEditClick"/>
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