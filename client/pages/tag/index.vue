<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
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

const tags = ref<DataDisplay[]>([])
const addTagDialog = ref<boolean>(false)
const personalTagForm = reactive({
  tagLabel: '',
  color: {
    red: 0,
    green: 0,
    blue: 0,
  },
  hex: '',
})
const confirm = useConfirm()
const activeTabIndex = ref<number>(0)

const tagToDelete = ref<DataDisplay | undefined>(undefined)

onMounted(() => {
  getAllTags().then((tagsData) => {
    tags.value = tagsData.map(e => formattedData(e))
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

function add() {
  const rgb = hexToRgb(personalTagForm.hex)
  addPersonalTag(
    personalTagForm.tagLabel,
    {
      red: rgb.r,
      green: rgb.g,
      blue: rgb.b,
    },
  ).then((tag) => {
    tags.value.push(formattedData(tag))
    addTagDialog.value = false
  })
}

function delTag(row: DataDisplay): void {
  tagToDelete.value = row
  confirm.require({
    message: 'Êtes-vous sûr de vouloir supprimer ce tag ?',
    header: 'Confirmer la suppression du tag',
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteTag(row.id).then(() => {
      const indexDelTag = tags.value.findIndex(e => e.id === row.id)
      if (indexDelTag !== -1) {
        tags.value.splice(indexDelTag, 1)
      }
    }),
  })
}
</script>

<template>
  <ConfirmDialog />
  <div class="mt-15 lg:mt-0 w-full flex flex-col justify-center align-center gap-5">
    <TabView v-model:active-index="activeTabIndex">
      <TabPanel header="Tags par défaut">
        <div class="flex flex-col lg:flex-row gap-10 justify-center align-center">
          <div v-for="tag in tags.filter(t => t.isDefault === 'Tag par défaut')" :key="tag.id" class="tag-card" :style="{ '--tag-color': tag.color }">
            <div class="tag-header">
              <h4>{{ tag.label }}</h4>
            </div>
          </div>
        </div>
      </TabPanel>
      <TabPanel header="Tags personnels">
        <div class="flex flex-col gap-10 w-full justify-center">
          <div class="flex flex-col lg:flex-row gap-10 justify-center align-center">
            <div v-for="tag in tags.filter(t => t.isDefault !== 'Tag par défaut')" :key="tag.id" class="tag-card" :style="{ '--tag-color': tag.color }">
              <div class="tag-header">
                <h4>{{ tag.label }}</h4>
              </div>
              <Button type="button" icon="pi pi-trash" class="w-35px h-35px" rounded raised @click="delTag(tag)" />
            </div>
          </div>
          <Button class="w50 self-center mb-10" @click="addTagDialog = true">
            Ajouter un nouveau tag personnel
          </Button>
        </div>
      </TabPanel>
    </TabView>

    <Dialog v-model:visible="addTagDialog" modal header="Ajouter un nouveau tag personnalisé">
      <div class="mt-6">
        <div class="flex flex-col gap-3">
          <label for="label" class="block text-sm font-medium text-gray-700">Libelle</label>
          <InputText id="label" v-model="personalTagForm.tagLabel" type="text" autocomplete="off" />
        </div>
        <div class="flex flex-col gap-3">
          <label for="colorPicker" class="block text-sm font-medium text-gray-700">Couleur</label>
          <input id="colorPicker" v-model="personalTagForm.hex" type="color">
        </div>
        <Button label="Ajouter le tag" class="mt-6 w-full bg-purple-600 text-white hover:bg-purple-700" @click="add()" />
      </div>
    </Dialog>
  </div>
</template>

<style lang="scss" scoped>
.tag-card {
  width: 300px;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  box-shadow: 0 4px 8px;
  background-color: var(--tag-color);
  text-transform: uppercase;
}

.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 0 10px;
  height: 100%;
  color: #000;
}

.tag-section h2 {
  margin-bottom: 10px;
  font-size: 20px;
  color: var(--primary);
}
</style>
