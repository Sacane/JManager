# Testing Guidelines

Guidelines de test pour le projet JManager.

## 1. Objectif et scope

Ce document definit la strategie de test Kotlin pour garantir:
- fiabilite des regles metier
- securite des evolutions
- feedback rapide pendant le developpement

La strategie suit l'architecture du projet:
- `domain`: tests unitaires metier purs
- `application`: tests des use cases et de l'orchestration
- `infrastructure`: tests d'integration des adaptateurs techniques

## 2. Principes directeurs

- Prioriser la validation des regles metier de `FEATURES.md`.
- Ecrire des tests deterministes (pas d'heure systeme non controlee, pas de hasard implicite).
- Tester le comportement observable, pas l'implementation interne.
- Garder des tests lisibles, courts et centres sur une intention.
- Tout bug corrige doit ajouter au moins un test de non-regression.

## 3. Pyramide de tests adaptee au projet

- Base majoritaire: tests unitaires dans `domain`.
- Niveau intermediaire: tests applicatifs dans `application` avec doubles de test (fakes/stubs).
- Sommet cible: tests d'integration dans `infrastructure` sur les frontieres techniques.

Regle pratique:
- un changement metier commence par un test dans `domain`
- un changement de flux ajoute des tests dans `application`
- un changement de connecteur technique ajoute des tests dans `infrastructure`

## 4. Conventions par module

### 4.1 Domain tests

- Cible: entites, value objects, services metier, transitions d'etat.
- Aucune dependance framework.
- Aucun acces I/O (DB, HTTP, filesystem).
- Verifier explicitement les invariants.

### 4.2 Application tests

- Cible: use cases, orchestration des ports, mapping des resultats, connecteurs REST.
- Utiliser des fakes/stubs pour les ports sortants (repository, notification, etc.).
- Verifier:
  - Les retours et input de l'API Rest (code http..)
  - resultat du use case
  - effets de bord attendus (appel port, evenement, persistance)
  - absence d'effet de bord en cas d'echec metier

### 4.3 Infrastructure tests

- Cible: adaptateurs concrets (persistance, clients externes, messagerie).
- Preferer des tests d'integration realistes plutot que des mocks lourds.
- Verifier les mappings:
  - technique -> metier
  - metier -> technique
- Les details de resilience (timeout, retry) sont testes ici.

## 5. Structure et nommage des tests

- Un fichier de test par type principal teste.
- Noms de tests explicites, orientes comportement.
- Format recommande: `shouldExpectedBehavior_whenContext`.

Exemples:
- `shouldRefuseOrder_whenTokensAreInsufficient`
- `shouldCancelOrder_whenOrderIsNotAcknowledged`
- `shouldReturnFailure_whenRepositoryIsUnavailable`

## 6. Donnees de test et fixtures

- Centraliser les fixtures partagees pour eviter la duplication.
- Utiliser des builders/factories de test avec valeurs par defaut explicites.
- Eviter les fixtures trop riches quand seul un sous-ensemble est utile.
- Toute donnee temporelle doit etre fixe (Clock de test ou timestamp constant).

## 7. Assertions et doubles de test

- Verifier les outcomes metier avant les interactions.
- Eviter la sur-verification des appels (tester l'essentiel, pas chaque detail interne).
- N'utiliser des mocks stricts que si l'interaction est une exigence fonctionnelle.
- Preferer des fakes simples quand le comportement est facile a simuler.

## 8. Flakiness et stabilite

- Interdire `Thread.sleep` dans les tests applicatifs/metier.
- Interdire les dependances implicites a l'ordre d'execution des tests.
- Chaque test doit etre independant et executable isolement.
- Les tests doivent passer de maniere identique en local et en CI.

## 9. Commandes de validation

- Suite complete:
  - `./gradlew test`
- Module cible:
  - `./gradlew :domain:test`
  - `./gradlew :application:test`
  - `./gradlew :infrastructure:test`

## 10. Definition of Done (testing)

Une evolution est consideree complete si:
- les regles metier impactees sont couvertes par des tests
- les cas d'echec metier importants sont testes
- les tests passent sur le module concerne et sur la suite complete
- aucun test flaky n'est introduit
- un test de non-regression est ajoute pour chaque bug corrige

## 11. Approche TDD (Red, Green, Refactor)

Le TDD est la methode recommandee pour implementer les regles metier et limiter les regressions.

### 11.1 Red: ecrire un test qui echoue

- Ecrire d'abord un test exprimant une regle metier ou un bug concret.
- Verifier que le test echoue pour la bonne raison.
- Le test Red doit etre petit, cible, et comprehensible en lecture rapide.
- Commencer au niveau le plus bas possible:
  - `domain` pour une regle metier
  - `application` pour une orchestration de use case
  - `infrastructure` pour un comportement technique

### 11.2 Green: faire passer le test avec le minimum de code

- Ecrire la plus petite implementation permettant de passer le test.
- Eviter d'anticiper des cas non demandes par le test courant.
- Si plusieurs tests echouent, traiter un seul comportement a la fois.
- Valider localement avec un scope minimal avant la suite complete.

### 11.3 Refactor: ameliorer sans changer le comportement

- Refactorer seulement quand tous les tests sont verts.
- Simplifier les noms, extraire les abstractions utiles, eliminer la duplication.
- Verifier qu'aucune regle metier n'est deplacee hors de `domain` sans justification.
- Relancer les tests du module puis la suite complete apres refactoring.

### 11.4 Regles pratiques d'application

- Un commit d'evolution metier doit contenir au moins:
  - un test Red initial
  - le code Green associe
  - les ajustements Refactor necessaires
- En cas de bug de production:
  - reproduire par un test qui echoue
  - corriger ensuite
  - conserver le test comme non-regression
- Si un test est difficile a ecrire, traiter cela comme un signal de design a ameliorer.

### 11.5 Anti-patterns TDD a eviter

- Ecrire la production en premier puis "ajouter des tests" apres coup.
- Ecrire des tests trop larges qui valident plusieurs regles en meme temps.
- Mocker excessivement au point de tester l'implementation interne plutot que le comportement.
- Ignorer un test instable au lieu de corriger sa cause (temps, ordre, etat partage, I/O implicite).
