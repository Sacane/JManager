<script setup lang="ts">
import useCsvImport from '~/composables/useCsvImport'

const props = defineProps<{
  bookletId: string
}>()

const emit = defineEmits(['visible', 'importSuccess'])

const { validateCsvFile, importTransactionsFromCsv } = useCsvImport()
const toast = useJToast()

const isVisible = ref(false)
const currentStep = ref<'analysis' | 'import'>('analysis')
const selectedFile = ref<File | null>(null)
const isAnalyzing = ref(false)
const isImporting = ref(false)

const validationReport = ref<CsvValidationReportDTO | null>(null)
const importResult = ref<CsvImportResultDTO | null>(null)

const fileInputRef = ref<HTMLInputElement | null>(null)

// Computed
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
    validationReport.value = await validateCsvFile(props.bookletId, selectedFile.value)

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
    importResult.value = await importTransactionsFromCsv(props.bookletId, selectedFile.value, true)

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
      <!-- Étape 1: Analysis -->
      <div v-if="currentStep === 'analysis'" class="analysis-section">
        <!-- File Selection -->
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

        <!-- Action Button -->
        <div class="action-zone">
          <Button
            :label="isAnalyzing ? 'Analyse en cours...' : 'Analyser le fichier'"
            :icon="isAnalyzing ? 'pi pi-spin pi-spinner' : 'pi pi-search'"
            :disabled="!canAnalyze"
            class="analyze-button"
            @click="analyzeFile"
          />
        </div>

        <!-- Validation Results -->
        <div v-if="validationReport" class="validation-results">
          <!-- Summary -->
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
                  Valeur: <code>{{ error.detectedValue }}</code>
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
                  Valeur: <code>{{ warning.detectedValue }}</code>
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

          <!-- Import Button -->
          <div v-if="canImport" class="import-action">
            <Button
              :label="isImporting ? 'Importation en cours...' : 'Importer les transactions'"
              :icon="isImporting ? 'pi pi-spin pi-spinner' : 'pi pi-upload'"
              :disabled="!canImport || isImporting"
              class="import-button"
              severity="success"
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

          <div class="import-summary">
            <div class="import-stat success">
              <i class="pi pi-check" />
              <span>{{ importResult.successCount }} importées</span>
            </div>
            <div v-if="importResult.failedCount > 0" class="import-stat error">
              <i class="pi pi-times" />
              <span>{{ importResult.failedCount }} échecs</span>
            </div>
          </div>

          <!-- Import Errors -->
          <div v-if="importResult.errors.length > 0" class="import-errors">
            <h4>Détails des erreurs</h4>
            <div class="import-errors-list">
              <div
                v-for="(error, index) in importResult.errors"
                :key="index"
                class="import-error-item"
              >
                <span class="error-line">Ligne {{ error.lineNumber }}</span>
                <ul>
                  <li v-for="(msg, msgIndex) in error.errors" :key="msgIndex">
                    {{ msg }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.csv-import-container {
  padding: 1rem 0;
}

/* File Selection */
.file-selection-zone {
  margin-bottom: 1.5rem;
}

.upload-area {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 3rem 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f8fafc;
}

.upload-area:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.upload-icon {
  font-size: 3rem;
  color: #64748b;
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
  color: #3b82f6;
}

.file-details {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-weight: 600;
  color: #1e293b;
}

.file-size {
  font-size: 0.875rem;
  color: #64748b;
}

/* Action Zone */
.action-zone {
  display: flex;
  justify-content: center;
  margin-bottom: 2rem;
}

.analyze-button {
  padding: 0.75rem 2rem;
  font-size: 1rem;
  font-weight: 600;
  background: #3b82f6;
  border-color: #3b82f6;
}

.analyze-button:hover:enabled {
  background: #2563eb;
  border-color: #2563eb;
}

/* Validation Results */
.validation-results {
  margin-top: 2rem;
}

.results-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.summary-card i {
  font-size: 1.5rem;
  color: #3b82f6;
}

.summary-card.error i {
  color: #ef4444;
}

.summary-card.warning i {
  color: #f59e0b;
}

.summary-card > div {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 600;
}

.summary-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

/* Issues Section */
.issues-section {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border-radius: 8px;
}

.error-section {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.warning-section {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.issues-section h4 {
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
}

.error-section h4 {
  color: #dc2626;
}

.warning-section h4 {
  color: #d97706;
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.issue-item {
  padding: 0.75rem;
  border-radius: 6px;
  background: white;
}

.error-item {
  border-left: 4px solid #ef4444;
}

.warning-item {
  border-left: 4px solid #f59e0b;
}

.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.issue-line {
  font-weight: 600;
  color: #1e293b;
}

.issue-type {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  text-transform: uppercase;
}

.issue-message {
  margin: 0.5rem 0;
  color: #475569;
}

.issue-value {
  margin: 0.5rem 0 0 0;
  font-size: 0.875rem;
  color: #64748b;
}

.issue-value code {
  background: #f1f5f9;
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
  font-family: monospace;
}

/* Suggestions */
.suggestions-section {
  padding: 1rem;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
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

.import-summary {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.import-stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-weight: 600;
}

.import-stat.success {
  background: #dcfce7;
  color: #166534;
}

.import-stat.error {
  background: #fef2f2;
  color: #991b1b;
}

/* Import Errors */
.import-errors {
  margin-top: 1rem;
}

.import-errors h4 {
  margin: 0 0 1rem 0;
  color: #dc2626;
}

.import-errors-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.import-error-item {
  padding: 0.75rem;
  background: #fef2f2;
  border-left: 4px solid #ef4444;
  border-radius: 4px;
}

.error-line {
  font-weight: 600;
  color: #991b1b;
  display: block;
  margin-bottom: 0.5rem;
}

.import-error-item ul {
  margin: 0;
  padding-left: 1.5rem;
}

.import-error-item li {
  color: #7f1d1d;
  margin-bottom: 0.25rem;
}

/* Responsive */
@media (max-width: 768px) {
  .results-summary {
    grid-template-columns: 1fr 1fr;
  }

  .upload-area {
    padding: 2rem 1rem;
  }

  .upload-icon {
    font-size: 2rem;
  }
}
</style>
