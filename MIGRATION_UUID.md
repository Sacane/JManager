# Migration des IDs de Long vers UUID

## Résumé
Cette migration transforme tous les identifiants des entités du domaine de `Long` vers `UUID` pour améliorer la sécurité et la scalabilité de l'application.

## Entités migrées vers UUID

### Domaine (domain/)
- **User** : `UserId(Long?)` → `UserId(UUID?)`
- **Booklet** : `id: Long?` → `id: UUID?`
- **Tag** : `id: Long?` → `id: UUID?`
- **Transaction** : `id: Long?` → `id: UUID?`
- **RegularTransactionTracker** : `bookletId: Long` → `bookletId: UUID`

### Infrastructure - Entités JPA (infra/spi/entity/)
- **UserResource** : `idUser: Long?` → `idUser: UUID?` avec `@GeneratedValue(strategy = GenerationType.UUID)`
- **BookletResource** : `idAccount: Long?` → `idAccount: UUID?` avec `@GeneratedValue(strategy = GenerationType.UUID)`
- **TransactionResource** : `idSheet: Long?` → `idSheet: UUID?` avec `@GeneratedValue(strategy = GenerationType.UUID)`
- **AbstractTagResource** : `idTag: Long?` → `idTag: UUID?` avec `@GeneratedValue(strategy = GenerationType.UUID)`
- **DefaultTagResource** : utilise UUID
- **TagPersonalResource** : utilise UUID
- **RegularTransactionTrackerEntity** : `bookletId: Long` → `bookletId: UUID`
- **MonthlyRegularTransactionEntity** : déjà en UUID ✓

### Entités conservant Long (tables de jointure)
- **FrequencyPropertyEntity** : `id: Long` (table de jointure)
- **RegularTransactionTrackerEntity** : `id: Long` (auto-increment pour la table de tracking)

## Modifications des Ports (domain/port/)

### SPI (Infrastructure)
- **BookletRepositoryPort** :
  - `findAccountByIdWithTransactions(Long)` → `findAccountByIdWithTransactions(UUID)`
  - `deleteAccountById(Long)` → `deleteAccountById(UUID)`
  
- **TransactionRepositoryPort** :
  - `deleteAllSheetsById(List<Long>)` → `deleteAllSheetsById(List<UUID>)`
  - `findTransactionById(Long)` → `findTransactionById(UUID)`
  - `save(Long, Transaction)` → `save(UUID, Transaction)`
  - `findAccountWithTransactionById(Long)` → `findAccountWithTransactionById(UUID)`
  - `findTransactionsByBookletId(Long)` → `findTransactionsByBookletId(UUID)`
  - `findTransactionsByBookletYearAndMonth(Long, Int, Month)` → `findTransactionsByBookletYearAndMonth(UUID, Int, Month)`

- **RegularTransactionRepository** :
  - `getAllRegularUsedByAccount(UserId, Long)` → `getAllRegularUsedByAccount(UserId, UUID)`
  - `saveMonthlyRegularTransaction(UserId, MonthlyTransaction, List<Long>)` → `saveMonthlyRegularTransaction(UserId, MonthlyTransaction, List<UUID>)`

- **RegularTransactionTrackerRepository** :
  - `findTracker(RegularTransactionId, Long)` → `findTracker(RegularTransactionId, UUID)`
  - `findAllTrackersForBooklet(Long)` → `findAllTrackersForBooklet(UUID)`
  - `deleteTrackerByBookletId(Long)` → `deleteTrackerByBookletId(UUID)`

### API (Application)
- **BookletFeature** :
  - `findAccountById(Long, String)` → `findAccountById(UUID, String)`
  - `deleteAccountById(Long, String)` → `deleteAccountById(UUID, String)`
  - `loadTransactionsForBookletForAMonth(String, Long, Month, Int)` → `loadTransactionsForBookletForAMonth(String, UUID, Month, Int)`

- **TransactionFeature** :
  - `editTransaction(Long, Transaction, String)` → `editTransaction(UUID, Transaction, String)`
  - `findById(Long, String)` → `findById(UUID, String)`
  - `deleteSheetsByIds(Long, List<Long>, String)` → `deleteSheetsByIds(UUID, List<UUID>, String)`
  - `confirmPreviewTransaction(String, Long, Long)` → `confirmPreviewTransaction(String, UUID, UUID)`

- **RegularTransactionFeature** :
  - `bookRegularTransaction(String, RegularTransaction, List<Long>)` → `bookRegularTransaction(String, RegularTransaction, List<UUID>)`

## Modifications de l'API REST (infra/api/)

### DTOs modifiés
- **UserDTO** : `id: Long` → `id: String`
- **UserStorageDTO** : `id: Long?` → `id: String?`
- **AccountDTO** : `id: Long?` → `id: String?`
- **TagDTO** : `tagId: Long` → `tagId: String?`
- **TransactionResult** : `id: Long?` → `id: String?`
- **UserAccountIdsTransactionRequest** : `accountId: Long` → `accountId: String`
- **AccountTransactionsIdRequest** : `accountId: Long`, `transactionIds: List<Long>` → `accountId: String`, `transactionIds: List<String>`
- **MonthlyRegularTransactionRequest** : `bookletIds: List<Long>` → `bookletIds: List<String>`
- **RegularTransactionLinkRequest** : `bookletId: Long` → `bookletId: String`
- **ConfirmPreviewCommand** : `accountID: Long`, `transactionID: Long` → `accountID: String`, `transactionID: String`

