# REFACTORING_PLAN — Tag Domain Model: Sealed Class Redesign

**Generated on**: 2026-04-30
**Applied pattern**: Domain Model Redesign (Sealed Class) + Use Case Command Fix
**Stack**: Kotlin + Spring Boot 3 + Hexagonal Architecture
**Overall status**: ⏳ To do — Step 0 / 9

---

## Initial Analysis

### What was detected

- `Tag` est une **classe ordinaire avec un flag `isDefault: Boolean`** (par défaut `false`) utilisé comme unique discriminant structurel entre un tag global/système et un tag personnel d'un utilisateur. Toute construction `Tag(label = "x")` sans `isDefault = true` est silencieusement traitée comme personnelle.
- **La régression** : `ConfirmVirtualTransactionCommand` ne transporte qu'un `tagLabel: String?`. Le domaine construit `Tag(label = it)` → `isDefault = false`, `id = null`. Dans `TransactionRepositoryJpaAdapter.save()`, la branche `else` appelle `tagPersonalPostgresRepository.findByIdNullable(null)` → retourne `null` → la transaction est sauvegardée sans tag → la lecture retourne toujours "Aucune".
- `TransactionRepositoryJpaAdapter.save()` suppose qu'un tag non-default a **toujours** un `id` non-null, hypothèse jamais garantie par le domaine.
- `DatasourceMapper.Tag.asResource()` utilise `when(this.isDefault)` — aucune sécurité typée, une erreur sur le flag redirige silencieusement vers la mauvaise table JPA.
- `RegularTransactionEntity` présente exactement le même double-champ (`tag: DefaultTagResource?`, `personalTag: TagPersonalResource?`) et le même pattern de dispatch par flag.
- `JpaTagMapperAdapter.mapToResource()` est la seule méthode qui résout correctement par ID avec conscience du type, mais elle n'est pas utilisée dans le chemin critique `save()`.

### What will NOT be modified

- Le schéma de base de données (tables `default_tag` et `tag_personal` restent séparées).
- Les entités JPA `DefaultTagResource`, `TagPersonalResource`, `AbstractTagResource`.
- Les use cases de gestion des tags (`AddTagUseCase`, `DeleteTagUseCase`, `EditTagUseCase`, `GetAllTagsUseCase`, `DefaultTagUseCase`, `AddDefaultTagsUseCase`) — leur logique métier reste intacte.
- `TagRepository` (port de sortie) — l'interface ne change pas.
- Les tests d'infrastructure et E2E des tags (`TagRepositoryJpaAdapterTest`).
- La logique de `mapToRightTag()` dans `persist()`.

### Justification of the chosen pattern

`Tag.isDefault: Boolean` encode une différence **structurelle** (quelle table JPA utiliser, quelle classe de ressource instancier) via un simple booléen mutable sans garantie. Convertir `Tag` en **sealed class** (`Tag.Default` / `Tag.Personal`) rend la distinction explicite et vérifiée à la compilation — chaque `when(tag)` sans branche `else` devient une erreur de compilation si un sous-type est oublié. C'est la correction minimale qui élimine toute la classe de bugs liée au flag par défaut, sans changer le schéma ni les contrats de ports existants.

---

## Refactoring Steps

---

### Step 1 — Domain: Convertir `Tag` en sealed class

**Status**: ⏳ To do
**Objective**: Remplacer `class Tag(... isDefault: Boolean = false)` par `sealed class Tag` avec deux sous-types `Tag.Default` et `Tag.Personal`. Exposer `val isDefault: Boolean` comme propriété calculée pour assurer la compatibilité des sites d'appel qui en dépendent.
**Blocking for**: Toutes les étapes suivantes

