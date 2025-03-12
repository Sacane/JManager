<script setup lang="ts">
export interface AccountBookingProps {
  integerpart: string
  decimalpart: string
}

const { integerpart, decimalpart } = defineProps<AccountBookingProps>()
const emit = defineEmits(['visible', 'createAccount', 'cancel'])

const accountData = ref({
  label: '',
  integerpart,
  decimalpart,
})
const isVisibleData = ref(false)

function createAccount() {
  emit('createAccount', accountData.value)
  closeDialog()
}

function closeDialog() {
  emit('visible', false)
  emit('cancel')
  isVisibleData.value = false
}
</script>

<template>
  <Dialog
    v-model:visible="isVisibleData"
    class="bg-grey"
    modal header="Ajouter un nouveau livret" @update:visible="closeDialog"
  >
    <div class="mt-6">
      <div class="flex flex-col gap-3">
        <label for="label" class="block text-sm font-medium text-gray-700">Libellé du livret</label>
        <InputText id="label" v-model="accountData.label" type="text" autocomplete="off" />
      </div>

      <label for="labelAmount" class="block mt-4 text-sm font-medium text-gray-700">Montant</label>
      <div id="labelAmount" class="flex-row">
        <InputText v-model="accountData.integerpart" type="string" placeholder="Partie entière" class="" />
        <InputText v-model="accountData.decimalpart" type="string" placeholder="Partie décimale" maxlength="2" class="" />
      </div>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="createAccount" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="closeDialog" />
    </div>
  </Dialog>
</template>

<style scoped>

</style>
