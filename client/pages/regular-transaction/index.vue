<script setup lang="ts">
definePageMeta({
  layout: 'sidebar-layout',
})

const { saveRegularTransaction, getRegularTransaction } = useRegularTransaction()

const transactions = ref<RegularTransaction[]>([])

onMounted(() => {
  getRegularTransaction()
    .then((res) => {
      transactions.value = res
    })
    .catch((err) => {
      console.error(err)
    })
})

function onSave(transaction: RegularTransactionCreationRequest) {
  saveRegularTransaction(transaction)
    .then(() => {
      console.log('Transaction saved successfully')
    })
    .catch((err) => {
      console.error(err)
    })
}
</script>

<template>
  <div>
    <h1>Hello world</h1>
  </div>
</template>

<style scoped>

</style>
