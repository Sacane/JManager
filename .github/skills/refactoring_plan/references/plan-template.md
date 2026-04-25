# Template — REFACTORING_PLAN.md

Le fichier généré doit suivre exactement cette structure.
Adapter la langue à celle du développeur (français par défaut).

---

```markdown
# REFACTORING_PLAN — [Nom du module / feature]

**Généré le** : YYYY-MM-DD  
**Pattern appliqué** : [Use Case Split / Hexagonal / CQRS / Extract Service / …]  
**Stack** : [ex : Kotlin + Spring Boot 3]  
**Statut global** : 🔄 En cours — Étape X / N

---

## Analyse initiale

### Ce qui a été détecté
[Description factuelle de l'état du code analysé — 3 à 6 points, sans jugement de valeur]

- `BudgetService` contient 11 méthodes couvrant des responsabilités distinctes (création, lecture, calcul, suppression)
- La couche `application` importe directement des entités JPA (`@Entity`), couplant le domaine à la persistance
- Les tests existants ciblent `BudgetServiceImpl` dans son ensemble, rendant les tests unitaires fins difficiles
- [...]

### Ce qui ne sera PAS modifié
[Périmètre négatif explicite — ce qui reste intact]

- La logique métier interne à chaque opération
- Le modèle de domaine (`Budget`, `Transaction`, `BudgetId`)
- Les ports de sortie (`BudgetRepository`, `NotificationPort`)
- Les tests d'intégration existants

### Justification du pattern choisi
[2 à 4 phrases expliquant pourquoi CE pattern pour CE projet, pas un autre]

---

## Étapes du refactoring

<!-- 
  Format de chaque étape :
  - Numéro et titre clair
  - Objectif : ce qu'on veut obtenir
  - Actions : liste ordonnée de ce que l'IA va faire
  - Critère de validation : comment le dev confirme que c'est bon
  - Statut : ⏳ À faire | 🔄 En cours | ✅ Terminé | ⏸️ En attente dev | ❌ Bloqué
-->

---

### Étape 1 — [Titre court et précis]

**Statut** : ⏳ À faire  
**Objectif** : [Ce que cette étape accomplit concrètement]  
**Bloquante pour** : Étapes 2, 3 *(ou "Aucune dépendance")*

**Actions :**
1. [Action atomique 1]
2. [Action atomique 2]
3. [...]

**Critère de validation :**
> [Ce que le dev doit vérifier pour confirmer que l'étape est réussie]
> Ex : "Le projet compile sans erreur. `BudgetServiceImpl` n'est plus référencée directement dans aucun controller."

---

### Étape 2 — [Titre]

**Statut** : ⏳ À faire  
**Dépend de** : Étape 1  
**Objectif** : [...]

**Actions :**
1. [...]

**Critère de validation :**
> [...]

---

### Étape N — [Titre]

**Statut** : ⏳ À faire  
**Dépend de** : Étapes X, Y  
**Objectif** : [...]

**Actions :**
1. [...]

**Critère de validation :**
> [...]

---

## Ordre d'exécution recommandé

```
Étape 1 → Étape 2 → Étape 3
                  ↘ Étape 4 (parallèle avec 3)
                            → Étape 5
```

*Les étapes en parallèle peuvent être faites dans n'importe quel ordre entre elles.*

---

## Protocole de validation

À chaque étape :
1. L'IA annonce l'étape et ce qu'elle va faire
2. L'IA produit les modifications
3. Le dev vérifie selon le critère de validation de l'étape
4. Le dev répond **"OK"** (ou demande un ajustement)
5. L'IA met à jour le statut et passe à l'étape suivante

**Ne jamais sauter une étape sans confirmation explicite.**

---

## Risques identifiés

| Risque | Probabilité | Mitigation |
|--------|-------------|------------|
| [Ex : Injection Spring cassée après split] | Moyenne | Vérifier les `@Autowired` après chaque étape |
| [...] | [...] | [...] |

---

## Références
- [Lien vers doc pattern si pertinent]
- [Ticket / PR associé si disponible]
```

---

## Notes pour la génération

- Le nombre d'étapes doit être proportionnel à la taille du refactoring : 3–5 pour un petit module, 8–15 pour un refactoring d'architecture complet
- Chaque étape doit être réalisable en moins de 15 minutes par l'IA — si c'est plus long, la découper
- Le critère de validation doit être **vérifiable par le dev sans expertise approfondie** : compilation, test qui passe, absence d'import incohérent, etc.
- Ne pas mélanger "créer des fichiers" et "supprimer des fichiers" dans la même étape
- Toujours commencer par les étapes les moins risquées (création de nouveaux fichiers avant suppression des anciens)
