<script setup lang="ts">
import type { AxiosError } from 'axios'
import useTag from '~/composables/useTag'
import { hexToRgb } from '~/utils/util'

definePageMeta({
  layout: 'sidebar-layout',
})

interface DataDisplay {
  id: number
  label: string
  isDefault: string
  color: string
}

const { addPersonalTag, getAllTags, deleteTag } = useTag()

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
const confirm = useConfirm()

const tagToDelete = ref<DataDisplay | undefined>(undefined)
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
    id: tagDTO.tagId,
    label: tagDTO.label,
    isDefault: (tagDTO.isDefault) ? 'Tag par défaut' : 'Tag personnel',
    color,
  }
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
function delTag(row: DataDisplay): void {
  tagToDelete.value = row
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ce tag ?',
    header: 'Confirmer la suppression du tag',
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteTag(row.id).then(() => {
      const indexDelTag = dataPersonal.value.findIndex(e => e.id === row.id)
      if (indexDelTag !== -1) {
        dataPersonal.value.splice(indexDelTag, 1)
      }
    }),
  })
}
</script>

<template>
  <ConfirmDialog />
  <div class="mt-5">
    <div class="flex flex-row justify-between">
      <div class="text-xl font-semibold text-gray-600">
        <p v-if="showDefault">
          Les tags par défaut
        </p>
        <p v-else>
          Mes tags personnels
        </p>
      </div>

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
          <Button type="button" icon="pi pi-trash" rounded outlined severity="danger" @click="delTag(slotTag.data)" />
        </template>
      </Column>
    </DataTable>
    <Dialog v-model:visible="addTagDialog" modal header="Ajouter un nouveau tag personnalisé">
      <div class="mt-6">
        <div class="flex flex-col gap-3">
          <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
          <InputText id="label" v-model="personalTagForm.tagLabel" type="text" autocomplete="off" />
        </div>
        <div class="flex flex-col gap-3">
          <label for="colorPicker" class="block text-sm font-medium text-gray-700">Couleur</label>
          <input id="colorPicker" v-model="personalTagForm.test" type="color">
        </div>
        <Button label="Ajouter le tag" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="add()" />
      </div>
    </Dialog>
  </div>
</template>

<style lang="scss" scoped>
</style>
