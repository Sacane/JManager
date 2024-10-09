<script setup lang="ts">
export interface TransactionCreationProps {
  label: string
}

// const { label } = defineProps<TransactionCreationProps>()
</script>

<template>
  <!--
  <Dialog modal :header="label">
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
        <InputText id="label" v-model="values.sheetLabel" type="text" autocomplete="off" />
      </div>
      <div class="mt5 flex flex-col gap-3">
        <label for="selectionType">Selectionner le type de transaction</label>
        <div id="selectionType" class="w-full flex flex-row flex-gap5 mt5px">
          <div>
            <RadioButton v-model="values.isIncome" input-id="selection1" value="false" />
            <label for="selection1">Dépense</label>
          </div>
          <div>
            <RadioButton v-model="values.isIncome" input-id="selection2" value="true" />
            <label for="selection2">Recette</label>
          </div>
        </div>
      </div>
      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="values.integerPart" type="number" placeholder="Partie entière" class="" />
        <InputText v-model="values.decimalPart" type="number" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <div mt5px class="flex flex-col gap-3">
        <label for="calendar" class="block mt-4 text-sm font-medium text-gray-700">Date</label>
        <Calendar id="calendar" v-model="values.date" placeholder="Date" date-format="dd-mm-yy" />
      </div>
      <Dropdown v-model="data.tagDTO" :options="tags" option-label="label" placeholder="Associer un tag" class="w-full md:w-14rem">
        <template #option="slotTag">
          <div class="flex flex-row gap-2">
            <div />
            {{ slotTag.option.label }}
          </div>
        </template>
      </Dropdown>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="onConfirm" />
    </div>
  </Dialog> -->
</template>

<style scoped>

</style>
