# Fichiers de Test CSV - Feature Import CSV

Ce dossier contient des fichiers de test pour valider la fonctionnalité d'import CSV de JManager.

## Format CSV Attendu

Le format CSV doit respecter la structure suivante :
- **5 colonnes** : date, label, depense, recette, tag
- **Séparateur** : virgule (,)
- **Format de date** : dd-MM-yyyy (exemple: 15-01-2025)
- **Montants** : nombres avec point ou virgule comme séparateur décimal (45.50 ou 45,50)
- **Contraintes** :
  - Une seule des colonnes depense ou recette doit être renseignée (pas les deux)
  - Le label est obligatoire (max 200 caractères)
  - La date est obligatoire et doit être valide
  - Les montants ne peuvent pas être négatifs

## Tags Disponibles (par défaut)

- Achat & Shopping
- Alimentation & Restaurant
- Logement & Charges
- Santé
- Transport
- Epargne & Placement
- Aucune

Les tags sont insensibles à la casse et peuvent être vides.

---

## 📁 Dossier OK/ - Cas Valides

### 1. `valid_basic.csv`
Fichier de base avec des transactions simples et correctes.
- Mélange de dépenses et recettes
- Tous les tags par défaut utilisés
- Format standard

### 2. `valid_with_decimal_comma.csv`
Test des montants avec virgule comme séparateur décimal.
- Montants au format : 67,89 au lieu de 67.89
- Validation de la conversion virgule -> point

### 3. `valid_empty_tags.csv`
Transactions sans tag ou avec tag vide.
- Colonnes tag vides
- Doit assigner automatiquement "Aucune"

### 4. `valid_case_insensitive_tags.csv`
Test de la sensibilité à la casse des tags.
- Tags en minuscules, majuscules, mixte
- Exemples : "santé", "TRANSPORT", "epargne & placement"

### 5. `valid_with_spaces.csv`
Validation du trimming des espaces.
- Espaces avant/après les valeurs
- Doit être correctement nettoyé

### 6. `valid_zero_amounts.csv`
Transactions avec montant zéro.
- Dépenses à 0
- Recettes à 0.00

### 7. `valid_extreme_dates.csv`
Test de dates extrêmes mais valides.
- Dates anciennes (2020)
- Dates futures (2030)

### 8. `valid_long_label.csv`
Label à la limite des 200 caractères autorisés.
- Label de ~190 caractères (sous la limite)

### 9. `valid_mixed_with_and_without_tags.csv`
Mélange de transactions avec et sans tags.
- Certaines lignes avec tags
- Certaines lignes sans tags

### 10. `valid_amounts_without_decimals.csv`
Montants entiers sans décimales.
- Exemples : 100, 200, 50.0

### 11. `valid_extreme_amounts.csv`
Test de montants extrêmes.
- Très grands montants : 9999999.99
- Très petits montants : 0.01

### 12. `valid_complete_example.csv`
Exemple complet et réaliste.
- 10 transactions variées
- Tous types de tags
- Mix dépenses/recettes

### 13. `valid_with_special_chars.csv`
Labels avec caractères spéciaux (virgules, guillemets).
- Labels entre guillemets avec virgules
- Guillemets échappés

---

## 📁 Dossier NON_OK/ - Cas Invalides

### 1. `invalid_empty_file.csv`
Fichier complètement vide.
- **Erreur attendue** : "CSV file is empty"

### 2. `invalid_missing_columns.csv`
Fichier avec moins de 5 colonnes.
- Seulement 3 colonnes (date, label, depense)
- **Erreur attendue** : "Expected 5 columns but found 3"

### 3. `invalid_extra_columns.csv`
Fichier avec plus de 5 colonnes.
- 6 colonnes au lieu de 5
- **Erreur attendue** : "Expected 5 columns but found 6"

### 4. `invalid_date_format.csv`
Formats de date incorrects.
- Format américain : 2025-01-15
- Format avec slashes : 15/01/2025
- Mois invalide : 01-13-2025
- **Erreur attendue** : "Format de date invalide. Format attendu: dd-MM-yyyy"

### 5. `invalid_empty_label.csv`
Labels vides ou composés uniquement d'espaces.
- Labels vides
- Labels avec seulement des espaces
- **Erreur attendue** : "Le libellé est obligatoire"

### 6. `invalid_no_amount.csv`
Aucun montant renseigné (ni dépense ni recette).
- Colonnes depense et recette vides
- **Erreur attendue** : "Vous devez renseigner soit une dépense soit une recette"

### 7. `invalid_both_amounts.csv`
Les deux montants renseignés (dépense ET recette).
- Dépense et recette toutes les deux remplies
- **Erreur attendue** : "Vous ne pouvez pas renseigner à la fois une dépense et une recette"

### 8. `invalid_amount_format.csv`
Formats de montant incorrects.
- Texte : "abc"
- Caractères spéciaux : "45€50"
- Format invalide : "45.50.00"
- **Erreur attendue** : "Format de montant invalide"

