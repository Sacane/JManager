---
name: refactoring-plan
description: Analyse le code source d'un projet et génère un fichier de planning de refactoring structuré, que l'IA exécute étape par étape avec validation du développeur entre chaque étape. Utiliser ce skill dès que l'utilisateur mentionne un refactoring à effectuer (hexagonal, CQRS, use case split, extract service, clean architecture, restructuration de packages…) et qu'il veut un plan avant d'agir. Déclencher aussi sur : "prépare le refactoring", "fais un plan", "qu'est-ce qu'il faut changer", "par où commencer", "analyse mon code avant de refactorer".
---

# Refactoring Plan Skill

Ce skill produit un fichier de planning (`REFACTORING_PLAN.md`) que l'IA et le développeur suivent ensemble. L'IA analyse d'abord le projet, choisit la meilleure stratégie, puis déroule les étapes une par une — le développeur valide avant chaque passage à la suivante.

---

## Phase 1 — Analyse du projet

Avant de produire quoi que ce soit, analyser le code fourni. L'objectif est de comprendre l'état réel du projet, pas d'appliquer un pattern de façon mécanique.

**Ce qu'il faut extraire :**

1. **Structure actuelle** : quels packages, quelles classes, quelles interfaces existent
2. **Couplages détectés** : qu'est-ce qui dépend de quoi (services, repositories, controllers)
3. **Points de douleur** : interfaces trop larges, classes God, dépendances croisées, logique métier dans la mauvaise couche
4. **Ce qui fonctionne déjà bien** : ne pas toucher à ce qui est sain
5. **Stack et conventions** : annotations Spring présentes, style de nommage, organisation en place

Lire les fichiers fournis. Si la structure complète du projet n'est pas fournie, demander les fichiers manquants avant de continuer.

---

## Phase 2 — Choix de la stratégie

Sur la base de l'analyse, choisir la stratégie de refactoring la plus adaptée à **l'état réel** du projet. Ne pas imposer un pattern dogmatique si le projet n'en a pas besoin.

Consulter `references/patterns.md` pour identifier le ou les patterns pertinents.

**Critères de choix :**
- Si les interfaces ont trop de méthodes sans cohésion → Use Case Split
- Si le domaine dépend de Spring/JPA → Hexagonal (isoler le domaine)
- Si les lectures et écritures sont mélangées avec des logiques complexes → CQRS
- Si une classe fait tout → Extract Service
- Si les couches sont floues → Package restructuring d'abord, puis le reste

La stratégie choisie doit être justifiée dans le plan.

---

## Phase 3 — Génération du fichier de planning

Produire le fichier `REFACTORING_PLAN.md` en suivant le template de `references/plan-template.md`.

**Règles impératives pour le plan :**

- Chaque étape est **atomique** : une seule action, un seul fichier ou un seul concept modifié
- Chaque étape a un **critère de validation explicite** : comment le dev sait que c'est bon
- L'ordre respecte les **dépendances** : on crée les interfaces avant les implémentations, on restructure les packages avant de déplacer les classes
- Les étapes **non bloquantes** sont marquées comme telles (peuvent être faites en parallèle)
- Le plan indique clairement **ce qui ne change pas** pour rassurer sur le périmètre

---

## Phase 4 — Exécution guidée

Une fois le plan validé par le développeur, l'exécuter étape par étape.

**Protocole d'exécution :**

1. Annoncer l'étape en cours : numéro, titre, objectif
2. Effectuer l'action (générer le code, proposer la modification)
3. Rappeler le critère de validation de l'étape
4. **Attendre la confirmation du développeur** avant de passer à la suivante
5. Si le dev demande un ajustement, l'appliquer avant de continuer
6. Mettre à jour le statut de l'étape dans le plan (✅ / 🔄 / ⏸️)

Ne jamais enchaîner deux étapes sans confirmation. Ne jamais modifier quelque chose qui n'est pas dans le plan sans le signaler.

---

## Fichiers de référence

- `references/plan-template.md` — Template exact du fichier `REFACTORING_PLAN.md` à produire. **Lire avant de générer le plan.**
- `references/patterns.md` — Descriptions des patterns courants et critères de choix. **Lire pendant la Phase 2.**
