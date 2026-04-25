# Patterns de refactoring — Référence

Lire ce fichier pendant la Phase 2 (choix de stratégie) pour identifier le pattern le plus adapté à l'état réel du projet.

---

## Comment choisir

Appliquer dans l'ordre ces questions au code analysé :

1. **Les couches sont-elles clairement séparées ?** (domain / application / infrastructure)
   - Non → commencer par `Package Restructuring` avant tout autre pattern
   - Oui → continuer

2. **Le domaine dépend-il de Spring, JPA, ou d'autres frameworks ?** (`@Entity` dans le domaine, `@Repository` injecté directement dans un service domaine…)
   - Oui → `Hexagonal Architecture` est prioritaire

3. **Des interfaces ou classes regroupent-elles des opérations sans lien fort ?**
   - Oui → `Use Case Split`

4. **Les opérations de lecture et d'écriture partagent-elles des modèles complexes ?**
   - Oui, avec de la complexité → `CQRS`
   - Oui, mais simple → `Use Case Split` suffit

5. **Une classe fait-elle trop de choses sans qu'il s'agisse d'une interface ?**
   - Oui → `Extract Service`

Plusieurs patterns peuvent s'appliquer en séquence. Dans ce cas, les ordonner dans le plan : restructuration des packages → hexagonal → use case split → CQRS si nécessaire.

---

## Use Case Split

**Signal** : Une interface ou classe service contient plusieurs méthodes qui couvrent des opérations distinctes (créer, lire, modifier, supprimer, calculer…).

**Ce qu'on fait** :
- Chaque méthode devient une interface dédiée avec une seule méthode (`execute` ou `invoke`)
- Chaque interface a une implémentation dédiée
- Les appelants n'injectent que le use case dont ils ont besoin

**Structure cible (Kotlin + Spring Boot)** :
```
domain/port/input/
├── CreateXUseCase.kt          // interface { fun execute(cmd: CreateXCommand): X }
├── GetXByIdUseCase.kt         // interface { fun execute(id: UUID): X }
└── DeleteXUseCase.kt          // interface { fun execute(id: UUID) }

application/usecase/
├── CreateXService.kt          // @Service + implements CreateXUseCase
├── GetXByIdService.kt
└── DeleteXService.kt
```

**Ordre des étapes type** :
1. Créer les interfaces use case dans `domain/port/input/`
2. Créer les implémentations dans `application/usecase/`
3. Migrer la logique de l'ancienne implémentation vers les nouvelles classes
4. Mettre à jour les appelants (controllers, autres services)
5. Supprimer l'ancienne interface et implémentation

**Risques** : injection Spring à mettre à jour chez tous les appelants

---

## Hexagonal Architecture (Ports & Adapters)

**Signal** : Des entités JPA (`@Entity`) ou des repositories Spring (`JpaRepository`) sont directement importés dans des classes de la couche domaine ou application. Le domaine n'est pas testable sans Spring.

**Ce qu'on fait** :
- Les ports de sortie (ex : `BudgetRepository`) deviennent des interfaces dans `domain/port/output/`
- Les implémentations JPA vont dans `infrastructure/persistence/`
- Le domaine ne contient plus aucune annotation framework

**Structure cible** :
```
domain/
├── model/                         // entités domaine pures (pas d'@Entity ici)
└── port/
    ├── input/                     // use cases (interfaces)
    └── output/                    // ports de sortie (ex: BudgetRepository)

application/usecase/               // implémentations des use cases

infrastructure/
├── persistence/
│   ├── BudgetJpaRepository.kt     // extends JpaRepository
│   └── BudgetRepositoryAdapter.kt // implements domain BudgetRepository
└── web/
    └── BudgetController.kt        // @RestController, injecte les use cases
```

**Ordre des étapes type** :
1. Créer les interfaces de port de sortie dans `domain/port/output/`
2. Créer les adaptateurs infrastructure qui implémentent ces interfaces
3. Mettre à jour les services pour dépendre des interfaces, pas des implémentations JPA
4. Retirer les annotations JPA du modèle domaine (créer des entités JPA séparées si nécessaire)
5. Vérifier qu'aucun import Spring/JPA ne reste dans `domain/`

**Risques** : si le modèle domaine est aussi l'entité JPA, la séparation est coûteuse — évaluer si c'est pertinent selon la taille du projet

---

## CQRS (Command Query Responsibility Segregation)

**Signal** : Les opérations de lecture retournent des données complexes ou agrégées qui n'ont pas la même forme que les objets utilisés pour écrire. Ou les performances des requêtes de lecture nécessitent des optimisations indépendantes.

**Ce qu'on fait** :
- Les commandes (mutations) ont leurs propres handlers avec leur propre modèle
- Les queries (lectures) ont leurs propres handlers, potentiellement avec des DTOs ou projections optimisées
- Les deux côtés peuvent évoluer indépendamment

**Structure cible** :
```
application/
├── command/
│   ├── CreateBudgetCommand.kt
│   └── CreateBudgetCommandHandler.kt
└── query/
    ├── GetBudgetSummaryQuery.kt
    └── GetBudgetSummaryQueryHandler.kt
```

**Quand NE PAS appliquer** : si les lectures et écritures partagent les mêmes modèles simples et qu'il n'y a pas de problème de performance ou de complexité — `Use Case Split` suffit.

**Ordre des étapes type** :
1. Identifier les opérations qui sont des commandes vs des queries
2. Créer les objets Command et Query
3. Créer les CommandHandlers (extraire la logique d'écriture)
4. Créer les QueryHandlers (extraire la logique de lecture, optimiser si besoin)
5. Supprimer l'ancienne interface unifiée

---

## Extract Service

**Signal** : Une classe (pas une interface) a trop de responsabilités — méthodes sans rapport entre elles, trop de dépendances injectées, fichier de 500+ lignes.

**Ce qu'on fait** :
- Identifier des groupes de méthodes cohérentes
- Extraire chaque groupe dans une classe service dédiée
- L'ancienne classe peut déléguer ou disparaître

**Ordre des étapes type** :
1. Cartographier les groupes de méthodes (par responsabilité)
2. Créer les nouveaux services avec les méthodes correspondantes
3. Migrer la logique méthode par méthode
4. Mettre à jour les appelants
5. Supprimer ou alléger la classe d'origine

---

## Package Restructuring

**Signal** : Les packages sont organisés par type technique (`controller/`, `service/`, `repository/`) plutôt que par feature ou couche architecturale. Difficile de trouver tout ce qui concerne une feature.

**Ce qu'on fait** :
- Réorganiser par couche architecturale (`domain/`, `application/`, `infrastructure/`) ou par feature (`budget/`, `transaction/`)
- Mettre à jour les imports

**Important** : faire ce refactoring **en premier**, avant les autres — déplacer des fichiers est plus simple quand la logique n'a pas encore changé.

**Ordre des étapes type** :
1. Définir la nouvelle structure cible (valider avec le dev)
2. Créer les nouveaux packages
3. Déplacer les fichiers package par package (pas classe par classe)
4. Corriger les imports
5. Vérifier que le projet compile et les tests passent
