<script setup lang="ts">
import type { AxiosError } from 'axios'
import { cursor } from 'sisteransi'
import useTag from '~/composables/useTag'
import show = cursor.show

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
const tagStatutLabel = ref<string>('')
const addTagDialog = ref<boolean>(false)
const personalTagForm = reactive({
  tagLabel: '',
  color: {
    red: 0,
    green: 0,
    blue: 0,
  },
  test: '',
})
function switchDisplay() {
  showDefault.value = !showDefault.value
  if (showDefault.value) {
    tagStatutLabel.value = 'Afficher mes tags personnels'
    displayData.value = dataByDefault.value
  } else {
    tagStatutLabel.value = 'Afficher les tags par défaut'
    displayData.value = dataPersonal.value
  }
}
onMounted(() => {
  // displayData.value = data.map(e => formattedData(e))
  getAllTags().then((tags) => {
    dataByDefault.value = displayData.value = tags.filter(e => e.isDefault).map(e => formattedData(e))
    dataPersonal.value = tags.filter(e => !e.isDefault).map(e => formattedData(e))
    tagStatutLabel.value = 'Afficher mes tags personnels'
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
function hexToRgb(hex: string): { r: number, g: number, b: number } {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result
    ? {
        r: Number.parseInt(result[1], 16),
        g: Number.parseInt(result[2], 16),
        b: Number.parseInt(result[3], 16),
      }
    : { r: 0, g: 0, b: 0 }
}
const jToast = useJToast()
function add() {
  const rgb = hexToRgb(personalTagForm.test)
  addPersonalTag(
    personalTagForm.tagLabel,
    {
      red: rgb.r,
      green: rgb.g,
      blue: rgb.b,
    },
  ).then((tag) => {
    dataPersonal.value.push(formattedData(tag))
    addTagDialog.value = false
  })
    .catch((err: AxiosError) => jToast.errorAxios(err))
}
</script>

<template>
  <div>
    <div class="flex flex-row justify-between">
      <p class="text-xl font-semibold text-gray-600">
        Mes tags
      </p>
      <Button class="text-white hover:bg-purple-700" @click="switchDisplay">
        {{ tagStatutLabel }}
      </Button>
      <Button @click="addTagDialog = true">
        Ajouter un nouveau tag personnel
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
      <Column v-if="!showDefault" header-style="width: 5rem; text-align: center" body-style="text-align: center; overflow: visible">
        <template #body="slotTag">
          <Button type="button" icon="pi pi-trash" rounded @click="console.log(slotTag.data)" />
        </template>
      </Column>
    </DataTable>
    <Dialog v-model:visible="addTagDialog" modal header="Ajouter un nouveau tag personnalisé">
      <div class="mt-6">
        <div class="flex flex-col gap-3">
          <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
          <InputText id="label" v-model="personalTagForm.tagLabel" type="text" autocomplete="off" />
        </div>
        <label for="colorPicker" class="block text-sm font-medium text-gray-700">Couleur</label>
        <input id="colorPicker" v-model="personalTagForm.test" type="color">
        <Button label="Ajouter le tag" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="add()" />
      </div>
    </Dialog>
  </div>
</template>

<style lang="scss" scoped>
</style>