### Contrôleurs modifiés
Tous les contrôleurs convertissent maintenant les String en UUID avec `UUID.fromString()` :
- **TransactionController** : endpoints `/api/transaction`
- **BookletController** : endpoints `/api/booklet` (supposé)
- Conversion des paramètres de requête et body

## Repositories JPA (infra/spi/repositories/)

Tous les repositories JPA ont été mis à jour pour utiliser `UUID` :
- **UserPostgresRepository** : `CrudRepository<UserResource, UUID>`
- **BookletJpaRepository** : `CrudRepository<BookletResource, UUID>`
- **TransactionJpaRepository** : `CrudRepository<TransactionResource, UUID>`
- **DefaultTagPostgresRepository** : `CrudRepository<DefaultTagResource, UUID>`
- **TagPersonalPostgresRepository** : `CrudRepository<TagPersonalResource, UUID>`

## Adapters (infra/spi/adapters/)

Mis à jour pour utiliser UUID :
- **UserRepositoryJpaAdapter**
- **BookletJpaRepositoryAdapter**
- **TransactionRepositoryJpaAdapter**
- **RegularTransactionRepositoryDataJpaAdapter**
- **RegularTransactionOperator**

## Mappings (infra/api/ et infra/spi/adapters/utils/)

### ApiMappingExtensions.kt
- Conversion `User.toDTO()` : `id.value ?: 0` → `id.value?.toString() ?: ""`
- Conversion `String.id()` : `UserId(this)` → `UserId(UUID.fromString(this))`
- Conversion `Tag.toDTO()` : `id!!` → `id?.toString()`
- Conversion entre Transaction et TransactionResult avec UUID.fromString()

### DatasourceMapper.kt
Pas de modifications majeures requises car il utilise déjà les types des entités

## Migration de la base de données (Flyway)

### Fichier : V4__migrate_ids_to_uuid.sql

**⚠️ ATTENTION : Cette migration supprime toutes les données existantes !**

La migration effectue les actions suivantes :
1. Suppression des contraintes de clés étrangères
2. Suppression des tables de jointure (seront recréées)
3. TRUNCATE de toutes les tables dépendantes
4. Suppression des séquences BIGINT
5. Modification des colonnes ID de BIGINT vers UUID avec `gen_random_uuid()`
6. Recréation des tables de jointure avec UUID
7. Recréation des contraintes de clés étrangères

### Tables modifiées :
- `user_resource` : `id_user BIGINT` → `id_user UUID`
- `default_tag_resource` : `id_tag BIGINT` → `id_tag UUID`
- `tag_personal_resource` : `id_tag BIGINT` → `id_tag UUID`
- `account` : `id_account BIGINT` → `id_account UUID`
- `sheet` : `id_sheet BIGINT` → `id_sheet UUID`
- `regular_transaction_tracker` : `booklet_id BIGINT` → `booklet_id UUID`

### Colonnes de clés étrangères modifiées :
- Toutes les colonnes de FK référençant les tables ci-dessus

## Avantages de cette migration

1. **Sécurité** : Les UUID sont non séquentiels, empêchant la prédiction d'IDs
2. **Scalabilité** : Pas de point de contention sur les séquences en environnement distribué
3. **Confidentialité** : Masque le nombre d'entités dans la base
4. **Génération côté application** : Possibilité de générer les IDs avant insertion
5. **Fusion de bases** : Pas de collision d'IDs lors de fusions de bases de données

## Points d'attention

1. **Performance** : Les UUID (16 bytes) sont plus lourds que les BIGINT (8 bytes)
2. **Index** : Peut fragmenter les index B-tree (utiliser UUID v7 pour améliorer cela)
3. **Lisibilité** : Les UUID sont moins lisibles que les nombres en debug
4. **Taille** : Augmente légèrement la taille de la base de données

## Tests à effectuer

- [ ] Créer un utilisateur
- [ ] Créer un booklet
- [ ] Créer des transactions
- [ ] Créer des tags personnalisés
- [ ] Créer des transactions régulières mensuelles
- [ ] Éditer une transaction
- [ ] Supprimer une transaction
- [ ] Supprimer un booklet
- [ ] Charger les transactions pour un mois donné
- [ ] Vérifier les trackers de transactions régulières

## Prochaines étapes recommandées

1. **Backup** : Faire un backup complet avant d'exécuter la migration
2. **Tests** : Exécuter tous les tests unitaires et d'intégration
3. **Frontend** : Mettre à jour le client Nuxt.js pour gérer les UUID en String
4. **Documentation** : Mettre à jour la documentation API
5. **Monitoring** : Surveiller les performances après migration

