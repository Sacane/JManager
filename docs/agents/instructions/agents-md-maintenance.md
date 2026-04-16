# AGENTS.md Maintenance Strategy

Strategie de maintenance du systeme de guidelines agent pour garder des instructions claires, coherentes, et applicables dans le temps.

## 1. Scope

Ce document couvre la maintenance des artefacts suivants:
- `AGENTS.md`
- `docs/agents/instructions/*.md`
- tout document de workflow agent lie au repository

Ce document ne couvre pas:
- les decisions metier produit (dans `FEATURES.md`)
- les details d'implementation d'une feature specifique

## 2. Objectifs

- Eviter la derive entre les instructions et la realite du codebase.
- Assurer des regles actionnables, testables, et non contradictoires.
- Reduire les ambiguites pour les humains et pour les agents.
- Garder un systeme de guidelines simple a maintenir.

## 3. Roles et responsabilites

- Owner guidelines:
  - valide la coherence globale des documents
  - arbitre les conflits entre instructions
  - planifie les revues periodiques
- Contributeur:
  - propose des mises a jour ciblees
  - justifie tout changement par un probleme concret
  - applique la checklist de qualite avant PR
- Reviewer:
  - verifie l'absence de contradictions
  - controle la clarte, la precision, et le caractere actionnable
  - demande des exemples quand une regle est ambigue

## 4. Triggers de mise a jour

Une mise a jour est obligatoire quand au moins un cas arrive:
- changement d'architecture (module, boundaries, conventions)
- ajout/suppression d'un outil ou d'une contrainte technique
- evolution du workflow de dev (tests, review, release)
- bug recurrent lie a une instruction manquante ou trompeuse
- conflit detecte entre deux documents de guidelines

## 5. Cadence de maintenance

- Maintenance continue: chaque PR qui modifie un workflow met a jour les guidelines impactees.
- Revue periodique: 1 revue mensuelle legere du systeme complet.
- Revue majeure: 1 revue trimestrielle pour simplifier, deprecier, ou fusionner des regles.

## 6. Workflow de changement

1. Identifier le probleme
- decrire le symptome observe
- relier le symptome a une regle manquante, obsolete, ou contradictoire

2. Definir la proposition
- preciser le document cible
- ecrire la regle en termes actionnables (verbe + condition + resultat attendu)
- limiter la portee au besoin reel

3. Implementer la mise a jour
- modifier le minimum de fichiers necessaires
- conserver le style editorial existant
- ajouter un exemple concret si la regle est sensible a l'interpretation

4. Verifier la qualite
- passer la checklist de la section 7
- faire relire par au moins un reviewer

5. Tracer le changement
- ajouter une entree courte dans la section historique (section 8)
- reference au PR dans le message de commit ou la description de PR

## 7. Checklist qualite

Avant merge, verifier:
- [ ] la regle est claire et testable
- [ ] la regle n'entre pas en conflit avec un autre document
- [ ] le scope est explicite (ou, quand, pour qui)
- [ ] les formulations vagues ont ete eliminees
- [ ] les liens internes et chemins de fichiers sont valides
- [ ] l'exemple (si present) est coherent avec le codebase actuel

## 8. Historique de maintenance

Format recommande:

```text
YYYY-MM-DD | type(change|cleanup|clarification|deprecation) | fichier(s) | resume court
```

## 9. Regles de redaction

- Preferer des phrases courtes et imperatives.
- Une regle = une intention.
- Eviter les formulations absolues non justifiees.
- Eviter la duplication entre documents; lier vers la source de verite.
- Quand une regle devient obsolete, la supprimer au lieu de la laisser inactive.

## 10. Definition of Done (maintenance)

Une mise a jour de guidelines est complete si:
- le probleme initial est explicite et resolu
- les conflits potentiels ont ete traites
- la checklist qualite est complete
- la trace de changement est ajoutee
- la nouvelle version est comprensible sans contexte oral supplementaire
