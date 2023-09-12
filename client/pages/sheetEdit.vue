<script setup lang="ts">

const route = useRoute()

definePageMeta({
  layout: 'sidebar-layout',
})

const data = reactive({
  id: route.query.id
})

onMounted(() => {
  console.log(data.id)
})

</script>


<template>
  <!--  <div wfull hfull flex items-center>
    
    <div class="form-container" flex-col mb5>
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
        <div mt5px>
          <label for="number">Indiquer le montant de la transaction (en euros)</label>
          <PInputNumber v-model="values.amount" id="number"/>
        </div>
        <div mt5px>
          <label for="calendar">Indiquer la date de la transaction</label>
          <PCalendar placeholder="Date" v-model="values.date" date-format="dd-mm-yy" id="calendar"/>
        </div>
        <div mt5px>
          <PButton label="Ajouter une nouvelle transaction"/>
        </div>
      </PFieldset>
      
    </div>
  </div>-->
</template>