**Actions:**
1. Remplacer le contenu de `domain/src/main/kotlin/fr/sacane/jmanager/domain/models/Tag.kt` :
   ```kotlin
   sealed class Tag(
       open val label: String,
       open val id: UUID? = null,
       open val color: Color = Color(0f, 0f, 0f, 0f)
   ) {
       val isDefault: Boolean get() = this is Default

       data class Default(
           override val label: String,
           override val id: UUID? = null,
           override val color: Color = Color(0f, 0f, 0f, 0f)
       ) : Tag(label, id, color)

       data class Personal(
           override val label: String,
           override val id: UUID? = null,
           override val color: Color = Color(0f, 0f, 0f, 0f)
       ) : Tag(label, id, color)

       companion object {
           fun noneTag(): Default = Default("Aucune", color = Color.WHITE)
       }

       override fun toString(): String = "name: $label\ncolor: (${color.red}, ${color.green}, ${color.blue}, ${color.alpha})"
   }

   val defaultTags: List<Tag.Default> = listOf(
       Tag.Default("Achat & Shopping",           color = Color(1f, 0f, 0f, 1f)),
       Tag.Default("Alimentation & Restaurant",  color = Color(1f, 0.5f, 0f, 1f)),
       Tag.Default("Logement & Charges",         color = Color(0f, 1f, 0f, 1f)),
       Tag.Default("Santé",                      color = Color(0.4f, 0.2f, 0.8f, 1f)),
       Tag.Default("Transport",                  color = Color(1f, 0f, 1f, 1f)),
       Tag.Default("Epargne & Placement",        color = Color(1f, 1f, 0f, 1f)),
       Tag.Default("Aucune",                     color = Color.WHITE),
   )

   fun String.asPersonalTag(color: Color = Color(0f, 0f, 0f, 0f)): Tag.Personal =
       Tag.Personal(this, color = color)
   ```
2. Corriger les erreurs de compilation dans le module `domain` (constructeurs `Tag(...)` directs → `Tag.Default(...)` ou `Tag.Personal(...)`).

**Validation criterion:**
> `:domain:compileKotlin` passe sans erreur. Aucun `Tag(...)` direct ne subsiste dans le module `domain/src/main`.

---

### Step 2 — Domain: Mettre à jour les use cases et commandes

**Status**: ⏳ To do
**Depends on**: Step 1
**Objective**: Corriger tous les sites du module `domain` qui construisent ou référencent un `Tag` directement, notamment les commandes `AddTagCommand`, `EditTagCommand`, et la logique de `AddTagService`.

**Actions:**
1. Dans `AddTagCommand`: le champ `tag: Tag` reste `Tag` (sealed) — aucun changement de signature nécessaire, mais vérifier que le site d'appel construit bien un `Tag.Personal`.
2. Dans `AddTagService.handle()`: la vérification `command.tag.isDefault` reste valide (propriété calculée).
3. Dans `EditTagCommand`: idem, s'assurer que le site d'appel passe un `Tag.Personal`.
4. Vérifier `DeleteTagService` : utilise `tagRepository.defaultTag()` (retourne `Tag`, ok).
5. Parcourir tous les `Tag(label = ..., isDefault = ...)` restants et remplacer par `Tag.Default(...)` ou `Tag.Personal(...)` selon le contexte.

**Validation criterion:**
> `:domain:compileKotlin` passe. Aucune occurrence de `Tag(label` ou `Tag("` dans `domain/src/main`.

---

### Step 3 — Domain: Corriger `ConfirmVirtualTransactionCommand`

**Status**: ⏳ To do
**Depends on**: Step 1
**Objective**: Remplacer `tagLabel: String?` par `tag: Tag?` dans la commande afin que l'infrastructure reçoive un `Tag` typé (avec ID et sous-type correct) plutôt qu'un label nu.

**Actions:**
1. Dans `ConfirmVirtualTransactionCommand`, remplacer :
   ```kotlin
   val tagLabel: String? = null
   ```
   par :
   ```kotlin
   val tag: Tag? = null
   ```
2. Dans `ConfirmVirtualTransactionService.handle()`, remplacer :
   ```kotlin
   val tag = command.tagLabel?.let { Tag(label = it) }
   ```
   par :
   ```kotlin
   val tag = command.tag
   ```
3. Le champ `tag` de `Transaction(...)` reçoit directement `tag`.

**Validation criterion:**
> `:domain:compileKotlin` passe. Le test `ConfirmVirtualTransactionFeatureTest` compile et reste vert.

---

### Step 4 — Domain: Mettre à jour les tests et l'InMemoryTagRepository

**Status**: ⏳ To do
**Depends on**: Steps 1, 2, 3
**Objective**: Corriger `InMemoryTagRepository`, `TagTest`, `TagFeatureTest`, `TransactionFeatureTest` et toutes les constructions de `Tag(...)` dans les tests du module `domain`.

