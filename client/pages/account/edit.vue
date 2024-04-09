<script setup lang="ts">
import { useRouter } from 'vue-router'

definePageMeta({
  layout: 'sidebar-layout',
})

const { updateAccount } = useAccounts()

const route = useRouter()
const data = reactive({
  id: 0,
  label: '',
  amount: '',
  integerPart: '',
  decimalPart: '',
  acc: undefined as AccountDTO | undefined,
})

onMounted(() => {
  const amountValue = (route.currentRoute.value.query.amount as string).split('.')
  data.id = Number.parseFloat(route.currentRoute.value.query.id as string)
  data.label = route.currentRoute.value.query.labelAccount as string
  data.integerPart = amountValue[0]
  data.decimalPart = amountValue[1].split(' ')[0]
})

function onEditClick() {
  updateAccount({
    id: data.id,
    labelAccount: data.label,
    amount: `${data.integerPart}.${data.decimalPart} €`,
    sheets: [],
  }, async () => {
    navigateTo('/account')
  })
}
</script>

<template>
  <div class="flex items-center mt3">
    <Toast/>
    <form class="p-8 mt-5 bg-white form-container">
      <h1 class="text-4xl font-bold c-#7F52FF text-center">Mettre à jour le compte</h1>
      <Fieldset>
        <label for="label">Libelle du compte</label>
        <InputText v-model="data.label" id="label"/>
        <label for="labelAmount">Montant</label>
        <div class="flex-row space-x-2" id="labelAmount">
          <InputText placeholder="Partie entière" v-model="data.integerPart" />
          <div class="p-2">.</div>
          <InputText placeholder="Partie décimal" v-model="data.decimalPart" maxlength="2"/>
        </div>
        <Button label="Mettre à jour" class="mt-3 bg-#7F52FF" @click="onEditClick"/>
      </Fieldset>
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
