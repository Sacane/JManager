<script setup lang="ts">
import useCsvImport from '~/composables/useCsvImport'

const props = defineProps<{
  bookletId: string
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

// Date options for day-only imports
const selectedMonth = ref<string>('')
const selectedYear = ref<number>(new Date().getFullYear())
const useDayOnlyMode = ref(false)

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
  useDayOnlyMode.value = false
  selectedMonth.value = ''
  selectedYear.value = new Date().getFullYear()
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
    // Convertir le mois sélectionné en numéro si le mode jour seul est activé
    let monthNumber: number | undefined
    let yearNumber: number | undefined

    if (useDayOnlyMode.value && selectedMonth.value && selectedYear.value) {
      monthNumber = date.numberFromMonth(date.englishMonth(selectedMonth.value))
      yearNumber = selectedYear.value
    }

    validationReport.value = await validateCsvFile(
      props.bookletId,
      selectedFile.value,
      monthNumber,
      yearNumber
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
    // Convertir le mois sélectionné en numéro si le mode jour seul est activé
    let monthNumber: number | undefined
    let yearNumber: number | undefined

    if (useDayOnlyMode.value && selectedMonth.value && selectedYear.value) {
      monthNumber = date.numberFromMonth(date.englishMonth(selectedMonth.value))
      yearNumber = selectedYear.value
    }

    importResult.value = await importTransactionsFromCsv(
      props.bookletId,
      selectedFile.value,
      true,
      monthNumber,
      yearNumber
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

            <!-- Column Structure -->
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

            <!-- Important Rules -->
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

            <!-- Tags disponibles -->
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

        <!-- Day-Only Mode Selection -->
        <div class="day-only-mode-section">
          <div class="mode-toggle">
            <label class="mode-toggle-label">
              <input
                v-model="useDayOnlyMode"
                type="checkbox"
                class="mode-checkbox"
              >
              <span class="mode-text">
                <i class="pi pi-calendar" />
                Mon CSV contient uniquement des jours (sans mois ni année)
              </span>
            </label>
          </div>

          <Transition name="slide-fade">
            <div v-if="useDayOnlyMode" class="date-selectors">
              <div class="date-info">
                <i class="pi pi-info-circle" />
                <p>Si votre fichier CSV contient seulement des jours (ex: 1, 15, 28), spécifiez le mois et l'année correspondants.</p>
              </div>

              <div class="date-inputs">
                <div class="date-input-group">
                  <label for="monthSelect">Mois :</label>
                  <select
                    id="monthSelect"
                    v-model="selectedMonth"
                    class="date-select"
                  >
                    <option value="" disabled>Sélectionnez un mois</option>
                    <option v-for="(month, index) in date.months" :key="index" :value="date.translate(month)">
                      {{ date.translate(month) }}
                    </option>
                  </select>
                </div>

                <div class="date-input-group">
                  <label for="yearSelect">Année :</label>
                  <input
                    id="yearSelect"
                    v-model.number="selectedYear"
                    type="number"
                    min="2000"
                    :max="new Date().getFullYear() + 10"
                    class="date-input"
                  >
                </div>
              </div>

              <div v-if="selectedMonth && selectedYear" class="date-example">
                <i class="pi pi-lightbulb" />
                <span>Exemple : "15" dans votre CSV deviendra le 15 {{ selectedMonth }} {{ selectedYear }}</span>
              </div>
            </div>
          </Transition>
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

/* Format Helper Section */
.format-helper-section {
  margin-bottom: 2rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.format-helper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.format-helper-header:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #6b3fa0 100%);
}

.format-helper-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 600;
  font-size: 1rem;
}

.format-helper-title i {
  font-size: 1.25rem;
}

.format-helper-header > i {
  transition: transform 0.3s ease;
}

/* CSV Example Standalone - Always visible */
.format-example-standalone {
  padding: 1.25rem;
  background: #fafafa;
  border-top: 1px solid #e2e8f0;
}

.format-example-standalone .format-example-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.format-example-standalone .format-example-header h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  color: #1e293b;
  font-size: 1rem;
  font-weight: 600;
}

.format-example-standalone .format-example-header h4 i {
  color: #667eea;
}

.format-example-standalone .csv-preview {
  background: #1e293b;
  border-radius: 6px;
  padding: 1rem;
  overflow-x: auto;
  margin-bottom: 1rem;
}

.format-example-standalone .csv-preview pre {
  margin: 0;
}

.format-example-standalone .csv-preview code {
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  color: #e2e8f0;
  line-height: 1.6;
}

.format-example-standalone .template-hint {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  color: #166534;
  font-size: 0.875rem;
  margin: 0;
}

.format-example-standalone .template-hint i {
  color: #16a34a;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.format-helper-content {
  padding: 1.5rem;
  background: #fafafa;
}

.format-intro {
  margin-bottom: 1.5rem;
}

.format-intro p {
  color: #475569;
  font-size: 0.95rem;
  margin: 0;
}

/* Column Structure */
.format-structure {
  margin-bottom: 1.5rem;
  padding: 1.25rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.format-structure h4,
.format-example h4,
.format-rules h4,
.format-tags h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 1rem 0;
  color: #1e293b;
  font-size: 1rem;
  font-weight: 600;
}

.format-structure h4 i,
.format-example h4 i,
.format-rules h4 i,
.format-tags h4 i {
  color: #667eea;
}

.columns-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.column-card {
  padding: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.column-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
  border-color: #667eea;
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
  font-weight: 700;
  color: #1e293b;
  font-family: 'Courier New', monospace;
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
  font-size: 0.85rem;
}

.columns-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  color: #92400e;
  font-size: 0.875rem;
  margin: 0;
}

