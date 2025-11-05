<script setup lang="ts">
import useCsvImport from '~/composables/useCsvImport'

const props = defineProps<{
  bookletId: string
  month?: string
  year?: number
}>()

const emit = defineEmits(['visible', 'importSuccess'])

const { validateCsvFile, importTransactionsFromCsv } = useCsvImport()
const toast = useJToast()
const date = useDate()

const isVisible = ref(false)
const currentStep = ref<'analysis' | 'import'>('analysis')
const selectedFile = ref<File | null>(null)
const isAnalyzing = ref(false)
const isImporting = ref(false)

const validationReport = ref<CsvValidationReportDTO | null>(null)
const importResult = ref<CsvImportResultDTO | null>(null)

const fileInputRef = ref<HTMLInputElement | null>(null)
const showFormatHelper = ref(false)

const hasDayOnlySupport = computed(() => props.month !== undefined && props.year !== undefined)

const canAnalyze = computed(() => selectedFile.value !== null && !isAnalyzing.value)
const canImport = computed(() =>
  validationReport.value?.canImport
  && !validationReport.value?.hasErrors
  && !isImporting.value,
)
const hasValidationErrors = computed(() =>
  validationReport.value?.hasErrors || false,
)
const hasValidationWarnings = computed(() =>
  validationReport.value && validationReport.value.warnings.length > 0,
)

// Methods
function openDialog() {
  isVisible.value = true
  resetDialog()
}

function closeDialog() {
  isVisible.value = false
  emit('visible', false)
  setTimeout(() => resetDialog(), 300)
}

function resetDialog() {
  currentStep.value = 'analysis'
  selectedFile.value = null
  validationReport.value = null
  importResult.value = null
  isAnalyzing.value = false
  isImporting.value = false
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0 && target.files[0]) {
    const file = target.files[0]

    if (!file.name.toLowerCase().endsWith('.csv')) {
      toast.error('Seuls les fichiers CSV sont acceptés (.csv)')
      if (fileInputRef.value) {
        fileInputRef.value.value = ''
      }
      return
    }

    const acceptedTypes = ['text/csv', 'text/plain', 'application/csv', 'application/vnd.ms-excel']
    if (file.type && !acceptedTypes.includes(file.type)) {
      console.warn(`Type MIME suspect: ${file.type}. Acceptation conditionnelle.`)
    }

    const maxSize = 5 * 1024 * 1024
    if (file.size > maxSize) {
      const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2)
      toast.error(`Le fichier est trop volumineux (${fileSizeMB} Mo). Taille maximale : 5 Mo`)
      if (fileInputRef.value) {
        fileInputRef.value.value = ''
      }
      return
    }

    if (file.size === 0) {
      toast.error('Le fichier est vide')
      if (fileInputRef.value) {
        fileInputRef.value.value = ''
      }
      return
    }

    selectedFile.value = file
    validationReport.value = null
    importResult.value = null
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function removeFile() {
  selectedFile.value = null
  validationReport.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function analyzeFile() {
  if (!selectedFile.value) return

  isAnalyzing.value = true
  try {
    // Convertir le mois de props en numéro si disponible
    let monthNumber: number | undefined
    let yearNumber: number | undefined

    if (hasDayOnlySupport.value && props.month && props.year) {
      monthNumber = date.numberFromMonth(props.month)
      yearNumber = props.year
    }

    validationReport.value = await validateCsvFile(
      props.bookletId,
      selectedFile.value,
      monthNumber,
      yearNumber,
    )

    if (validationReport.value && validationReport.value.hasErrors) {
      toast.error('Le fichier contient des erreurs qui empêchent l\'importation')
    } else if (validationReport.value && validationReport.value.warnings.length > 0) {
      toast.warn('Le fichier contient des avertissements, vérifiez-les avant d\'importer')
    } else {
      toast.success('Fichier validé avec succès !')
    }
  } catch (error) {
    console.error('Erreur lors de l\'analyse:', error)
    toast.error('Erreur lors de l\'analyse du fichier')
  } finally {
    isAnalyzing.value = false
  }
}

async function importFile() {
  if (!selectedFile.value || !canImport.value) return

  isImporting.value = true
  try {
    // Convertir le mois de props en numéro si disponible
    let monthNumber: number | undefined
    let yearNumber: number | undefined

    if (hasDayOnlySupport.value && props.month && props.year) {
      monthNumber = date.numberFromMonth(props.month)
      yearNumber = props.year
    }

    importResult.value = await importTransactionsFromCsv(
      props.bookletId,
      selectedFile.value,
      true,
      monthNumber,
      yearNumber,
    )

    if (importResult.value && importResult.value.hasErrors) {
      toast.error(`Importation terminée avec des erreurs. ${importResult.value.successCount} transactions importées, ${importResult.value.failedCount} échecs`)
    } else if (importResult.value) {
      toast.success(`${importResult.value.successCount} transactions importées avec succès !`)
      emit('importSuccess', importResult.value)
      setTimeout(() => closeDialog(), 2000)
    }
  } catch (error) {
    console.error('Erreur lors de l\'importation:', error)
    toast.error('Erreur lors de l\'importation du fichier')
  } finally {
    isImporting.value = false
  }
}

function downloadTemplate() {
  const headers = 'date,label,depense,recette,tag'
  const exampleRows = [
    '15-01-2025,Courses Carrefour,45.50,,Alimentation & Restaurant',
    '16-01-2025,Salaire,,2500.00,Aucune',
    '17-01-2025,Essence,60.00,,Transport',
  ]
  const csvContent = [headers, ...exampleRows].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })

  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)

  link.setAttribute('href', url)
  link.setAttribute('download', 'template_import_transactions.csv')
  link.style.visibility = 'hidden'

  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  URL.revokeObjectURL(url)

  toast.success('Fichier template téléchargé avec succès !')
}

