## Plan: Réduire la latence “beaucoup de transactions”

Objectif: identifier puis supprimer les goulots d’étranglement quand un livret contient beaucoup de transactions. Côté backend, le problème principal est le chargement “gros agrégat” (JOIN FETCH sur toutes les sheets/tags) puis des filtres/tri/calculs en mémoire (notamment `Booklet.retrieveSheetSurroundAndSortedByDate` et `calculatePrevisionalSold` qui balaie `booklet.transactions`). Côté frontend, la page recharge l’intégralité des transactions du mois à chaque interaction (changement mois/année, suppression, confirmation) et retrie côté client. Le plan priorise d’abord la mesure, puis des quick wins, puis une refonte d’endpoints + pagination/réduction de payload.

### Steps
1. **Instrumenter le backend (mesures de latence & volume)** sur l’endpoint `GET api/account/report/{accountID}` dans `infra/src/main/kotlin/fr/sacane/jmanager/infrastructure/api/booklet/Controller.kt` et le flux `BookletFeatureImpl.loadTransactionsForBookletForAMonth` dans `domain/src/main/kotlin/fr/sacane/jmanager/domain/port/api/BookletFeature.kt`.
2. **Diagnostiquer/optimiser la requête JPA** autour de `BookletJpaRepository.findByIdWithSheets` dans `infra/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/repositories/BookletJpaRepository.kt` (JOIN FETCH, cardinalité, doublons, taille du graphe chargé).
3. **Déporter filtre/tri au niveau base** (mois/année, tri), éviter les parcours full-scan en mémoire (`retrieveSheetSurroundAndSortedByDate`, `calculatePrevisionalSold`) via requêtes dédiées et/ou projections DTO.
4. **Refondre le contrat d’API “report”**: endpoint(s) paginés + endpoint(s) “sold only”, et stratégies de cache côté serveur (ETag/Cache-Control) + côté client (memoization par {accountId, month, year}).
5. **Optimiser le frontend Nuxt/Vue**: réduire les reload complets, éviter le tri/redondance, virtualiser l’affichage, et rendre la suppression/confirmation “incrementales” sans recharger tout.
6. **Valider par charge réaliste**: dataset volumineux, objectif p95/p99, et comparaison avant/après sur 2–3 scénarios typiques.

### Further Considerations
1. La priorité est-elle **réactivité UI** (temps perçu) ou **coût serveur** (CPU/DB) ? Ça influence pagination vs cache agressif.
2. Voulez-vous conserver la sémantique actuelle “report = transactions + sold” ou séparer strictement “liste” et “sold” (mieux pour perf, API plus simple) ?


## Détails actionnables (backend)

### 1) Mesures à ajouter (immédiat, faible effort, gros ROI)
- **Découper la latence en sous-étapes** dans `loadTransactionsForBookletForAMonth`:
  - temps `findAccountByIdWithTransactions` (DB + mapping)
  - temps génération/filtrage des preview (incl. `trackerRepository.findTracker` si appelé en boucle)
  - temps tri/partition (`retrieveSheetSurroundAndSortedByDate`)
  - temps calcul `calculatePrevisionalSold`
  - taille des collections: `booklet.transactions.size`, nb transactions du mois, nb preview virtuelles, nb regularTransactions
- **Activer slow query log / statistiques Hibernate** (selon config Spring) pour repérer:
  - requêtes > X ms
  - nombre de requêtes (pour éviter N+1, surtout autour de tags)
  - taille résultat (lignes renvoyées, risque de “cartesian product” avec `JOIN FETCH` sur multiples relations)
- **Ajouter un identifiant de requête** (correlation id) dans logs afin de relier front/back.

Risques/efforts: très faible, sans changement fonctionnel.  
Priorité: P0.

---

