<script setup lang="ts">
const route = useRoute()
const { dateFromString } = useDate()

definePageMeta({
  layout: 'sidebar-layout',
})

const data = reactive({
  id: route.query.id as string,
  label: route.query.label as string,
  income: Number.parseFloat(route.query.income as string),
  expenses: Number.parseFloat(route.query.expenses as string),
  date: dateFromString(route.query.date as string),
  amount: 0,
  selectedMode: 'expenses',
  accountAmount: Number.parseFloat(route.query.accountAmount as string),
  accountId: Number.parseInt(route.query.accountID as string),
})

const values = reactive({
  integerPart: 0,
  decimalPart: 0,
})

const { editSheet } = useSheets()

onMounted(() => {
  data.amount = (data.income === 0) ? data.expenses : data.income
  data.selectedMode = (data.income === 0) ? 'expenses' : 'income'
})

function onEdit() {
  const amount = `${values.integerPart}.${values.decimalPart} €`
  const sheet = {
    id: Number.parseInt(data.id),
    label: data.label,
    expenses: data.selectedMode === 'expenses' ? amount : '0 €',
    income: data.selectedMode === 'income' ? amount : '0 €',
    date: data.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    accountAmount: `${data.accountAmount} €`,
  }
  console.log(sheet)
  editSheet(sheet, data.accountId).then((_) => {
    navigateTo({
      path: `/account/${data.accountId}`,
      query: {
        labelAccount: route.query.accountLabel,
        id: data.accountId,
        amount: route.query.currentAccountAmount,
      },
    })
  }).catch()
}
</script>

<template>
  <div class="h-full flex justify-center items-center">
    <PFieldset legend="Modifier la transaction" class="form-container w-50% p-3">
      <div>
        <label for="label">Libelle</label>
        <PInputText id="label" v-model="data.label" placeholder="Ex: Achat d'une chaise" />
      </div>
      <div mt5px>
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="flex-row flex-gap3 mt5px">
          <div flex-row>
            <PRadioButton v-model="data.selectedMode" input-id="selection1" value="expenses" />
            <label for="selection1" ml-2>Dépense</label>
          </div>
          <div flex-row>
            <PRadioButton v-model="data.selectedMode" input-id="selection2" value="income" />
            <label for="selection2" ml-2>Recette</label>
          </div>
        </div>
      </div>
      <div id="labelAmount">
        <label for="amount">Selectionner le montant de la transaction (en €)</label>
        <div id="amount" class="flex-row space-x-2 mt5px">
          <PInputNumber v-model="values.integerPart" placeholder="Partie entière" />
          <div>.</div>
          <PInputNumber v-model="values.decimalPart" placeholder="Partie décimal" maxlength="2" />
        </div>
      </div>
      <div mt5px>
        <label for="calendar">Indiquer la date de la transaction</label>
        <PCalendar id="calendar" v-model="data.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <div mt5px>
        <PButton label="Ajouter une nouvelle transaction" @click="onEdit" />
      </div>
    </PFieldset>
  </div>
</template>