**Actions:**
1. Dans `InMemoryTagRepository`, remplacer `inMemoryDatabase.defaultTags.find { it.label == "Aucune" }!!` → retourne `Tag.Default`, vérifier le retour de `defaultTag()`.
2. Dans `TagTest.kt` : remplacer `Tag("Shopping")` → `Tag.Personal("Shopping")`, `Tag("Alimentation", isDefault = true)` → `Tag.Default("Alimentation")`, etc.
3. Dans `TagFeatureTest.kt` : remplacer toutes les constructions directes.
4. Dans `TransactionFeatureTest.kt` : adapter les constructions dans les tests de `ConfirmVirtualTransactionFeatureTest` — passer `tag = Tag.Default(...)` ou `Tag.Personal(...)` au lieu de `tagLabel`.
5. Dans `FakeFactory` (si applicable) : corriger les constructions de tag.

**Validation criterion:**
> `.\gradlew :domain:test` passe entièrement au vert (0 failure).

---

### Step 5 — Infrastructure: Mettre à jour `DatasourceMapper`

**Status**: ⏳ To do
**Depends on**: Step 1
**Objective**: Remplacer `when(this.isDefault)` par `when(this)` dans `Tag.asResource()` et `AbstractTagResource.toDomain()` pour un dispatch typé et sûr.

**Actions:**
1. Modifier `Tag.asResource()` :
   ```kotlin
   fun Tag.asResource(): AbstractTagResource = when (this) {
       is Tag.Default   -> DefaultTagResource(id, label, Color(color.red, color.green, color.blue))
       is Tag.Personal  -> TagPersonalResource(id, label, Color(color.red, color.green, color.blue))
   }
   ```
2. Modifier `AbstractTagResource.toDomain()` :
   ```kotlin
   fun AbstractTagResource.toDomain(): Tag = when (this) {
       is DefaultTagResource  -> Tag.Default(name, idTag, color.asAwtColor())
       is TagPersonalResource -> Tag.Personal(name, idTag, color.asAwtColor())
   }
   ```
3. Vérifier `Tag.toPersonalTag()` — reste inchangé (construit `TagPersonalResource`).
4. Corriger `DatasourceMapperTest` si des constructions `Tag(... isDefault = ...)` y sont présentes.

**Validation criterion:**
> `:infrastructure:compileKotlin` passe. `DatasourceMapperTest` reste vert.

---

### Step 6 — Infrastructure: Corriger `TransactionRepositoryJpaAdapter.save()`

**Status**: ⏳ To do
**Depends on**: Step 5
**Objective**: Remplacer la résolution basée sur `tag.isDefault` par un dispatch sealed dans `save()`, en gérant proprement le cas `id = null` pour les tags par label.

**Actions:**
1. Réécrire la résolution du tag dans `save()` :
   ```kotlin
   val tagResource: AbstractTagResource? = when (val t = transaction.tag) {
       null             -> tagRepository.findUnknownTag()
       is Tag.Default   -> if (t.id != null) tagRepository.findByIdNullable(t.id)
                           else tagRepository.findByName(t.label)
       is Tag.Personal  -> if (t.id != null) tagPersonalPostgresRepository.findByIdNullable(t.id)
                           else tagPersonalPostgresRepository.findByNameAndOwnerId(t.label, /* résolu via booklet owner si disponible */)
   }
   ```
   > **Note**: pour un `Tag.Personal` sans `id`, une résolution par label+owner est impossible sans le userId. Avec le Step 3, ce cas ne devrait plus se produire (le contrôleur passe toujours un tag avec ID). Traiter ce cas comme `null` (fallback "Aucune") et loguer un warning.
2. Garder la méthode `mapToRightTag()` utilisée dans `persist()` inchangée pour l'instant — elle gère déjà plusieurs cas.

**Validation criterion:**
> `:infrastructure:compileKotlin` passe. `TransactionRepositoryJpaAdapterTest` reste vert.

---

### Step 7 — Application: Mettre à jour `ConfirmVirtualTransactionRequest`

**Status**: ⏳ To do
**Depends on**: Steps 3, 5, 6
**Objective**: Remplacer `tagLabel: String?` par `tagId: UUID?` + `tagIsDefault: Boolean` dans le DTO de requête, permettant au contrôleur de construire un `Tag` typé avant de dispatcher la commande.

**Actions:**
1. Modifier `ConfirmVirtualTransactionRequest` :
   ```kotlin
   data class ConfirmVirtualTransactionRequest(
       // ... champs existants ...
       val tagId: String? = null,          // UUID en string (nullable → pas de tag)
       val tagIsDefault: Boolean = false,
   )
   ```
   > Supprimer le champ `tagLabel`.
