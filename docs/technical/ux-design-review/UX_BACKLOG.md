# Chantier UI/UX — Backlog priorisé

> Compagnon d'exécution de [`UX_DESIGN_REVIEW.md`](./UX_DESIGN_REVIEW.md).
> Le rapport pose le diagnostic ; ce document dit **quoi faire, dans quel ordre, et à quel coût**.
> Date : 2026-09-02.

---

## 1. Comment lire ces tableaux

| Colonne | Valeurs |
|---|---|
| **Type** | `Fix` défaut à corriger · `Feature` nouveauté · `Dette` cohérence / nettoyage |
| **Impact** | `Critique` casse l'usage · `Fort` friction quotidienne · `Moyen` gêne ponctuelle · `Faible` finition |
| **Effort** | `XS` < 1 h · `S` 2–4 h · `M` ~1 j · `L` 2–3 j · `XL` > 1 semaine |
| **Portée** | `FE` frontend seul · `FS` full-stack (domain → infra → application → client) |

Les charges sont indicatives et **incluent les tests** (Vitest côté client, TDD par couche côté backend conformément à `CLAUDE.md`).

### Vérifications backend effectuées

Trois éléments du rapport changent de priorité une fois le backend inspecté :

| Constat | Réalité backend | Conséquence |
|---|---|---|
| « Mot de passe oublié » manquant | **Aucun endpoint** (`forgot`, `reset-password` : 0 occurrence) | Ce n'est pas un correctif : c'est une feature full-stack `XL` → passe en P1 |
| Bouton « Actions » admin mort | `AdminController` n'expose que `POST /users` et `GET /users` | Le correctif P0 est de **retirer** le bouton ; les vraies actions sont une feature P2 |
| Pas de section « Mon compte » / RGPD | `DELETE /api/user/me` **existe déjà** et n'est appelé par aucun écran | La suppression de compte est un chantier **frontend seul** → gain rapide et conformité |
| Filtrage par plage de dates | `GET /booklet/{id}/transactions` accepte déjà `startDate` / `endDate` | Le filtre par période est **frontend seul**, pas besoin de toucher au backend |
| Recherche textuelle | Aucun paramètre `label` / `search` sur l'endpoint | Recherche = full-stack `L` |
| Changement de mot de passe | **Entièrement implémenté** sur les 4 couches et testé. Le domain distingue 4 échecs (`USER_NOT_FOUND`, `USER_UNAUTHORIZED`, `PASSWORD_NOT_MATCH`, `PASSWORD_UNCHANGED`) | Le client n'en mappe que 2 → rattaché à `UX-20`, et la règle « différent de l'ancien » à `UX-27`. Aucun travail backend |
| Budget côté serveur | `UserSettingsDTO` = `projectionWindowDays` + `bookletCycles` uniquement | Migration du budget = full-stack `L` |

---

## 2. P0 — Défauts bloquants (lot 1, à démarrer maintenant)

Tout ce qui, aujourd'hui, produit une interface cassée, une donnée fausse ou un contrôle qui ment.

| ID | Sujet | Type | Impact | Effort | Portée | Fichiers principaux |
|---|---|---|---|---|---|---|
| **UX-01** | Le hamburger mobile recouvre le contenu de 6 pages | Fix | Critique | S | FE | `layouts/sidebar-layout.vue`, `pages/tag/index.vue` |
| **UX-02** | Page 404 nue, sans layout ni retour, en anglais | Fix | Critique | S | FE | `pages/[...all].vue` |
| **UX-03** | Moyenne journalière divisée par 30 quelle que soit la période | Fix | Fort | XS | FE | `pages/index.vue` |
| **UX-04** | Bouton « Actions » admin sans handler (desktop + mobile) | Fix | Fort | XS | FE | `pages/admin/index.vue` |
| **UX-05** | Flèche de sortie sur `/consent` et `/force-password-change` | Fix | Fort | S | FE | `layouts/centercard.vue` |
| **UX-06** | Paramètres : 2 boutons « Enregistrer » ambigus, perte de saisie | Fix | Fort | M | FE | `pages/settings/index.vue` |
| **UX-07** | Fautes d'accents visibles par l'utilisateur (7 libellés) | Fix | Moyen | XS | FE | `settings/index.vue`, `TransactionCreationDialog.vue`, `booklet/index.vue` |
| **UX-08** | Scroll-jacking des graphiques du dashboard | Fix | Fort | S | FE | `pages/index.vue` |
| **UX-09** | Suppression du code mort (5 fichiers, dont 1 cassé) | Dette | Moyen | XS | FE | `monthPicker.vue`, `TitleCard.vue`, `BalanceCard.vue`, `delete.vue`, `pages/user/[id].vue` |
| **UX-10** | `outline: none !important` sans remplacement (4 fichiers) | Fix | Fort | S | FE | `BookletPageHeader.vue`, `BookletFilterActionBar.vue`, `monthPicker.vue` |

