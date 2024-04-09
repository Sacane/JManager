<script setup lang="ts">
import useSheet from '../../composables/useSheets'

definePageMeta({
  layout: 'sidebar-layout',
})

const route = useRoute()
const { saveSheet } = useSheet()
const { fetch, findById } = useAccounts()
const account = ref<AccountDTO>()
const values = reactive({
  accountId: route.query.id,
  accountLabel: route.query.label as string,
  accountAmount: route.query.amount as string,
  amount: 0.0,
  selectedMode: 'expenses',
  sheetLabel: '',
  date: new Date(),
  integerPart: '0',
  decimalPart: '0',
})

onMounted(() => {
  findById(Number.parseInt(values.accountId as string)).then(res => account.value = res)
})

async function onConfirm() {
  if ((values.integerPart === '0' && values.decimalPart === '0') || values.sheetLabel === '') {
    return
  }
  const amount = `${values.integerPart}.${values.decimalPart} €`;
  await saveSheet(values.accountLabel, {
    id: 0,
    label: values.sheetLabel,
    expenses: (values.selectedMode === 'expenses') ? amount : '0 €',
    income: (values.selectedMode === 'income') ? amount : '0 €',
    date: values.date.toLocaleDateString('fr-FR').replace(/\//g, '-'),
    accountAmount: `${account.value?.amount}`,
  }).then((sheet: SheetDTO) => {
    fetch()
    navigateTo({
      path:`/account/${values.accountId}`,
      query: {
        id: values.accountId,
        labelAccount: values.accountLabel,
        amount,
      },
    })
  })
}
</script>
<template>
  <div class="wfull hfull flex items-center justify-center content ">
    <div class="flex-col w60% mb5 form-container">
      <Fieldset :legend="`Ajouter une nouvelle transaction pour le compte ${values.accountLabel }`">
        <div>
          <label for="label" mt5px>Libelle</label>
          <InputText placeholder="Ex: Achat d'une chaise" v-model="values.sheetLabel" id="label"/>
        </div>
        <div mt5px>
          <label for="selectionType">Selectionner le type de transaction</label>
          <div id="selectionType" class ="flex-row flex-gap3 mt5px">
            <div flex-row>
              <RadioButton v-model="values.selectedMode" input-id="selection1" value="expenses"/>
              <label for="selection1"  class="ml-2">Dépense</label>
            </div>
            <div class="flex-row">
              <RadioButton v-model="values.selectedMode" input-id="selection2" value="income"/>
              <label for="selection2" class="ml-2">Recette</label>
            </div>
          </div>
        </div>
        <div id="labelAmount">
          <label for="amount">Selectionner le montant de la transaction (en €)</label>
          <div class="flex-row space-x-2 mt5px" id="amount">
            <InputText placeholder="Partie entière" v-model="values.integerPart" />
            <div>.</div>
            <InputText placeholder="Partie décimal" v-model="values.decimalPart" maxlength="2"/>
          </div>
        </div>
        <div mt5px>
          <label for="calendar">Date de la transaction</label>
          <Calendar placeholder="Date" v-model="values.date" date-format="dd-mm-yy" id="calendar"/>
        </div>
        <div mt5px>
          <Button label="Ajouter une nouvelle transaction" @click="onConfirm" />
        </div>
      </Fieldset>

    </div>
  </div>
</template>

<style scoped lang="scss">
.content{
  .form-container{
    background-color: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    justify-content: center;
  }
}
</style>