2. Dans `Controller.confirmVirtualTransaction()`, construire le tag avant le dispatch :
   ```kotlin
   val resolvedTag: Tag? = request.tagId?.let { rawId ->
       val id = UUID.fromString(rawId)
       if (request.tagIsDefault) Tag.Default(id = id, label = "")
       else Tag.Personal(id = id, label = "")
   }
   ```
   > Le `label` n'a pas besoin d'être renseigné ici — l'infrastructure fera un lookup par ID.
3. Passer `tag = resolvedTag` au lieu de `tagLabel = request.tagLabel` dans `ConfirmVirtualTransactionCommand(...)`.
4. Mettre à jour `ConfirmVirtualTransactionApiTest` (ou équivalent) pour adapter le corps de la requête.

**Validation criterion:**
> `:application:compileKotlin` passe. Les tests API existants pour `POST /api/transaction/virtual/confirm` restent verts.

---

### Step 8 — Frontend: Adapter le payload de confirmation de transaction virtuelle

**Status**: ⏳ To do
**Depends on**: Step 7
**Objective**: Remplacer l'envoi de `tagLabel: string` par `tagId: string` + `tagIsDefault: boolean` dans le composable/composant qui appelle `POST /api/transaction/virtual/confirm`.

**Actions:**
1. Localiser le composable ou la page qui appelle confirm virtual transaction (chercher `tagLabel` dans `client/`).
2. Remplacer le payload :
   ```ts
   // Avant
   tagLabel: tag?.isDefault ? undefined : tag?.label
   // Après
   tagId: tag?.id,
   tagIsDefault: tag?.isDefault ?? false,
   ```
3. Mettre à jour le type `ConfirmVirtualTransactionRequest` côté client si présent dans `client/types/index.d.ts`.
4. Mettre à jour les tests du composable/composant correspondant.

**Validation criterion:**
> `pnpm test` (client) passe au vert. L'appel réseau envoie `tagId` + `tagIsDefault` et non `tagLabel`. Manuellement : confirmer une transaction virtuelle avec un tag personnel → le tag est correctement persisté et retourné par l'API.

---

### Step 9 — Validation finale cross-couches

**Status**: ⏳ To do
**Depends on**: Steps 1–8
**Objective**: S'assurer que la suite complète est verte et qu'aucune régression n'a été introduite.

**Actions:**
1. `.\gradlew :domain:test` → 0 failure.
2. `.\gradlew :infrastructure:test` → 0 failure.
3. `.\gradlew :application:test` → 0 failure.
4. `pnpm test` (dans `client/`) → 0 failure.
5. Test manuel E2E : créer/sélectionner un tag personnel, confirmer une transaction virtuelle, vérifier que le tag persisté correspond.

**Validation criterion:**
> Toutes les suites de tests passent. Une transaction virtuelle confirmée avec un tag personnel (ou défaut) affiche le bon tag dans l'UI et en base de données.

---

## Recommended Execution Order

```
Step 1 (sealed class)
   └─► Step 2 (use cases domain)
   └─► Step 3 (ConfirmVirtualTransactionCommand)
         └─► Step 4 (tests domain)        ← fin du bloc domain
   └─► Step 5 (DatasourceMapper)
         └─► Step 6 (TransactionRepository.save)  ← fin du bloc infra
Steps 4 + 6 rendus verts
   └─► Step 7 (Application DTO + Controller)
         └─► Step 8 (Frontend payload)
               └─► Step 9 (Validation finale)
```

Steps 2 et 5 peuvent être travaillés **en parallèle** après Step 1.
Steps 4 et 6 peuvent être travaillés **en parallèle** (domaine et infrastructure indépendants).
Steps 7 et 8 sont **séquentiels** (l'API doit être stable avant d'adapter le client).

---

## Risk Register

| Risk | Mitigation |
|---|---|
| Sites d'appel `Tag(...)` manqués dans le module `domain` | Le compilateur Kotlin forcera la correction sur tous les constructeurs directs après le changement en sealed class |
| `Tag.Personal` sans `id` passé à l'infra | Step 6 ajoute un fallback + warning loggé ; Step 3+7 éliminent ce cas côté commande |
| Breaking change API (`tagLabel` → `tagId`) | Frontend et backend migrés ensemble dans Steps 7+8 ; aucun autre client externe connu |
| Tests E2E cassés par le changement de DTO | Couverts dans Step 9 — run obligatoire avant de considérer le refactoring terminé |