defineExpose({
  openDialog,
})
</script>

<template>
  <Dialog
    v-model:visible="isVisible"
    header="Importer des transactions CSV"
    :style="{ width: '50rem', maxWidth: '95vw' }"
    modal
    dismissable-mask
    @hide="closeDialog"
  >
    <div class="csv-import-container">
      <!-- Format Helper Section -->
      <div class="format-helper-section">
        <div class="format-helper-header" @click="showFormatHelper = !showFormatHelper">
          <div class="format-helper-title">
            <i class="pi pi-question-circle" />
            <span>Besoin d'aide sur le format CSV ?</span>
          </div>
          <i class="pi" :class="[showFormatHelper ? 'pi-chevron-up' : 'pi-chevron-down']" />
        </div>

        <!-- CSV Example - Always visible -->
        <div class="format-example-standalone">
          <div class="format-example-header">
            <h4><i class="pi pi-file-edit" /> Exemple de fichier CSV valide</h4>
            <Button
              label="Télécharger le template"
              icon="pi pi-download"
              severity="secondary"
              size="small"
              outlined
              @click="downloadTemplate"
            />
          </div>
          <div class="csv-preview">
            <pre><code>date,label,depense,recette,tag
15-01-2025,Courses Carrefour,45.50,,Alimentation & Restaurant
16-01-2025,Salaire,,2500.00,Aucune
17-01-2025,Essence,60.00,,Transport
18-01-2025,Restaurant,35.80,,Alimentation & Restaurant</code></pre>
          </div>
          <p class="template-hint">
            <i class="pi pi-lightbulb" />
            Téléchargez ce fichier template pour commencer rapidement. Il contient déjà les en-têtes et quelques exemples que vous pouvez modifier.
          </p>
        </div>

        <Transition name="slide-fade">
          <div v-show="showFormatHelper" class="format-helper-content">
            <div class="format-intro">
              <p>Votre fichier CSV doit respecter le format suivant pour être importé correctement :</p>
            </div>

            <div class="format-structure">
              <h4><i class="pi pi-list" /> Structure des colonnes (ordre important)</h4>
              <div class="columns-grid">
                <div class="column-card">
                  <div class="column-header">
                    <i class="pi pi-calendar" />
                    <span class="column-name">date</span>
                    <span class="column-required">Obligatoire</span>
                  </div>
                  <p class="column-description">
                    Format : <code>JJ-MM-AAAA</code>
                  </p>
                  <p class="column-example">
                    Exemple : <code>15-01-2025</code>
                  </p>
                </div>

                <div class="column-card">
                  <div class="column-header">
                    <i class="pi pi-tag" />
                    <span class="column-name">label</span>
                    <span class="column-required">Obligatoire</span>
                  </div>
                  <p class="column-description">
                    Description de la transaction
                  </p>
                  <p class="column-example">
                    Exemple : <code>Courses Carrefour</code>
                  </p>
                </div>

                <div class="column-card">
                  <div class="column-header">
                    <i class="pi pi-arrow-down" />
                    <span class="column-name">depense</span>
                    <span class="column-optional">Optionnel*</span>
                  </div>
                  <p class="column-description">
                    Montant de la dépense (positif)
                  </p>
                  <p class="column-example">
                    Exemple : <code>45.50</code>
                  </p>
                </div>

                <div class="column-card">
                  <div class="column-header">
                    <i class="pi pi-arrow-up" />
                    <span class="column-name">recette</span>
                    <span class="column-optional">Optionnel*</span>
                  </div>
                  <p class="column-description">
                    Montant de la recette (positif)
                  </p>
                  <p class="column-example">
                    Exemple : <code>2500.00</code>
                  </p>
                </div>

                <div class="column-card">
                  <div class="column-header">
                    <i class="pi pi-bookmark" />
                    <span class="column-name">tag</span>
                    <span class="column-optional">Optionnel</span>
                  </div>
                  <p class="column-description">
                    Catégorie de la transaction
                  </p>
                  <p class="column-example">
                    Exemple : <code>Alimentation & Restaurant</code>
                  </p>
                </div>
              </div>
              <p class="columns-note">
                <i class="pi pi-info-circle" />
                * Vous devez remplir <strong>soit</strong> depense <strong>soit</strong> recette, mais pas les deux en même temps
              </p>
            </div>

            <div class="format-rules">
              <h4><i class="pi pi-exclamation-circle" /> Règles importantes</h4>
              <ul class="rules-list">
                <li><i class="pi pi-check-circle" /> La première ligne doit contenir les en-têtes de colonnes</li>
                <li><i class="pi pi-check-circle" /> Les colonnes doivent être séparées par des virgules</li>
                <li><i class="pi pi-check-circle" /> Les montants utilisent le point comme séparateur décimal</li>
                <li><i class="pi pi-check-circle" /> Les montants doivent être positifs (pas de signe moins)</li>
                <li><i class="pi pi-check-circle" /> Taille maximale du fichier : 5 Mo</li>
                <li><i class="pi pi-check-circle" /> Maximum 10 000 lignes de transactions</li>
                <li><i class="pi pi-check-circle" /> Si un tag n'existe pas, un avertissement sera affiché</li>
              </ul>
            </div>

            <div class="format-tags">
              <h4><i class="pi pi-bookmark" /> Tags disponibles</h4>
              <p class="tags-intro">
                Vous pouvez utiliser l'un des tags suivants (ou laisser vide) :
              </p>
              <div class="tags-list">
                <span class="tag-chip">Aucune</span>
                <span class="tag-chip">Alimentation & Restaurant</span>
                <span class="tag-chip">Transport</span>
                <span class="tag-chip">Loisirs</span>
                <span class="tag-chip">Santé</span>
                <span class="tag-chip">Logement</span>
                <span class="tag-chip">Abonnements</span>
              </div>
              <p class="tags-note">
                <i class="pi pi-info-circle" />
                Si vous utilisez un tag qui n'existe pas, il sera créé automatiquement avec un avertissement.
              </p>
            </div>
          </div>
        </Transition>
      </div>

      <div v-if="currentStep === 'analysis'" class="analysis-section">
        <div class="file-selection-zone">
          <input
            ref="fileInputRef"
            type="file"
            accept=".csv"
            style="display: none"
            @change="onFileSelected"
          >

          <div v-if="!selectedFile" class="upload-area" @click="triggerFileInput">
            <i class="pi pi-cloud-upload upload-icon" />
            <h3>Sélectionnez un fichier CSV</h3>
            <p>Cliquez pour parcourir vos fichiers</p>
            <div class="file-constraints">
              <p class="file-format-hint">
                <i class="pi pi-info-circle" /> Format: date, label, depense, recette, tag
              </p>
              <p class="file-limit-hint">
                <i class="pi pi-exclamation-triangle" /> Taille max: 5 Mo | Max 10,000 lignes
              </p>
              <p class="file-type-hint">
                <i class="pi pi-file" /> Uniquement des fichiers .csv
              </p>
            </div>
          </div>

          <div v-else class="file-selected">
            <div class="file-info">
              <i class="pi pi-file" />
              <div class="file-details">
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">{{ (selectedFile.size / 1024).toFixed(2) }} KB</span>
              </div>
            </div>
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click="removeFile"
            />
          </div>
        </div>

        <div v-if="hasDayOnlySupport" class="day-only-info-section">
          <div class="info-card">
            <i class="pi pi-calendar" />
            <div class="info-content">
              <h4>Support des dates avec jour seul</h4>
              <p>
                Votre fichier CSV peut contenir uniquement des jours (ex: 1, 15, 28).
                Les dates seront automatiquement complétées avec
                <strong>{{ date.translate(props.month!) }} {{ props.year }}</strong>.
              </p>
              <div class="info-example">
                <i class="pi pi-lightbulb" />
                <span>Exemple : "15" dans votre CSV deviendra le 15 {{ date.translate(props.month!) }} {{ props.year }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="action-zone">
          <Button
            :label="isAnalyzing ? 'Analyse en cours...' : 'Analyser le fichier'"
            :icon="isAnalyzing ? 'pi pi-spin pi-spinner' : 'pi pi-search'"
            :disabled="!canAnalyze"
            class="analyze-button"
            @click="analyzeFile"
          />
        </div>

        <div v-if="validationReport" class="validation-results">
          <div class="results-summary">
            <div class="summary-card">
              <i class="pi pi-file" />
              <div>
                <span class="summary-label">Lignes totales</span>
                <span class="summary-value">{{ validationReport.totalLines }}</span>
              </div>
            </div>
            <div class="summary-card">
              <i class="pi pi-check-circle" />
              <div>
                <span class="summary-label">Lignes valides</span>
                <span class="summary-value">{{ validationReport.validLines }}</span>
              </div>
            </div>
            <div v-if="validationReport.errors.length > 0" class="summary-card error">
              <i class="pi pi-times-circle" />
              <div>
                <span class="summary-label">Erreurs</span>
                <span class="summary-value">{{ validationReport.errors.length }}</span>
              </div>
            </div>
            <div v-if="validationReport.warnings.length > 0" class="summary-card warning">
              <i class="pi pi-exclamation-triangle" />
              <div>
                <span class="summary-label">Avertissements</span>
                <span class="summary-value">{{ validationReport.warnings.length }}</span>
              </div>
            </div>
          </div>

          <!-- Errors -->
          <div v-if="hasValidationErrors" class="issues-section error-section">
            <h4><i class="pi pi-times-circle" /> Erreurs bloquantes</h4>
            <div class="issues-list">
              <div
                v-for="(error, index) in validationReport.errors"
                :key="index"
                class="issue-item error-item"
              >
                <div class="issue-header">
                  <span class="issue-line">Ligne {{ error.lineNumber }}</span>
                  <span class="issue-type">{{ error.type }}</span>
                </div>
                <p class="issue-message">
                  {{ error.message }}
                </p>
                <p v-if="error.detectedValue" class="issue-value">
                  Valeur détectée : <code>{{ error.detectedValue }}</code>
                </p>
              </div>
            </div>
          </div>

          <!-- Warnings -->
          <div v-if="hasValidationWarnings" class="issues-section warning-section">
            <h4><i class="pi pi-exclamation-triangle" /> Avertissements</h4>
            <div class="issues-list">
              <div
                v-for="(warning, index) in validationReport.warnings"
                :key="index"
                class="issue-item warning-item"
              >
                <div class="issue-header">
                  <span class="issue-line">Ligne {{ warning.lineNumber }}</span>
                  <span class="issue-type">{{ warning.type }}</span>
                </div>
                <p class="issue-message">
                  {{ warning.message }}
                </p>
                <p v-if="warning.detectedValue" class="issue-value">
                  Valeur détectée : <code>{{ warning.detectedValue }}</code>
                </p>
              </div>
            </div>
          </div>

          <!-- Suggestions -->
          <div v-if="validationReport.suggestions.length > 0" class="suggestions-section">
            <h4><i class="pi pi-lightbulb" /> Suggestions</h4>
            <ul class="suggestions-list">
              <li v-for="(suggestion, index) in validationReport.suggestions" :key="index">
                {{ suggestion }}
              </li>
            </ul>
          </div>

          <!-- Import Action -->
          <div v-if="canImport" class="import-action">
            <Button
              :label="isImporting ? 'Importation en cours...' : 'Importer les transactions'"
              :icon="isImporting ? 'pi pi-spin pi-spinner' : 'pi pi-upload'"
              :disabled="!canImport || isImporting"
              class="import-button"
              @click="importFile"
            />
          </div>
        </div>

        <!-- Import Results -->
        <div v-if="importResult" class="import-results">
          <div class="results-header" :class="{ success: !importResult.hasErrors, error: importResult.hasErrors }">
            <i :class="importResult.hasErrors ? 'pi pi-exclamation-circle' : 'pi pi-check-circle'" />
            <h3>{{ importResult.hasErrors ? 'Importation terminée avec des erreurs' : 'Importation réussie !' }}</h3>
          </div>

          <div class="import-stats">
            <div class="import-stat success">
              <span>{{ importResult.successCount }} importées</span>
            </div>
            <div v-if="importResult.failedCount > 0" class="import-stat error">
              <span>{{ importResult.failedCount }} échecs</span>
            </div>
          </div>

          <!-- Failed lines -->
          <div v-if="importResult.errors.length > 0" class="import-errors">
            <h4>Détails des erreurs</h4>
            <div
              v-for="(error, index) in importResult.errors"
              :key="index"
              class="import-error-item"
            >
              <div class="error-line">
                Ligne {{ error.lineNumber }}
              </div>
              <ul class="error-messages">
                <li v-for="(msg, msgIndex) in error.errors" :key="msgIndex">
                  {{ msg }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.csv-import-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Format Helper Section */
.format-helper-section {
  background: #f8fafc;
  border-radius: 12px;
  padding: 1rem;
  border: 1px solid #e2e8f0;
}

.format-helper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 0.75rem;
  border-radius: 8px;
  transition: background 0.2s;
}

.format-helper-header:hover {
  background: rgba(102, 126, 234, 0.05);
}

.format-helper-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: #667eea;
  font-weight: 600;
  font-size: 1rem;
}

