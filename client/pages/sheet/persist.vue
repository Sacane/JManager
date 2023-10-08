<script setup lang="ts">

import useSheet from '../../composables/useSheets';
import { SheetDTO } from '../../types/index';
import useJToast from '../../composables/useJToast';

definePageMeta({
  layout: 'sidebar-layout',     
})

const {success} = useJToast()
const route = useRoute()
const {saveSheet} = useSheet()
const {fetch} = useAccounts()

const values = reactive({
  accountId: route.query.id,
  accountLabel: route.query.label as string,
  accountAmount: route.query.amount as string,
  amount: 0.0,
  selectedMode: 'expenses',
  sheetLabel: '',
  date: new Date(),
  integerPart: 0.0,
  decimalPart: 0.0
})

const onConfirm = async () => {
  if((values.integerPart === 0 && values.decimalPart === 0) || values.sheetLabel === '') {
    return
  }
  const amount = parseFloat((parseFloat(`${values.integerPart}`) + parseFloat(`0.${values.decimalPart}`)).toFixed(2))
  console.log(amount)
  await saveSheet(values.accountLabel, {
    id: 0,
    label: values.sheetLabel,
    expenses: (values.selectedMode === 'expenses') ? amount : 0,
    income: (values.selectedMode === 'income') ? amount : 0,
    date: values.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    accountAmount: parseFloat(values.accountAmount)
  }).then((sheet: SheetDTO) => {
      fetch()
      success('La transaction a bien été ajouté')
      navigateTo({
      path:'/account/'+values.accountId,
      query: {
        id: values.accountId,
        labelAccount: values.accountLabel,
        amount: amount
      }
    })
  })
}
</script>


<template>
  <div class="wfull hfull flex items-center justify-center content ">
    <div class="flex-col w60% mb5 form-container">
      <PFieldset :legend="`Ajouter une nouvelle transaction pour le compte ${values.accountLabel }`">
        <div>
          <label for="label" mt5px>Libelle</label>
          <PInputText placeholder="Ex: Achat d'une chaise" v-model="values.sheetLabel" id="label"/>
        </div>
        <div mt5px>
          <label for="selectionType">Selectionner le type de transaction</label>
          <div id="selectionType" flex-row flex-gap3 mt5px>
            <div flex-row>
              <PRadioButton v-model="values.selectedMode" inputId="selection1" value="expenses"/>
              <label for="selection1" ml-2>Dépense</label>
            </div>
            <div flex-row>
              <PRadioButton v-model="values.selectedMode" inputId="selection2" value="income"/>
              <label for="selection2" ml-2>Recette</label>
            </div>
          </div>
        </div>
        <div id="labelAmount">
          <label for="amount">Selectionner le montant de la transaction (en €)</label>
          <div class="flex-row space-x-2 mt5px" id="amount">
            <PInputNumber placeholder="Partie entière" v-model="values.integerPart" />
            <div>.</div>
            <PInputNumber placeholder="Partie décimal" v-model="values.decimalPart" maxlength="2"/>
          </div>
        </div>
        <div mt5px>
          <label for="calendar">Date de la transaction</label>
          <PCalendar placeholder="Date" v-model="values.date" date-format="dd-mm-yy" id="calendar"/>
        </div>
        <div mt5px>
          <PButton @click="onConfirm" label="Ajouter une nouvelle transaction"/>
        </div>
      </PFieldset>
      
    </div>
  </div>
</template>

<style scoped lang="scss">


.content{

  .form-container{
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1); /* Ajoutez l'ombre ici */
    justify-content: center;
  }
}

</style>