### 2) Causes probables de latence (constats + code)
- **Chargement trop large de l’agrégat**:
  - `BookletJpaRepository.findByIdWithSheets` fait `LEFT JOIN FETCH acc.sheets s LEFT JOIN FETCH s.personalTag LEFT JOIN FETCH s.tag`.
  - Si un livret a N transactions, la requête ramène N lignes * (multiplicités tag/personalTag) → gros transfert DB→app + coût Hibernate (dédup, hydration).
- **Full-scan en mémoire**:
  - `Booklet.retrieveSheetSurroundAndSortedByDate(month, year)` filtre puis trie sur `transactions` (donc sur toutes si la collection est déjà chargée).
  - `calculatePrevisionalSold` scanne `booklet.transactions` et filtre sur une plage de dates.
- **Risque N+1 via trackers**:
  - le filtrage “excluded months” appelle `trackerRepository.findTracker(...)` potentiellement **par transaction preview** (boucle → multiples requêtes).
- **Réponse trop lourde**:
  - `BookletReport` renvoie la liste des transactions + soldes. Ça force un gros payload même si l’UI n’affiche qu’une vue paginée (ou si l’utilisateur scrolle peu).

---

### 3) Quick wins backend (faible effort, impact élevé)
- **Éviter `JOIN FETCH` systématique des tags si non nécessaires**:
  - Si l’écran a besoin du tag label/couleur, garder une forme légère via projection DTO plutôt que fetch complet d’entités.
- **Ajouter une requête “transactions du mois” directement** (au niveau repository) pour ne pas charger toutes les transactions:
  - filtrage DB: `WHERE booklet_id = :id AND date >= :from AND date < :to`
  - tri DB: `ORDER BY date ASC, last_modified ASC` (ou l’ordre voulu)
- **Indexer la table transactions** (DB):
  - index composite recommandé: `(booklet_id, date)` + éventuellement `(booklet_id, is_preview, date)`
  - si tri sur `lastModified`: inclure `last_modified` (selon SGBD et plan)
- **Réduire les appels tracker**:
  - charger en une fois les trackers nécessaires (par bookletId + regularTransactionIds) et mettre en map en mémoire.
  - ou pré-calculer une structure d’exclusion (set) sans requête par transaction.

Risques/efforts: faible à moyen (touches repo + mapping DTO).  
Priorité: P0–P1.

---

### 4) Refonte endpoints (moyen effort, durable)
Proposition de découpage (à discuter):
- **Endpoint A: “soldes uniquement”**  
  - `GET api/account/{id}/balances?month=&year=` → renvoie `realSold`, `previewSold`, + métadonnées (comptes, nb transactions mois, etc.)
  - permet de rafraîchir rapidement après delete/confirm sans recharger la liste.
- **Endpoint B: “liste transactions paginée”**  
  - `GET api/account/{id}/transactions?from=&to=&cursor=&limit=&includePreview=` (ou `month/year`)
  - renvoie une page + `nextCursor` (pagination curseur préférable à offset si beaucoup de données).
- **Option: endpoint C “delta après mutation”**  
  - après `delete` / `confirm`, renvoyer directement:
    - soldes (nouveaux)
    - ids supprimés / transaction confirmée
  - évite `loadBookletData()` complet.

Risques/efforts: moyen (contrats API + adaptation FE + rétrocompat).  
Priorité: P1.

---

### 5) Cache backend (faible à moyen, selon infra)
- **ETag / If-None-Match** sur listes de transactions mensuelles et/ou sur report:
  - ETag basé sur (bookletId, month, year, max(lastModified), count) côté serveur.
- **Cache local “read-mostly”** (Caffeine) pour `report` par {id, month, year} avec TTL court (ex: 30–120s) + invalidation sur mutation (create/edit/delete/confirm).
- **Compression HTTP** (gzip/br) si pas déjà activée.

Risques: invalidation à bien maîtriser pour éviter données périmées.  
Priorité: P1 (ETag) / P2 (cache applicatif).


## Détails actionnables (frontend Nuxt/Vue)

