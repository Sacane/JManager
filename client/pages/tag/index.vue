<script setup lang="ts">
import useTag from '~/composables/useTag'

definePageMeta({
  layout: 'sidebar-layout',
})

interface DataDisplay {
  label: string
  isDefault: string
  color: string
}

const { addPersonalTag, getAllTags } = useTag()

const displayData = ref<DataDisplay[]>([])
const dataByDefault = ref<DataDisplay[]>([])
const dataPersonal = ref<DataDisplay[]>([])
const showDefault = ref<boolean>(true)

function switchDisplay() {
  showDefault.value = !showDefault.value
  if (showDefault.value) {
    displayData.value = dataByDefault.value
  } else {
    displayData.value = dataPersonal.value
  }
}
onMounted(() => {
  // displayData.value = data.map(e => formattedData(e))
  getAllTags().then((tags) => {
    dataByDefault.value = displayData.value = tags.filter(e => e.isDefault).map(e => formattedData(e))
    dataPersonal.value = tags.filter(e => !e.isDefault).map(e => formattedData(e))
  })
})

function formattedData(tagDTO: TagDTO): DataDisplay {
  const color = `rgb(${tagDTO.colorDTO.red}, ${tagDTO.colorDTO.green}, ${tagDTO.colorDTO.blue})`
  return {
    label: tagDTO.label,
    isDefault: (tagDTO.isDefault) ? 'Tag par défaut' : 'Tag personnel',
    color,
  }
}
</script>

<template>
  <div>
    <div class="flex flex-row justify-between">
      <p class="text-xl font-semibold text-gray-600">
        Mes tags
      </p>
      <Button class="text-white hover:bg-purple-700" @click="switchDisplay">
        Mes tags personnels
      </Button>
    </div>

    <DataTable data-key="id" table-style="min-width: 50rem" :value="displayData">
      <Column field="label" header="Libellé du tag" />
      <Column field="isDefault" header="Statut" />
      <Column header-style="width: 5rem; text-align: center" body-style="text-align: center; overflow: visible">
        <template #body="slotTag">
          <div :style="`width: 20px; height: 20px; background-color: ${slotTag.data.color}; border-radius: 50%;`" />
        </template>
      </Column>
      <Column header-style="width: 5rem; text-align: center" body-style="text-align: center; overflow: visible">
        <template #body="slotTag">
          <Button type="button" icon="pi pi-trash" rounded @click="console.log(slotTag.data)" />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style lang="scss" scoped>
</style>
