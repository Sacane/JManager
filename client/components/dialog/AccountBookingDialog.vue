<script setup lang="ts">
const emit = defineEmits(['visible', 'createAccount', 'cancel'])

const accountData = ref({
  label: '',
  digit: null,
})
const isVisibleData = ref(false)

function createAccount() {
  if (accountData.value.digit === null) {
    return
  }
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
        <InputNumber v-model="accountData.digit" placeholder="0,00" class="w-full" :max-fraction-digits="2" />
      </div>
      <Button label="Créer" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="createAccount" />
      <Button label="Annuler" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="closeDialog" />
    </div>
  </Dialog>
</template>

<style scoped>
</style>