**Total lot 1 : ~2,5 jours.** Aucune dépendance backend, aucune dépendance entre les items — parallélisable.

---

## 3. P1 — UX majeure (lot 2 et 3)

Ce qui coûte tous les jours à l'utilisateur sans être « cassé ».

| ID | Sujet | Type | Impact | Effort | Portée | Dépend de |
|---|---|---|---|---|---|---|
| **UX-11** | Tokens sémantiques `--success` / `--danger` / `--income` / `--expense` | Dette | Fort | S | FE | — |
| **UX-12** | Unifier le code couleur recette/dépense (bleu vs vert selon les pages) | Fix | Fort | S | FE | UX-11 |
| **UX-13** | « Mon compte » : profil, export, **suppression de compte** (RGPD) | Feature | Fort | M | FE | — |
| **UX-14** | Filtre par plage de dates dans un livret (backend déjà prêt) | Feature | Fort | M | FE | — |
| **UX-15** | Rendre Modifier / Supprimer découvrables sur les régulières | Fix | Fort | M | FE | — |
| **UX-16** | Harmoniser l'interaction desktop (double-clic) / mobile (simple appui) | Fix | Fort | S | FE | UX-15 |
| **UX-17** | Confirmation forte sur la suppression d'un livret (retaper le nom) | Fix | Fort | S | FE | — |
| **UX-18** | Restructurer le dashboard : 3 zones au lieu de 15 blocs | Dette | Fort | L | FE | UX-11 |
| **UX-19** | Prochaine échéance + total mensuel engagé sur les régulières | Feature | Fort | M | FE | — |
| **UX-20** | Erreurs de formulaire inline, en complément des toasts | Fix | Fort | M | FE | — |
| **UX-21** | « Mot de passe oublié » (aucun endpoint aujourd'hui) | Feature | Critique | XL | FS | — |
| **UX-22** | Recherche textuelle des transactions | Feature | Critique | L | FS | — |
| **UX-23** | Migrer la cible de budget de `localStorage` vers le serveur | Fix | Fort | L | FS | — |
| **UX-24** | Livrets liés affichés sur la ligne d'une régulière | Feature | Moyen | S | FE | — |
| **UX-25** | Solde courant (running balance) par ligne de transaction | Feature | Fort | M | FE | — |
| **UX-26** | Skeletons de chargement à la place des spinners plein écran | Dette | Moyen | M | FE | — |
| **UX-27** | Règles + indicateur de force du mot de passe (3 écrans) | Feature | Fort | M | FE | — |

**Total P1 : ~14 jours**, dont 3 items full-stack (`UX-21`, `UX-22`, `UX-23`) qui représentent à eux seuls ~8 jours.

---

## 4. P2 — Cohérence et finition (lot 4)

| ID | Sujet | Type | Impact | Effort | Portée |
|---|---|---|---|---|---|
| **UX-28** | Migrer le dashboard vers les shortcuts UnoCSS (12 styles inline dupliqués) | Dette | Moyen | L | FE |
| **UX-29** | Remettre la page Tags aux couleurs de la marque (retirer l'indigo `#6366f1`) | Dette | Moyen | XS | FE |
| **UX-30** | Shortcuts `page-shell` / `page-header` / `stat-card` + migration des 4 en-têtes de page | Dette | Moyen | M | FE |
| **UX-31** | Trancher sur i18n : activer (extraction des chaînes) ou retirer le module | Dette | Moyen | M ou XL | FE |
| **UX-32** | Retirer le libellé de thème « Clair / Sombre » de `NHeader` | Fix | Faible | XS | FE |
| **UX-33** | `centercard` : `max-w` manquant → débordement sous 400 px | Fix | Moyen | XS | FE |
| **UX-34** | Sommaire ancré + « retour en haut » sur les pages légales (23 sections) | Feature | Moyen | S | FE |
| **UX-35** | Retour contextuel des pages légales (aujourd'hui codé en dur vers `/login`) | Fix | Moyen | XS | FE |
| **UX-36** | Alternative clavier + tactile au drag & drop (livrets, sous-tags) | Fix | Moyen | M | FE |
| **UX-37** | Recherche + tri sur la liste des utilisateurs admin | Feature | Moyen | M | FS |
| **UX-38** | Actions admin réelles (rôle, désactivation, reset) — endpoints à créer | Feature | Moyen | L | FS |
| **UX-39** | Confirmation sur le basculement d'un feature flag à impact global | Fix | Moyen | S | FE |
| **UX-40** | Formulaire de création admin dans un dialog plutôt qu'en tête de page | Dette | Faible | S | FE |
| **UX-41** | Aperçu en direct du cycle mensuel (« septembre = 25/08 → 24/09 ») | Feature | Fort | M | FE |
| **UX-42** | Compteur d'usage et montant cumulé par tag | Feature | Moyen | M | FS |
| **UX-43** | État vide global du dashboard (aucun livret) | Fix | Moyen | S | FE |
| **UX-44** | Vraie option « Tous les comptes » (le libellé existe, pas le mode) | Feature | Moyen | M | FE |
| **UX-45** | Explication de la limite de 6 livrets + chemin de sortie | Fix | Faible | XS | FE |
| **UX-46** | `autocomplete` complet sur les formulaires d'authentification | Fix | Moyen | XS | FE |
| **UX-47** | Toggle « afficher le mot de passe » (4 écrans) | Feature | Moyen | S | FE |
| **UX-48** | Conserver la saisie lors de la bascule connexion ↔ inscription | Fix | Faible | XS | FE |
| **UX-49** | Liens légaux en pied de la page de connexion | Fix | Moyen | XS | FE |
| **UX-50** | Remplacer « Actions rapides » du dashboard par de vraies actions | Dette | Moyen | S | FE |

---

## 5. P3 — Roadmap produit (après le chantier)

| ID | Sujet | Impact | Effort | Portée |
|---|---|---|---|---|
| **UX-51** | Recherche globale ⌘K (transactions, tags, livrets, actions) | Fort | XL | FS |
| **UX-52** | Budgets par tag avec jauges et alertes | Fort | XL | FS |
| **UX-53** | Pointage bancaire (transaction rapprochée / non rapprochée) | Fort | L | FS |
| **UX-54** | Saisie assistée : autocomplétion du libellé, tag pré-rempli, duplication | Fort | L | FS |
| **UX-55** | Annulation (undo) via toast à la place de la confirmation modale | Fort | M | FS |
| **UX-56** | Onboarding en 3 étapes au premier login | Fort | M | FE |
| **UX-57** | Mise en pause d'une régulière (au lieu de supprimer) | Moyen | M | FS |
| **UX-58** | Exports PDF / Excel et export multi-mois | Moyen | L | FS |
| **UX-59** | Raccourcis clavier sur les tableaux (`n`, `/`, `j/k`) | Moyen | M | FE |
| **UX-60** | PWA installable | Moyen | M | FE |
| **UX-61** | Rapport mensuel par e-mail (infrastructure e-mail déjà en place) | Moyen | L | FS |
| **UX-62** | Archivage d'un livret plutôt que suppression | Moyen | M | FS |
| **UX-63** | Fusion de deux tags / réaffectation obligatoire à la suppression | Moyen | L | FS |

---

## 6. Séquencement recommandé

```
Lot 1 — Assainissement           ~2,5 j   UX-01 → UX-10
        Aucune dépendance. Tout est frontend, parallélisable.
        Sortie : plus aucun contrôle mort, plus aucune donnée fausse,
                 mobile utilisable, clavier navigable.

Lot 2 — Socle visuel             ~1,5 j   UX-11, UX-12, UX-29, UX-30, UX-32, UX-33
        À faire avant toute refonte de page : les tokens sémantiques
        conditionnent UX-18 et UX-28.
        Sortie : un seul vert, un seul rouge, un seul style de carte.

Lot 3 — Conformité & gains vite acquis   ~3 j   UX-13, UX-14, UX-17, UX-43, UX-45 → UX-49
        Uniquement du frontend sur des endpoints qui existent déjà
        (DELETE /api/user/me, startDate/endDate).
        Sortie : écart RGPD comblé, filtre par période, parcours d'auth propre.

Lot 4 — Pages métier             ~6 j   UX-15, UX-16, UX-19, UX-20, UX-24, UX-25, UX-26, UX-41, UX-50
        Sortie : régulières et livret détail réellement utilisables.

Lot 5 — Refonte dashboard        ~3 j   UX-18, UX-28, UX-44
        Dépend du lot 2.

Lot 6 — Chantiers full-stack     ~8 j   UX-21, UX-22, UX-23
        Chacun est un sujet à part entière (TDD par couche).
        À lancer en parallèle des lots 4/5 si plusieurs personnes.

Lot 7 — Reste P2 + décision i18n
```

**Chemin critique jusqu'à une application « saine » : lots 1 à 4, soit ~13 jours.**

---

## 7. Fiches d'exécution — Lot 1

### UX-01 — Hamburger mobile qui recouvre le contenu

- **Constat** : `.toggle-btn` est `position: fixed; top: 1rem; left: 1rem` en 44×44 px sous 769 px. Seule `pages/tag/index.vue` compense (`margin-top: 4rem`). Les 6 autres pages ne compensent pas ; sur `booklet/[id]` le bouton recouvre le **bouton retour**.
- **Correctif** : porter le décalage dans `layouts/sidebar-layout.vue` (`<main class="pt-16 md:pt-0">`) plutôt que dans chaque page, puis **retirer** le `margin-top: 4rem` de `tag/index.vue` pour éviter le double décalage.
- **Critère d'acceptation** : à 375 px de large, sur les 7 pages du layout `sidebar-layout`, aucun élément interactif ou textuel n'est recouvert par le bouton de menu.
- **Tests** : ajouter un test de non-régression sur `sidebar-layout` vérifiant la classe de décalage.

### UX-02 — Page 404

- **Correctif** : reconstruire `pages/[...all].vue` avec `layout: 'centercard'`, icône, message en français, bouton « Retour au tableau de bord ».
- **Critère d'acceptation** : `/nimportequoi` affiche une page thémée (clair et sombre) avec un chemin de sortie.

### UX-03 — Moyenne journalière fausse

- **Constat** : `{{ (monthlyExpenses / 30).toFixed(2) }}` dans `pages/index.vue`. En vue Trimestre le résultat est ×3 trop élevé, en vue Année ×12.
- **Correctif** : diviser par le nombre de jours réel de la période active (la plage est déjà calculée par `resolveMonthlyCycleRangeForTargetMonth`).
- **Critère d'acceptation** : test unitaire couvrant les trois périodes (`month`, `quarter`, `year`).

### UX-04 — Bouton « Actions » admin mort

- **Constat** : `icon="pi pi-ellipsis-v"` sans `@click`, en version tableau **et** en version mobile. `AdminController` n'expose aucun endpoint de modification d'utilisateur.
- **Correctif** : **retirer** le bouton et la colonne « Actions ». La feature réelle est `UX-38` (backend à créer).
- **Critère d'acceptation** : aucun contrôle sans effet dans la console admin.

### UX-05 — Sortie possible depuis les murs de parcours

- **Constat** : `layouts/centercard.vue` affiche une flèche fixe vers `/`. Sur `/consent` et `/force-password-change`, l'utilisateur est censé ne pas pouvoir passer.
- **Correctif** : rendre la flèche conditionnelle (prop de page ou `route.meta.allowBack`), masquée sur ces deux routes.
- **Critère d'acceptation** : aucune affordance de sortie sur `/consent` et `/force-password-change` ; la flèche reste sur `/verify-email`.

### UX-06 — Deux boutons « Enregistrer » ambigus

- **Constat** : « Enregistrer les parametres » (bas de page) ne sauvegarde **que** projection + cycles. Modifier la projection puis cliquer sur « Changer le mot de passe » perd les réglages sans avertissement.
- **Correctif** : sauvegarde par section avec confirmation inline, indicateur « modifications non enregistrées », et garde-fou à la navigation (`onBeforeRouteLeave`).
- **Critère d'acceptation** : chaque carte a son propre bouton de portée explicite ; quitter la page avec des modifications non sauvegardées déclenche une confirmation.

### UX-07 — Fautes d'accents

`Parametres` → `Paramètres` · `Definis` → `Définis` · `previsions` → `prévisions` · `Fenetre` → `Fenêtre` · `7 a 60` → `7 à 60` · `Selectionner` → `Sélectionner` · `Cette action et irréversible` → `est irréversible` · `transactions enregistrés` → `enregistrées`.

### UX-08 — Scroll-jacking

- **Constat** : `@wheel.prevent` sur les conteneurs Line et Bar capte la molette pour zoomer l'axe Y, sur une page de plusieurs écrans de haut, sans possibilité de réinitialiser.
- **Correctif** : n'agir que si `event.ctrlKey || event.metaKey`, sinon laisser le défilement passer ; ajouter un bouton « Réinitialiser l'échelle » visible dès que `yMin`/`yMax` sont surchargés.
- **Critère d'acceptation** : molette seule = la page défile ; `Ctrl/⌘ + molette` = zoom ; le bouton de réinitialisation remet `null`.

### UX-09 — Code mort

Supprimer `components/monthPicker.vue`, `components/TitleCard.vue`, `components/card/BalanceCard.vue`, `components/delete.vue` (jamais importé **et** cassé : `visible` n'est jamais passé à `true`, `onActionValid` est un `ref()` vide utilisé comme handler), et `pages/user/[id].vue` (route accessible en production, affiche `User {id}`).

### UX-10 — Focus invisible

- **Correctif** : retirer les `outline: none !important` de `BookletPageHeader.vue` et `BookletFilterActionBar.vue`, et ajouter une règle `:focus-visible` globale dans `assets/css/reset.css` (`outline: 2px solid var(--primary); outline-offset: 2px`).
- **Critère d'acceptation** : parcourir au clavier la barre de filtres du livret détail sans jamais perdre l'indicateur de focus, en clair et en sombre.

---

## 8. Traçabilité — issues et cartes Trello (P0 + P1)

Board **Développement** → liste **« À faire ! »**, étiquette **UI/UX** (verte).
Les issues suivent la convention `docs/features/{feature}/{module}_{titre}.md` et passent toutes le validateur du skill `create-issue`.

| ID | Modules | Dossier d'issue (`docs/features/`) | Carte |
|---|---|---|---|
| UX-01 | client | `ux-01-mobile-sidebar-offset/` | https://trello.com/c/6PsNzdjR |
| UX-02 | client | `ux-02-not-found-page/` | https://trello.com/c/WhCZ8qpQ |
| UX-03 | client | `ux-03-daily-average-period/` | https://trello.com/c/OG7xjP9q |
| UX-04 | client | `ux-04-admin-dead-actions-button/` | https://trello.com/c/NMLWmoiu |
| UX-05 | client | `ux-05-centercard-escape-hatch/` | https://trello.com/c/78ROCUyb |
| UX-06 | client | `ux-06-settings-save-scope/` | https://trello.com/c/ScfaWwQd |
| UX-07 | client | `ux-07-french-typos/` | https://trello.com/c/duiMbj1s |
| UX-08 | client | `ux-08-chart-scroll-jacking/` | https://trello.com/c/ppSOVoF3 |
| UX-09 | client | `ux-09-dead-code-removal/` | https://trello.com/c/GgeQsijh |
| UX-10 | client | `ux-10-focus-visible/` | https://trello.com/c/Q5n0607E |
| UX-11 | client | `ux-11-semantic-color-tokens/` | https://trello.com/c/OIU0lqvf |
| UX-12 | client | `ux-12-income-expense-color-consistency/` | https://trello.com/c/DHYt0yiU |
| UX-13 | client | `ux-13-account-section-gdpr/` | https://trello.com/c/GazMZk0k |
| UX-14 | client | `ux-14-booklet-date-range-filter/` | https://trello.com/c/vhSK3BNE |
| UX-15 | client | `ux-15-regular-transaction-row-actions/` | https://trello.com/c/YahblFjh |
| UX-16 | client | `ux-16-edit-interaction-consistency/` | https://trello.com/c/qqv3cTew |
| UX-17 | client | `ux-17-booklet-delete-confirmation/` | https://trello.com/c/aVjHEWWq |
| UX-18 | client | `ux-18-dashboard-restructure/` | https://trello.com/c/cDeMCJr3 |
| UX-19 | client | `ux-19-regular-transaction-next-occurrence/` | https://trello.com/c/KivHyatB |
| UX-20 | client | `ux-20-inline-form-errors/` | https://trello.com/c/9GDCgFeQ |
| UX-21 | **4 modules** | `ux-21-password-reset/` | https://trello.com/c/o5vUbR2j |
| UX-22 | **4 modules** | `ux-22-transaction-search/` | https://trello.com/c/MnUxNqSz |
| UX-23 | **4 modules** | `ux-23-server-side-budget-target/` | https://trello.com/c/tZEm5mo6 |
| UX-24 | client | `ux-24-regular-transaction-linked-booklets/` | https://trello.com/c/uEbABYS3 |
| UX-25 | client | `ux-25-running-balance/` | https://trello.com/c/2hheQnJy |
| UX-26 | client | `ux-26-loading-skeletons/` | https://trello.com/c/Xc9D1OuF |
| UX-27 | client | `ux-27-password-strength/` | https://trello.com/c/BEtdi2SP |

### Structure d'un dossier d'issue

Chaque dossier contient **deux niveaux**, tous deux au format Gherkin et tous deux validés :

| Fichier | Rôle | Sert à |
|---|---|---|
| `feature.md` | **Critères d'acceptation fonctionnels**, indépendants des couches — ce que l'utilisateur obtient, pas comment c'est construit | Tests d'acceptation / bout en bout, validation produit |
| `{module}_{titre}.md` | Scénarios d'implémentation et cas limites, propres à une couche | Tests unitaires et d'intégration de la couche |

**63 fichiers pour 27 items** : 27 `feature.md` + 36 fichiers module. Les trois chantiers full-stack (UX-21, UX-22, UX-23) sont découpés en `domain` / `infrastructure` / `application` / `client` dans l'ordre d'implémentation TDD, et leur `feature.md` est la référence fonctionnelle **commune aux quatre couches** — c'est lui qui garantit que le découpage ne perd pas la vue d'ensemble.

Le Gherkin fonctionnel de `feature.md` est repris **à l'identique dans la description de la carte Trello**, pour que le critère de « fini » soit lisible sans ouvrir le dépôt. Une seule carte par item.

Les scénarios sont rédigés en **anglais**, comme les issues (imposé par le skill `create-issue`) et comme la suite de tests, pour qu'ils soient transposables tels quels en cas de test.

Les items **P2 et P3** (`UX-28` → `UX-63`) n'ont volontairement ni issue ni carte : ils seront formalisés au moment de leur lot, pour ne pas figer des spécifications qui auront changé d'ici là.

---

## 9. Suivi

Ce backlog est le document de référence du chantier. À chaque item livré : cocher ici, déplacer la carte Trello, mettre à jour `Changelog.md` **uniquement quand un lot est complet** (conformément à `CLAUDE.md`), et supprimer les fiches devenues caduques.