### 9. `invalid_negative_amounts.csv`
Montants négatifs.
- Dépense négative : -45.50
- Recette négative : -100.00
- **Erreur attendue** : "Le montant ne peut pas être négatif"

### 10. `invalid_missing_date.csv`
Date manquante.
- Colonne date vide
- **Erreur attendue** : "La date est obligatoire"

### 11. `invalid_impossible_dates.csv`
Dates impossibles.
- 29-02-2023 (année non bissextile)
- 32-01-2025 (jour invalide)
- 15-13-2025 (mois invalide)
- **Erreur attendue** : "Format de date invalide"

### 12. `invalid_label_too_long.csv`
Label dépassant 200 caractères.
- Label de plus de 200 caractères
- **Erreur attendue** : "Le libellé ne peut pas dépasser 200 caractères"

### 13. `invalid_inconsistent_columns.csv`
Lignes avec nombre de colonnes variable.
- Certaines lignes avec 3 colonnes, d'autres avec 5
- **Erreur attendue** : Erreurs de parsing

### 14. `invalid_semicolon_separator.csv`
Utilisation de point-virgule au lieu de virgule.
- Séparateur : ; au lieu de ,
- **Erreur attendue** : Erreurs de colonnes manquantes

### 15. `invalid_only_header.csv`
Fichier avec seulement l'en-tête, pas de données.
- Header présent mais aucune ligne de données
- **Erreur attendue** : Validation OK mais 0 transactions

### 16. `invalid_mixed_valid_invalid.csv`
Mélange de lignes valides et invalides.
- Certaines lignes correctes
- Certaines lignes mal formées
- **Erreur attendue** : Erreurs sur les lignes invalides uniquement

### 17. `invalid_wrong_header_names.csv`
Noms de colonnes incorrects dans l'en-tête.
- Colonnes avec majuscules ou accents
- **Erreur attendue** : Erreur de header

### 18. `invalid_excel_file.xlsx`
Fichier Excel au lieu d'un fichier CSV.
- Extension .xlsx
- Contenu binaire Excel
- **Erreur attendue** : Erreurs de parsing CSV

### 19. `invalid_html_file.html`
Fichier HTML avec tableau au lieu d'un CSV.
- Format HTML
- **Erreur attendue** : Erreurs de parsing CSV

### 20. `invalid_json_file.json`
Fichier JSON au lieu d'un fichier CSV.
- Format JSON
- **Erreur attendue** : Erreurs de parsing CSV

### 21. `invalid_xml_file.xml`
Fichier XML au lieu d'un fichier CSV.
- Format XML
- **Erreur attendue** : Erreurs de parsing CSV

---

## Utilisation dans les Tests

```kotlin
val validCsvContent = File("domain/src/test/resources/csv-test-files/OK/valid_basic.csv")
    .readText()

val result = csvImportFeature.validateCsvFile(token, bookletId, validCsvContent)
assertTrue(result.isSuccess())

val invalidCsvContent = File("domain/src/test/resources/csv-test-files/NON_OK/invalid_empty_file.csv")
    .readText()

val result2 = csvImportFeature.validateCsvFile(token, bookletId, invalidCsvContent)
assertTrue(result2.isSuccess())
assertTrue(result2.getOrNull()!!.errors.isNotEmpty()) // Mais contient des erreurs
```

## Structure des Dossiers

```
csv-test-files/
├── README.md (ce fichier)
├── OK/ (13 fichiers - cas valides)
│   ├── valid_basic.csv
│   ├── valid_with_decimal_comma.csv
│   ├── valid_empty_tags.csv
│   ├── valid_case_insensitive_tags.csv
│   ├── valid_with_spaces.csv
│   ├── valid_zero_amounts.csv
│   ├── valid_extreme_dates.csv
│   ├── valid_long_label.csv
│   ├── valid_mixed_with_and_without_tags.csv
│   ├── valid_amounts_without_decimals.csv
│   ├── valid_extreme_amounts.csv
│   ├── valid_complete_example.csv
│   └── valid_with_special_chars.csv
└── NON_OK/ (21 fichiers - cas invalides)
    ├── invalid_empty_file.csv
    ├── invalid_missing_columns.csv
    ├── invalid_extra_columns.csv
    ├── invalid_date_format.csv
    ├── invalid_empty_label.csv
    ├── invalid_no_amount.csv
    ├── invalid_both_amounts.csv
    ├── invalid_amount_format.csv
    ├── invalid_negative_amounts.csv
    ├── invalid_missing_date.csv
    ├── invalid_impossible_dates.csv
    ├── invalid_label_too_long.csv
    ├── invalid_inconsistent_columns.csv
    ├── invalid_semicolon_separator.csv
    ├── invalid_only_header.csv
    ├── invalid_mixed_valid_invalid.csv
    ├── invalid_wrong_header_names.csv
    ├── invalid_excel_file.xlsx
    ├── invalid_html_file.html
    ├── invalid_json_file.json
    └── invalid_xml_file.xml
```

