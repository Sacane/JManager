<script setup>
const visible = ref(false)
const onActionValid = ref()
</script>

<template>
  <Dialog v-model:visible="visible" modal>
    <p>Etes vous sur de bien vouloir supprimer l'élément ?</p>
    <Button label="Supprimer" icon="pi pi-trash" severity="danger" @click="onActionValid" />
  </Dialog>
</template>
