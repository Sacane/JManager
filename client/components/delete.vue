<script setup>
const visible = ref(false)
const onActionValid = ref()

</script>


<template>
  <PDialog v-model:visible="visible" modal>
    <p>Etes vous sur de bien vouloir supprimer l'élément ?</p>
    <PButton @click="onActionValid" label="Supprimer" icon="pi pi-trash" severity="danger" />
  </PDialog>
</template>