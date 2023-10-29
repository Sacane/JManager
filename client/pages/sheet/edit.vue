
<script setup lang="ts">

const route = useRoute()
const {dateFromString} = useDate()

definePageMeta({
  layout: 'sidebar-layout',
})

const data = reactive({
  id: route.query.id as string,
  label: route.query.label as string,
  income: parseFloat(route.query.income as string),
  expenses: parseFloat(route.query.expenses as string),
  date: dateFromString(route.query.date as string),
  amount: 0,
  selectedMode: "expenses",
  accountAmount: parseFloat(route.query.accountAmount as string),
  accountId: parseInt(route.query.accountID as string)
})

const values = reactive({
  integerPart: '0',
  decimalPart: '0'
})

const {editSheet} = useSheets()

onMounted(() => {
  data.amount = (data.income === 0) ? data.expenses : data.income
  data.selectedMode = (data.income === 0) ? 'expenses' : 'income'
})

const onEdit = () => {
  const amount = `${values.integerPart}.${values.decimalPart} €`;
  editSheet({
    id: parseInt(data.id),
    label: data.label,
    expenses: data.selectedMode === 'expenses' ? amount : '0 €',
    income: data.selectedMode === 'income' ? amount : '0 €',
    date: data.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    accountAmount: `${data.accountAmount} €`
  }, data.accountId).then(_ => {

    navigateTo({
      path: '/account/' + data.accountId,
      query: {
        labelAccount: route.query.accountLabel,
        id: data.accountId,
        amount: route.query.currentAccountAmount
      }
    })
    
  }).catch(e => {
    
  })
}

</script>


<template>
    <div wfull hfull flex items-center>
    
    <div class="form-container" flex-col mb5>
      <PFieldset :legend="`Modifier la transaction`">
        <div>
          <label for="label" mt5px>Libelle</label>
          <PInputText placeholder="Ex: Achat d'une chaise" v-model="data.label" id="label"/>
        </div>
        <div mt5px>
          <label for="selectionType">Selectionner le type de transaction</label>
          <div id="selectionType" flex-row flex-gap3 mt5px>
            <div flex-row>
              <PRadioButton v-model="data.selectedMode" inputId="selection1" value="expenses"/>
              <label for="selection1" ml-2>Dépense</label>
            </div>
            <div flex-row>
              <PRadioButton v-model="data.selectedMode" inputId="selection2" value="income"/>
              <label for="selection2" ml-2>Recette</label>
            </div>
          </div>
        </div>
        <div id="labelAmount">
          <label for="amount">Selectionner le montant de la transaction (en €)</label>
          <div class="flex-row space-x-2 mt5px" id="amount">
            <PInputText placeholder="Partie entière" v-model="values.integerPart" />
            <div>.</div>
            <PInputText placeholder="Partie décimal" v-model="values.decimalPart" maxlength="2"/>
          </div>
        </div>
        <div mt5px>
          <label for="calendar">Indiquer la date de la transaction</label>
          <PCalendar placeholder="Date" v-model="data.date" date-format="dd-mm-yy" id="calendar"/>
        </div>
        <div mt5px>
          <PButton @click="onEdit" label="Ajouter une nouvelle transaction"/>
        </div>
      </PFieldset>
      
    </div>
  </div>
</template>