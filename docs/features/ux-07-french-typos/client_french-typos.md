# Client Module — Fix the French typos visible to end users

**Context**
Several user-facing labels are missing accents or contain grammar mistakes, which undermines the
perceived quality of the product. They are concentrated in the settings page, the transaction dialog
and the booklet deletion confirmation.

**Acceptance Criteria**
Feature: Correct French wording
  In order to trust the product
  As a French-speaking user
  I want the interface wording to be spelled correctly

Scenario: 1. The settings page wording is corrected
  Given I open the settings page
  When the page is rendered
  Then the labels read "Parametres utilisateur" with its accents, "Definis" with its accent, "previsions" with its accent and "Fenetre de projection (7 a 60 jours)" with its accents

Scenario: 2. The transaction dialog wording is corrected
  Given I open the transaction creation dialog
  When the dialog is rendered
  Then the type label reads "Selectionner" with its accent

Scenario: 3. The booklet deletion message is corrected
  Given I ask to delete a booklet
  When the confirmation dialog is displayed
  Then the message reads "Cette action est irreversible" and "toutes les transactions enregistrees"

**Notes**
- Corrections: Parametres -> Paramètres, Definis -> Définis, previsions -> prévisions,
  Fenetre -> Fenêtre, "7 a 60" -> "7 à 60", Selectionner -> Sélectionner,
  "Cette action et irréversible" -> "Cette action est irréversible",
  "transactions enregistrés" -> "transactions enregistrées".
- Files: `pages/settings/index.vue`, `components/dialog/TransactionCreationDialog.vue`, `pages/booklet/index.vue`.
- Priority P0 - Effort XS - Frontend only.