.columns-note i {
  color: #f59e0b;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

/* Format Rules */
.format-rules {
  margin-bottom: 1.5rem;
  padding: 1.25rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.rules-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.rules-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  color: #475569;
  font-size: 0.9rem;
}

.rules-list li i {
  color: #10b981;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

/* Format Tags */
.format-tags {
  padding: 1.25rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tags-intro {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0 0 1rem 0;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tag-chip {
  padding: 0.4rem 0.8rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.2);
  transition: all 0.2s ease;
}

.tag-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.tags-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  color: #0c4a6e;
  font-size: 0.875rem;
  margin: 0;
}

.tags-note i {
  color: #0284c7;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

/* Slide Fade Transition */
:deep(.slide-fade-enter-active),
:deep(.slide-fade-leave-active) {
  transition: all 0.3s ease;
  max-height: 2000px;
  overflow: hidden;
}

:deep(.slide-fade-enter-from),
:deep(.slide-fade-leave-to) {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
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

/* Day-Only Mode Section */
.day-only-mode-section {
  margin: 1.5rem 0;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.mode-toggle {
  margin-bottom: 1rem;
}

.mode-toggle-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  user-select: none;
}

.mode-checkbox {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}

.mode-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  color: #1e293b;
  font-weight: 500;
}

.mode-text i {
  color: #667eea;
}

.date-selectors {
  padding: 1rem;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.date-info {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #dbeafe;
  border-radius: 6px;
  margin-bottom: 1rem;
}

.date-info i {
  color: #2563eb;
  margin-top: 0.125rem;
  flex-shrink: 0;
}

.date-info p {
  margin: 0;
  color: #1e40af;
  font-size: 0.875rem;
  line-height: 1.5;
}

.date-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

.date-input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.date-input-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #475569;
}

.date-select,
.date-input {
  padding: 0.625rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  transition: all 0.2s;
}

.date-select:focus,
.date-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.date-select:hover,
.date-input:hover {
  border-color: #94a3b8;
}

.date-example {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #fef3c7;
  border-radius: 6px;
  border: 1px solid #fde047;
}

.date-example i {
  color: #f59e0b;
  flex-shrink: 0;
}

.date-example span {
  color: #92400e;
  font-size: 0.875rem;
  font-weight: 500;
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