### 1) Causes probables de latence FE (avec vos constats)
- `client/pages/account/[id].vue`:
  - `loadBookletData()` appelle `findByIdMonthAndYear` à chaque:
    - changement mois (`onMonthChange`)
    - changement année (`onYearChange`)
    - suppression (`confirmDelete` → reload)
    - confirmation preview (`confirmPreview` → reload)
    - import CSV success (`onCsvImportSuccess` → reload)
  - **tri côté client** à chaque chargement (`sort`), potentiellement coûteux si beaucoup d’items.
- DataTable:
  - beaucoup de lignes + rendu complet = coût DOM (surtout sans virtual scroll).
- Payload API:
  - si le backend renvoie beaucoup de champs (tags inclus), le parsing JSON + mapping + tri prend du temps.

---

### 2) Quick wins frontend (faible effort)
- **Debounce / éviter les double fetch** mois+année:
  - lorsque l’utilisateur change année via datepicker, éviter d’appeler `loadBookletData()` plusieurs fois pendant la sélection.
- **Supprimer le tri redondant si l’API renvoie déjà trié**:
  - décider “source of truth”: backend trié → FE ne trie plus.
- **Optimiser l’après-mutation**:
  - après delete: retirer localement les transactions supprimées + mettre à jour soldes depuis la réponse mutation ou endpoint “balances-only”.
  - après confirm preview: remplacer l’item local + maj soldes, sans reload.
- **Virtualiser la table** (PrimeVue DataTable supporte virtual scroll selon version):
  - affichage rapide même si 5k+ lignes.

Priorité: P0–P1.

---

### 3) Évolution UI/UX (moyen effort, améliore le “perceived performance”)
- **Pagination / “Load more” / infinite scroll**:
  - charger seulement 50–200 transactions d’abord, puis suite au scroll.
- **Skeleton/loading progressif**:
  - afficher soldes et header immédiatement, puis la liste ensuite.
- **Préchargement du mois voisin** (optionnel):
  - si navigation mois ±1 est fréquente, préfetch discret en idle.

Priorité: P1–P2.


## Priorisation (impact vs effort)

### P0 (1–2 jours, impact immédiat)
- Ajouter métriques/logs de timing + volumes sur `report`.
- Réduire N+1 tracker (batch load / map cache).
- Ajouter index DB `(booklet_id, date)` (et ajuster requête).
- Côté FE: éviter reload complet après delete/confirm si possible (au minimum éviter tri/re-render inutiles + debounce).

### P1 (3–7 jours, amélioration structurelle)
- Requête dédiée “transactions du mois” (DB filter + sort), plus chargement agrégat complet.
- API séparée balances vs liste, et suppression de `JOIN FETCH` multi-relations au profit de projections DTO.
- Virtual scroll / pagination côté FE.

### P2 (1–3 semaines, robustesse long terme)
- Pagination curseur + contrat “delta” après mutations.
- Cache (ETag + cache applicatif avec invalidation).
- Refonte de calcul `previsionalSold` pour éviter full-scan (pré-agrégation DB, ou calcul incrémental/stocké).


## Risques & points de vigilance
- **Changement de contrat API**: nécessite versioning ou compat (feature flag / endpoints additionnels).
- **Cohérence des soldes**: si on passe en incrémental côté FE, il faut une source backend fiable (balances endpoint ou réponse mutation enrichie).
- **JOIN FETCH + pagination**: attention, JPA/Hibernate gère mal pagination + fetch join sur collections; préférer 2 requêtes (ids puis fetch) ou projections.
- **Indices**: à valider avec le SGBD réel (Postgres/MySQL/etc.) et les plans d’exécution.
- **Volume tags**: si tags sont `ManyToOne`, ok; si `OneToMany`, risque de cartesian product amplifié.

--- 

Si tu veux, je peux refaire ce plan en mode “ADR” (décision d’architecture) avec 2–3 options d’API (conserver `/report` vs split endpoints vs pagination cursor) et leurs compromis.