.format-helper-title i {
  font-size: 1.25rem;
}

.format-example-standalone {
  margin-top: 1rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.format-example-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.format-example-header h4 {
  margin: 0;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.csv-preview {
  background: #1e293b;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 0.75rem;
}

.csv-preview pre {
  margin: 0;
  color: #e2e8f0;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  line-height: 1.6;
}
.csv-preview code {
  color: #e2e8f0;
}

.template-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  padding: 0.75rem;
  background: #fef3c7;
  border-radius: 6px;
  color: #92400e;
  font-size: 0.875rem;
}

.template-hint i {
  color: #f59e0b;
}

.format-helper-content {
  margin-top: 1rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.format-intro {
  margin-bottom: 1.5rem;
}

.format-intro p {
  margin: 0;
  color: #475569;
  font-size: 0.95rem;
}

.format-structure {
  margin-bottom: 1.5rem;
}

.format-structure h4 {
  margin: 0 0 1rem 0;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.columns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.column-card {
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.column-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.column-header i {
  color: #667eea;
  font-size: 1.1rem;
}

.column-name {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #1e293b;
  font-size: 0.95rem;
}

.column-required {
  background: #fee2e2;
  color: #dc2626;
  font-size: 0.7rem;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-weight: 600;
  text-transform: uppercase;
}

.column-optional {
  background: #dbeafe;
  color: #2563eb;
  font-size: 0.7rem;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-weight: 600;
  text-transform: uppercase;
}

.column-description {
  margin: 0 0 0.5rem 0;
  color: #64748b;
  font-size: 0.875rem;
}

.column-example {
  margin: 0;
  color: #475569;
  font-size: 0.85rem;
}

.column-example code,
.column-description code {
  background: white;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  color: #667eea;
  border: 1px solid #e2e8f0;
}

.columns-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #fef3c7;
  border-radius: 6px;
  color: #92400e;
  font-size: 0.875rem;
  margin: 0;
}

.columns-note i {
  color: #f59e0b;
  margin-top: 0.125rem;
}

.format-rules {
  margin-bottom: 1.5rem;
}

.format-rules h4 {
  margin: 0 0 0.75rem 0;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.rules-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.rules-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  color: #475569;
  font-size: 0.875rem;
  line-height: 1.5;
}

.rules-list li i {
  color: #10b981;
  margin-top: 0.125rem;
}

.format-tags {
  margin-bottom: 0;
}

.format-tags h4 {
  margin: 0 0 0.75rem 0;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tags-intro {
  margin: 0 0 0.75rem 0;
  color: #475569;
  font-size: 0.875rem;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.tag-chip {
  padding: 0.375rem 0.75rem;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 20px;
  font-size: 0.813rem;
  font-weight: 500;
}

.tags-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #dbeafe;
  border-radius: 6px;
  color: #1e40af;
  font-size: 0.875rem;
  margin: 0;
}

.tags-note i {
  color: #2563eb;
  margin-top: 0.125rem;
}

/* Analysis Section */
.analysis-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.file-selection-zone {
  margin-bottom: 0;
}

.upload-area {
  padding: 3rem 2rem;
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.upload-area:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);
  transform: translateY(-2px);
}

.upload-icon {
  font-size: 3rem;
  color: #667eea;
  margin-bottom: 1rem;
}

.upload-area h3 {
  margin: 0 0 0.5rem 0;
  color: #1e293b;
  font-size: 1.25rem;
}

.upload-area p {
  margin: 0.25rem 0;
  color: #64748b;
}

.file-constraints {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.file-format-hint,
.file-limit-hint,
.file-type-hint {
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
}

.file-format-hint {
  color: #3b82f6;
}

.file-limit-hint {
  color: #f59e0b;
}

.file-type-hint {
  color: #64748b;
}

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.file-info i {
  font-size: 2rem;
  color: #667eea;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.file-name {
  font-weight: 600;
  color: #1e293b;
}

.file-size {
  font-size: 0.875rem;
  color: #64748b;
}

/* Day-Only Info Section */
.day-only-info-section {
  margin: 1.5rem 0;
}

.info-card {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 6px rgba(102, 126, 234, 0.2);
}

.info-card > i {
  font-size: 2rem;
  flex-shrink: 0;
  opacity: 0.9;
}

.info-content {
  flex: 1;
}

.info-content h4 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.info-content p {
  margin: 0 0 0.75rem 0;
  font-size: 0.95rem;
  line-height: 1.5;
  opacity: 0.95;
}

.info-content strong {
  font-weight: 700;
  text-decoration: underline;
}

.info-example {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.info-example i {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.info-example span {
  font-size: 0.875rem;
  font-weight: 500;
}

/* Action Zone */
.action-zone {
  display: flex;
  justify-content: center;
}

.analyze-button {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 600;
}

/* Validation Results */
.validation-results {
  margin-top: 1rem;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.results-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.summary-card.error {
  background: #fef2f2;
  border-color: #fecaca;
}

.summary-card.warning {
  background: #fefce8;
  border-color: #fef08a;
}

.summary-card i {
  font-size: 1.5rem;
  color: #667eea;
}

.summary-card.error i {
  color: #ef4444;
}

.summary-card.warning i {
  color: #f59e0b;
}

.summary-card div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.813rem;
  color: #64748b;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

/* Issues Section */
.issues-section {
  margin-bottom: 1.5rem;
}

.issues-section h4 {
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.error-section h4 {
  color: #dc2626;
}

.warning-section h4 {
  color: #f59e0b;
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.issue-item {
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid;
}

.error-item {
  background: #fef2f2;
  border-color: #ef4444;
}

.warning-item {
  background: #fefce8;
  border-color: #f59e0b;
}

.issue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.issue-line {
  font-weight: 600;
  color: #1e293b;
}

.issue-type {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  color: #475569;
  font-family: 'Courier New', monospace;
}

.issue-message {
  margin: 0 0 0.5rem 0;
  color: #475569;
  font-size: 0.875rem;
  line-height: 1.5;
}

.issue-value {
  margin: 0;
  font-size: 0.813rem;
  color: #64748b;
}

.issue-value code {
  background: rgba(0, 0, 0, 0.05);
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

/* Suggestions Section */
.suggestions-section {
  margin-bottom: 1.5rem;
}

.suggestions-section h4 {
  margin: 0 0 0.75rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #0284c7;
}

.suggestions-list {
  margin: 0;
  padding-left: 1.5rem;
}

.suggestions-list li {
  color: #0c4a6e;
  margin-bottom: 0.5rem;
}

/* Import Action */
.import-action {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.import-button {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 600;
}

/* Import Results */
.import-results {
  margin-top: 2rem;
  padding: 1.5rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.results-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid;
}

.results-header.success {
  color: #16a34a;
  border-color: #86efac;
}

.results-header.error {
  color: #dc2626;
  border-color: #fca5a5;
}

.results-header i {
  font-size: 2rem;
}

.results-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.import-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.import-stat {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
}

.import-stat.success {
  background: #d1fae5;
  color: #065f46;
}

.import-stat.error {
  background: #fee2e2;
  color: #991b1b;
}

.import-errors h4 {
  margin: 0 0 1rem 0;
  color: #dc2626;
}

.import-error-item {
  padding: 1rem;
  background: #fef2f2;
  border-radius: 6px;
  border-left: 4px solid #ef4444;
  margin-bottom: 0.75rem;
}

.error-line {
  font-weight: 600;
  color: #991b1b;
  margin-bottom: 0.5rem;
}

.error-messages {
  margin: 0;
  padding-left: 1.25rem;
  color: #7f1d1d;
}

.error-messages li {
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
}

/* Slide Fade Transition */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from {
  transform: translateY(-10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>
