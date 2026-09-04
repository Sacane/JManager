# Revue UX / Design — JManager Client

> Périmètre : `client/` (Nuxt 4 / Vue 3 / PrimeVue 4 Lara / UnoCSS).
> Méthode : lecture des 16 pages, 4 layouts, 29 composants, du design system (`unocss.config.ts`, `assets/css/variables.css`, `assets/css/typography.css`) et de la configuration (`nuxt.config.ts`).
> Date : 2026-09-02.

---

## 1. Résumé exécutif

La fondation est bonne : palette de marque définie, tokens sémantiques clair/sombre complets, échelle typographique documentée, shortcuts UnoCSS pensés (`card`, `btn-primary`, `heading-*`, `body-*`), préréglage PrimeVue aligné sur la marque, et des pages récentes (`consent`, `verify-email`, `force-password-change`, `privacy`, `terms`) qui l'appliquent proprement.

Le problème n'est pas le design system : c'est qu'il n'est **appliqué que sur la périphérie de l'application**. Les six pages métier — celles où l'utilisateur passe 95 % de son temps — sont antérieures et utilisent trois grammaires de style concurrentes. S'y ajoutent des manques fonctionnels qui coûtent cher dans une application de finances personnelles : pas de recherche de transactions, pas de « mot de passe oublié », pas de solde courant, et un dashboard qui affiche quinze blocs de poids visuel identique.

### Notation par page

| Page | Design | UX / flux | Responsive | A11y | Global |
|---|---|---|---|---|---|
| `login.vue` | 4/5 | 2/5 | 4/5 | 3/5 | **3,2** |
| `index.vue` (Dashboard) | 3/5 | 2/5 | 3/5 | 2/5 | **2,5** |
| `booklet/index.vue` (Livrets) | 4/5 | 3/5 | 3/5 | 2/5 | **3,0** |
| `booklet/[id].vue` (Détail livret) | 3/5 | 2/5 | 4/5 | 2/5 | **2,7** |
| `regular-transaction/index.vue` | 3/5 | 2/5 | 3/5 | 2/5 | **2,5** |
| `tag/index.vue` | 4/5 | 4/5 | 4/5 | 3/5 | **3,7** |
| `settings/index.vue` | 2/5 | 2/5 | 3/5 | 3/5 | **2,5** |
| `admin/index.vue` | 4/5 | 2/5 | 3/5 | 3/5 | **3,0** |
| `consent` / `verify-email` / `force-password-change` | 5/5 | 4/5 | 3/5 | 4/5 | **4,0** |
| `privacy` / `terms` | 5/5 | 3/5 | 5/5 | 4/5 | **4,2** |
| `[...all].vue` (404) | 0/5 | 0/5 | 0/5 | 0/5 | **0,0** |
| `user/[id].vue` | 0/5 | 0/5 | — | — | **0,0** |

---

## 2. Constats transverses

### 2.1 Trois grammaires de style cohabitent

| Grammaire | Où | Exemple |
|---|---|---|
| Styles inline + utilitaires bruts | `pages/index.vue` | `class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);"` répété **12 fois** |
| SCSS `scoped` avec classes maison | `booklet/index`, `tag/index`, `admin/index`, `settings/index`, `login` | `.booklet-card`, `.tag-card`, `.settings-card` — trois définitions différentes d'une carte |
| Design system UnoCSS | `consent`, `verify-email`, `privacy`, `terms`, `force-password-change` | `class="card p-5"`, `class="heading-2"`, `class="btn-primary"` |

Le shortcut `card` existe et n'est utilisé que dans les pages légales. Conséquence directe : quatre rayons de bordure différents pour la même notion de carte (`12px`, `16px`, `20px`, `24px`), trois profondeurs d'ombre, deux échelles de titres.

**Action** — définir des shortcuts `page-shell`, `page-header`, `stat-card` dans `unocss.config.ts`, puis migrer page par page en commençant par le dashboard (le plus dupliqué).

### 2.2 Les couleurs sémantiques manquent dans les tokens

`variables.css` définit la marque et les surfaces, mais **aucun token succès / danger / avertissement / recette / dépense**. Chaque fichier réinvente donc sa nuance :

| Notion | Valeurs trouvées dans le code |
|---|---|
| Positif / recette | `text-green-500`, `#10b981`, `text-emerald-600`, `#009CFE` (bleu), `text-green-400` |
| Négatif / dépense | `text-red-500`, `#ef4444`, `#FF084B`, `text-pink-500`, `text-red-400` |
| Avertissement | `text-amber-500`, `text-amber-600`, `#f59e0b`, `#fbbf24`, `#FFC108` |

Plus gênant : dans `booklet/[id].vue` les recettes sont **bleues** (`#009CFE`) alors que partout ailleurs (dashboard, régulières, livrets) elles sont **vertes**. L'utilisateur doit réapprendre le code couleur en changeant de page.

**Action** — ajouter `--success`, `--danger`, `--warning`, `--info`, `--income`, `--expense` dans `variables.css` (clair + sombre) et des shortcuts `amount-positive` / `amount-negative`. Trancher une bonne fois : recette = vert **ou** bleu.

### 2.3 Hex codés en dur malgré la règle « jamais de hex brut »

`#45058C` (en-tête d'`AppTable`), `#190233` et `#FF084B` (sidebar), `#A30053`, `#009CFE`, `#FFC108` (boutons d'action du livret), `#10b981` / `#ef4444` (montants livrets), et `rgba(99,102,241)` / `#6366f1` (bouton principal de la page Tags et fallback de `TagCard`) — ce dernier est un **indigo hors palette** : le bouton principal de la page Tags n'est pas aux couleurs de la marque.

### 2.4 Accessibilité

- `outline: none !important` sans remplacement visible dans `BookletPageHeader.vue`, `BookletFilterActionBar.vue` et `monthPicker.vue`. La navigation clavier devient invisible sur toute la barre de filtres du livret.
- **2 usages de `:focus-visible`** dans l'ensemble du client.
- Nombreuses zones cliquables non focusables : cartes livret (`div @click` + `draggable`), lignes de transaction mobile, chips de tags du dashboard, en-tête dépliable du dialog d'import CSV.
- Les chevrons de navigation de période du dashboard et le `select` de livret n'ont pas de libellé accessible.
- Le drag & drop de réordonnancement (livrets, sous-tags) n'a **aucune alternative clavier** — et ne fonctionne pas au tactile (HTML5 DnD).

### 2.5 Responsive — le bouton hamburger recouvre le contenu

`app-sidebar.vue` place `.toggle-btn` en `position: fixed; top: 1rem; left: 1rem;` sur 44×44 px sous 769 px. **Seule `tag/index.vue` compense** (`margin-top: 4rem` sous 768 px).

Les six autres pages ne compensent pas :

| Page | Padding mobile | Élément recouvert |
|---|---|---|
| `index.vue` | `p-5` (20 px) | « Bonjour, {user} » |
| `booklet/index.vue` | `1.5rem` | icône + titre « Mes Livrets » |
| `booklet/[id].vue` | `py-3 px-5` | **bouton retour** de `BookletPageHeader` |
| `regular-transaction/index.vue` | `py-5 px-5` | titre de la carte d'en-tête |
| `settings/index.vue` | `1.5rem` | « Parametres utilisateur » |
| `admin/index.vue` | `1rem` | icône bouclier + titre |

**Action** — sortir la compensation des pages : décaler dans `layouts/sidebar-layout.vue` (`<main class="pt-16 md:pt-0">`), ou basculer le hamburger dans une vraie barre supérieure mobile, puis retirer le `margin-top: 4rem` de la page Tags.

### 2.6 États de chargement hétérogènes, aucun skeleton

Quatre traitements différents : spinner plein écran `pi pi-spin pi-spinner` (dashboard, livret détail), `ProgressSpinner` PrimeVue dans une carte (livrets, tags, admin), texte brut « Chargement des parametres... » (settings), spinner inline (dialogs). Aucun skeleton : le dashboard reste quasi vide pendant tout le chargement, puis quinze blocs apparaissent d'un coup avec une animation d'entrée.

### 2.7 Gestion d'erreur : tout passe par le toast

À l'exception de la confirmation de mot de passe, **aucune erreur n'est attachée au champ concerné**. `TransactionCreationDialog` affiche `jToast.warn('Veuillez saisir un montant supérieur à 0')` alors que la condition testée couvre aussi le libellé vide — le message ne correspond pas à l'erreur réelle. Un toast disparaît en quelques secondes ; l'utilisateur ne sait plus quel champ corriger.

### 2.8 i18n installé mais mort

`nuxt.config.ts` déclare `i18n: {}`, `i18n.config.ts` exporte `{}`, aucun fichier de locale n'existe, et 100 % des libellés sont du français en dur. Trois composables (`useCustomI18n`, `useLocale`, `useLocalizedNavigateTo`) et un `LocaleSwitch` sont maintenus pour rien — `LocaleSwitch` affiche par ailleurs les **codes bruts** (`fr`, `en`) au lieu des noms de langue.

**Décision à prendre** : soit l'internationalisation est au programme et on extrait les chaînes, soit on retire le module et ses composables. L'état actuel cumule les inconvénients des deux.

### 2.9 Code mort

| Fichier | État |
|---|---|
| `components/monthPicker.vue` | jamais importé |
| `components/TitleCard.vue` | jamais importé, `bg-white` en dur (cassé en sombre) |
| `components/card/BalanceCard.vue` | jamais importé, `bg-white` en dur |
| `components/delete.vue` | jamais importé **et cassé** : `visible` n'est jamais passé à `true`, `onActionValid` est un `ref()` vide utilisé comme gestionnaire de clic |
| `pages/user/[id].vue` | route accessible en production, affiche `User {id}` |

### 2.10 Fautes de français visibles par l'utilisateur

| Texte | Fichier |
|---|---|
| « Parametres utilisateur », « Enregistrer les parametres » | `settings/index.vue` |
| « Definis le nombre de jours », « les previsions » | `settings/index.vue` |
| « Fenetre de projection (7 a 60 jours) » | `settings/index.vue` |
| « Chargement des parametres... » | `settings/index.vue` |
| « Selectionner le type de transaction » | `TransactionCreationDialog.vue` |
| « Cette action **et** irréversible », « toutes les transactions **enregistrés** » | `booklet/index.vue` |
| « Etes vous sur de bien vouloir supprimer » | `delete.vue` |

---

## 3. Analyse page par page

### 3.1 `pages/login.vue` — Connexion / Inscription

**Ce qui fonctionne.** Carte centrée soignée, formes décoratives, bascule connexion/inscription sans changement de route, `maxlength` cohérent avec le backend, consentement CGU/Confidentialité au bon endroit, bouton désactivé tant que le formulaire est incomplet, gestion de l'état `loading`.

**Problèmes.**

1. **Pas de « Mot de passe oublié ? »** — c'est un cul-de-sac produit. Un utilisateur qui oublie son mot de passe n'a aucun recours dans l'interface. Manque n° 1 de la page. Le backend n'expose **aucun endpoint** correspondant (`forgot` / `reset-password` : 0 occurrence) : c'est donc un chantier full-stack complet (`UX-21`), pas un simple ajout de lien.
2. **Aucune règle de mot de passe affichée à l'inscription** et aucun indicateur de force. L'utilisateur découvre la contrainte via une erreur serveur générique (« Une erreur est survenue lors de l'inscription ») qui ne dit pas ce qui a échoué.
3. **La confirmation de mot de passe n'est vérifiée qu'à la soumission.** Un `watch` sur les deux champs, avec message inline sous le champ, éviterait l'aller-retour.
4. **`autocomplete` incomplet** : présent sur l'e-mail de connexion, absent sur le mot de passe (`current-password`), sur le nom d'utilisateur (`username`) et sur les mots de passe d'inscription (`new-password`). Les gestionnaires de mots de passe ne s'accrochent pas correctement.
5. **Pas de bouton « afficher le mot de passe »** — sur mobile, la saisie à l'aveugle est la première cause d'échec de connexion.
6. **La bascule login → register perd la saisie.** L'e-mail tapé côté connexion n'est pas repris côté inscription.
7. **Ni sélecteur de thème ni sélecteur de langue** sur cette page (le layout `default` n'inclut pas `NHeader`), alors que `centercard` et `legal` en ont un. Un utilisateur en thème sombre système arrive sur une page qui suit le thème, mais sans contrôle.
8. **Pas de liens légaux en pied de page** : CGU et Confidentialité ne sont accessibles que depuis les cases à cocher du formulaire d'inscription. Un visiteur en mode connexion ne peut pas les consulter.
9. « JManager Application » — le mot « Application » n'apporte rien, `JManager` suffit.

**Idées.** Lien « Mot de passe oublié ? » sous le champ mot de passe · barre de force du mot de passe avec les règles cochées en direct · toggle œil · connexion par lien magique (l'infrastructure e-mail existe déjà pour la vérification) · mémoriser le dernier e-mail utilisé.

---

### 3.2 `pages/index.vue` — Tableau de bord

C'est la page la plus riche et la plus problématique : 2 023 lignes, ~15 blocs de contenu.

**Ce qui fonctionne.** Sélecteur de période mois/trimestre/année avec navigation, KPI avec variation vs période précédente, graphique d'évolution, donut de répartition avec drill-down sur les sous-tags (très bonne idée), top tags avec variation, alertes contextuelles, persistance du livret sélectionné.

**Problèmes.**

1. **Aucune hiérarchie visuelle.** Les quinze blocs partagent exactement le même traitement (`rounded-2xl p-6 shadow-lg`, fond `--card-bg`). Rien n'indique ce qui est important. Un dashboard doit répondre à une question en trois secondes ; celui-ci demande de tout lire.
2. **Quatre pastilles d'en-tête** (« Période », « À venir 15 j », « Solde prévisionnel court terme », « Projection fin de période ») avec des libellés longs qui passent à la ligne, en jargon, et dont deux dupliquent des informations affichées plus bas.
3. **Le libellé « Tous les comptes » est mensonger.** Le sous-titre affiche `selectedBooklet?.label || 'Tous les comptes'`, mais il n'existe aucun mode agrégé : le `select` ne contient que les livrets et sélectionne d'office le premier. Le fallback n'apparaît qu'en état d'erreur.
4. **`select` HTML natif** au milieu de composants PrimeVue — rupture visuelle, et le rendu de la liste déroulante n'est pas thémé en mode sombre.
5. **Calcul faux hors vue mensuelle** : `Moy. journalière: {{ (monthlyExpenses / 30).toFixed(2) }} €` divise par 30 quelle que soit la période. En vue Trimestre la moyenne est ×3 trop élevée, en vue Année ×12. **À corriger** en divisant par le nombre de jours réel de la période.
6. **« Objectif : 30 % »** est codé en dur sous le taux d'épargne, non configurable, et sans lien avec la carte Budget juste en dessous.
7. **La cible de budget est stockée en `localStorage`** (`dashboard.budgetTargetsByBooklet.v1`) : perdue au changement de navigateur ou d'appareil, invisible côté serveur. Incohérent avec la page Paramètres qui, elle, persiste côté serveur. Les alertes budget deviennent donc « par appareil ».
8. **Scroll-jacking sur les graphiques.** `@wheel.prevent` sur les conteneurs des graphiques Line et Bar capte la molette pour zoomer l'axe Y. Sur une page qui fait plusieurs écrans de haut, avec un graphique pleine largeur, l'utilisateur qui scrolle se retrouve bloqué et modifie l'échelle sans le vouloir. Il n'y a par ailleurs **aucun bouton pour réinitialiser l'échelle**. → Exiger `Ctrl/⌘ + molette`, ou remplacer par des boutons `+ / − / Réinitialiser`.
9. **Pas d'état vide global.** Sans livret, l'utilisateur voit malgré tout quatre KPI à `0.00 €`, trois graphiques vides et un `select` vide. Il faut un écran d'accueil dédié : « Créez votre premier livret pour voir votre tableau de bord ».
10. **Drag & drop ambigu** dans « Mes livrets » : la carte entière est `draggable` **et** cliquable pour naviguer, la poignée `pi pi-bars` est purement décorative. Un glisser raté déclenche la navigation.
11. **« Actions rapides » ne sert à rien** : les trois boutons (« Voir mes comptes », « Ajuster les régulières », « Revoir mes tags ») dupliquent la sidebar. Les remplacer par les actions réellement manquantes depuis le dashboard : « Ajouter une transaction », « Importer un CSV », « Créer une régulière ».
12. **Le bandeau de statistiques en bas duplique une troisième fois** des chiffres déjà présents (nombre de tags, transactions prévisionnelles, catégories actives).
13. **Pas de retour à la période courante.** Après plusieurs clics sur les chevrons, aucun bouton « Aujourd'hui ».
14. Les variations en pourcentage n'indiquent pas leur référence (période précédente) — un tooltip suffirait.
15. Les trois listes latérales ont un `max-h-87.5 overflow-y-auto` (350 px) sans aucun affordance de défilement.

**Idées.** Réduire à trois zones : *Où j'en suis* (solde + projection), *Ce qui arrive* (échéances), *Où part l'argent* (répartition + top tags) · déplacer le reste dans un onglet « Analyse » · rendre la cible de budget serveur et par tag · ajouter un vrai mode « Tous les comptes » · afficher la courbe de solde plutôt que revenus/dépenses seuls.

---

### 3.3 `pages/booklet/index.vue` — Mes Livrets

**Ce qui fonctionne.** Une des pages les plus abouties visuellement : en-tête avec icône dégradée, compteur `n/6`, état vide illustré et incitatif (« Commencez votre parcours d'épargne »), carte « Ajouter » en pointillés avec le nombre d'emplacements restants, grille responsive 3 → 2 → 1 colonne, états de chargement.

**Problèmes.**

1. **La limite de 6 livrets n'est jamais expliquée.** Le bouton « Limite atteinte » est désactivé, sans tooltip ni chemin de sortie (supprimer un livret ? passer à une offre supérieure ?).
2. **Suppression sous-protégée.** Le message annonce une action irréversible qui supprime **toutes les transactions** du livret, mais un simple clic sur « Supprimer » suffit. Pour une destruction de cette ampleur, demander de retaper le nom du livret. Le message contient par ailleurs deux fautes (« Cette action **et** irréversible », « transactions **enregistrés** »).
3. **La carte ne porte qu'un solde.** Ni nombre de transactions, ni date du dernier mouvement, ni variation sur le mois. Avec `min-height: 280px` par carte, on consomme beaucoup d'espace pour très peu d'information.
4. **Réordonnancement impossible au tactile** : le drag & drop HTML5 ne fonctionne pas sur mobile, et la poignée `.drag-handle` n'a qu'un `@click.stop` — c'est la carte entière qui est `draggable`.
5. Pas de tri ni de filtre (par solde, par nom, par activité).
6. Couleurs de montant en dur (`#10b981` / `#ef4444`) au lieu des tokens.
7. La zone cliquable est un `div` : non focusable, non atteignable au clavier.

**Idées.** Bascule grille / liste compacte · mini-sparkline du solde sur 6 mois dans la carte · archivage plutôt que suppression · boutons monter/descendre en complément du drag pour le tactile et le clavier.

---

### 3.4 `pages/booklet/[id].vue` — Détail d'un livret

Le cœur de l'application : 1 496 lignes, trois mises en page distinctes (mobile / desktop / desktop-court en mode barre latérale).

**Ce qui fonctionne.** Effort réel sur le responsive : liste groupée par jour façon application bancaire sur mobile avec chargement paresseux au scroll, tableau dense avec tri serveur sur desktop, barre d'action verticale quand la hauteur d'écran est faible. Filtres tag/sous-tag intégrés dans les en-têtes de colonnes. Transactions prévisionnelles bien différenciées (fond ambre, icône horloge, action de validation dédiée). Import/export CSV avec une aide au format remarquablement documentée.

**Problèmes.**

1. **Aucune recherche textuelle sur les transactions.** C'est le manque fonctionnel le plus coûteux de l'application. Pour retrouver « Carrefour », il faut parcourir les pages une à une, mois par mois. Les seuls filtres sont : tag, sous-tag, « tout le mois », « prévisionnelles ».
2. **Navigation strictement mensuelle.** Un `Select` mois + un `DatePicker` année, soit deux contrôles pour une seule notion, sans « mois précédent / suivant ». Impossible de voir une plage de dates, ni « les 90 derniers jours », ni de chercher à travers les mois.
3. **Pas de solde courant (running balance) ligne à ligne** — information standard et attendue dans un relevé de compte.
4. **Incohérence de couleur** : dans la colonne Recettes, la valeur *et* le tiret d'absence sont bleus (`#009CFE`) ; dans la colonne Dépenses la valeur est rose et le tiret gris. Deux traitements différents du vide, et un code couleur qui contredit le reste de l'application (vert = recette ailleurs).
5. **Les filtres actifs sont invisibles.** Les `Select` de tag/sous-tag sont dans les en-têtes de colonnes ; rien n'indique globalement qu'un filtre est appliqué, et il n'y a pas de bouton « réinitialiser les filtres ». Un utilisateur peut croire son livret vide.
6. **Cinq boutons d'action en icônes seules, sans sémantique de couleur** : rose = « nouvelle transaction » *et* « régénérer », jaune = « prévisionnelle », bleu = « importer » *et* « exporter ». Les couleurs ne portent aucun sens réutilisable ; seuls les tooltips sauvent la découvrabilité — et ils n'existent pas au tactile.
7. **Édition par double-clic** sur la ligne (desktop) : non découvrable. Le bouton crayon compense, mais l'interaction diffère de la version mobile (simple appui).
8. **`:loading="isConfirmPreviewLoading"` est global** : valider une transaction prévisionnelle met **toutes** les lignes en état de chargement simultanément.
9. **Mobile : deux affordances contradictoires.** La ligne entière déclenche la sélection *et* une case à cocher est affichée. Il n'existe pas de geste pour « ouvrir » une transaction — le clic sélectionne. Deux boutons 40×40 (crayon + validation) par ligne alourdissent fortement la liste.
10. **`outline: none !important`** sur l'ensemble des champs de l'en-tête et de la barre de filtres : navigation clavier invisible.
11. Le retour depuis le mode « Tout le mois » vers la pagination est un lien texte souligné noyé dans une phrase — peu visible pour une action de changement de mode.
12. Trois implémentations de la même barre d'action à maintenir (chips mobiles, barre desktop, colonne verticale).

**Idées.** Champ de recherche avec filtres avancés (texte, plage de dates, plage de montants, type, tag) et chips de filtres actifs supprimables · colonne « solde après opération » · duplication d'une transaction · autocomplétion du libellé à partir de l'historique avec tag pré-rempli · pointage bancaire (transaction rapprochée/non rapprochée) · swipe sur mobile pour éditer/supprimer · annulation via toast après suppression, à la place de la confirmation modale.

---

### 3.5 `pages/regular-transaction/index.vue` — Transactions régulières

**Ce qui fonctionne.** Tableau desktop et cartes mobile, filtre par tag, distinction recette/dépense claire, dialogs Lier/Délier explicites, sélection multiple avec suppression en masse.

**Problèmes.**

1. **Ni Modifier ni Supprimer visibles dans la colonne « Actions ».** Elle ne contient que « Lier » et « Délier ». L'édition passe par un **double-clic sur la ligne**, jamais signalé ; la suppression unitaire n'est accessible que depuis le dialog d'édition. Un utilisateur peut raisonnablement conclure qu'une transaction régulière n'est pas modifiable.
2. **Interaction incohérente entre plateformes** : desktop = double-clic pour éditer, mobile = simple appui sur la carte (le gestionnaire s'appelle pourtant `handleRowDoubleClick`). Sur mobile, on ouvre l'édition en effleurant la carte alors qu'on voulait juste faire défiler.
3. **La suppression en masse n'existe pas sur mobile** (`v-if="!isMobile"` sur le bouton).
4. **L'information la plus attendue est absente** : aucune prochaine échéance (« prochain prélèvement le 5 octobre »), aucun total mensuel engagé, aucun cumul recettes/dépenses régulières. C'est pourtant la raison d'être de la page.
5. **Les livrets liés ne sont pas affichés sur la ligne.** Il faut ouvrir le dialog « Délier » pour savoir à quoi la régulière est rattachée. Une colonne de chips résoudrait le problème.
6. **Pas de recherche**, ni de filtre par livret, par fréquence ou par type.
7. Boutons Lier/Délier désactivés sans explication au tactile (tooltip desktop uniquement).
8. Les en-têtes de dialogs utilisent des emoji (`🔗 Lier à un livret`, `⛓️‍💥 Délier un livret`) alors que le reste de l'application utilise des PrimeIcons — rupture de langage visuel.
9. Hiérarchie incohérente : le titre est dans une carte, mais les boutons d'action flottent en dehors, directement sur le fond dégradé.

**Idées.** Colonne « Prochaine échéance » + bandeau récapitulatif « X € de dépenses régulières / mois, Y € de revenus » · mise en pause d'une régulière (au lieu de supprimer) · aperçu des 3 prochaines occurrences dans le dialog d'édition · timeline mensuelle des échéances.

---

### 3.6 `pages/tag/index.vue` — Mes Tags

La page la mieux pensée de la partie métier.

**Ce qui fonctionne.** Recherche, filtre Tous/Par défaut/Personnels, sélection multiple avec état indéterminé, suppression en masse, `TransitionGroup` pour les animations de liste, sous-tags réordonnables, distinction visuelle tag par défaut / personnel, bande de couleur en tête de carte, actions révélées au survol mais **forcées visibles sous 768 px** (bon réflexe tactile), `aria-label` sur toutes les cases et boutons.

**Problèmes.**

1. **Le bouton principal « Nouveau tag » est hors palette** : `.modern-fab` utilise un dégradé indigo `rgba(99,102,241) → rgba(79,70,229)`, sans rapport avec le violet de marque `#6508CC`. Idem pour le fallback `var(--p-primary-color, #6366f1)` dans `TagCard`.
2. **La couleur est affichée en hexadécimal brut** (`#A30053`) sous le nom du tag — donnée technique sans valeur pour l'utilisateur final. Remplacer par un nom lisible ou supprimer.
3. **Aucun indicateur d'usage** : ni nombre de transactions, ni montant total par tag. Impossible de savoir si un tag est mort avant de le supprimer, ni d'identifier ses catégories dominantes.
4. **La suppression d'un tag parent ne dit pas ce qu'il advient** de ses sous-tags et des transactions rattachées.
5. **Réordonnancement des sous-tags inopérant au tactile** (HTML5 DnD).
6. **État vide générique** : avec `filterType = 'personal'` et aucun tag personnel, le message parle de recherche ou de « premier tag », sans proposer de revenir au filtre « Tous ».
7. Aucun aperçu de la lisibilité du texte sur la couleur choisie, alors que l'utilitaire `toReadableTagTextColor` existe déjà et est utilisé sur le dashboard.

**Idées.** Compteur d'utilisation et montant cumulé sur chaque carte · fusion de deux tags · réaffectation obligatoire à la suppression (« déplacer les 42 transactions vers… ») · palette de couleurs suggérées assurant le contraste · glisser un sous-tag d'un parent à un autre.

---

### 3.7 `pages/settings/index.vue` — Paramètres

La page visuellement la plus en retrait de l'application.

**Ce qui fonctionne.** `ThemePicker` (clair / sombre / système) est excellent et le seul composant de la page au niveau du design system. La carte de vérification d'e-mail avec cooldown de renvoi est bien traitée.

**Problèmes.**

1. **Rupture visuelle totale** : pas de dégradé de fond (contrairement à toutes les autres pages), `input` / `select` / `button` HTML natifs alors que le reste de l'application utilise PrimeVue, cartes plates sans ombre. On dirait une autre application.
2. **Deux boutons « Enregistrer » de portée différente, non explicitée.** « Changer le mot de passe » (dans sa carte) et « Enregistrer les parametres » en bas de page — ce dernier ne sauvegarde **que** la fenêtre de projection et les cycles mensuels, sur une page qui contient aussi un formulaire de mot de passe et une section de vérification d'e-mail. Le libellé promet donc davantage que ce qu'il fait.
   > **Correction (constatée à l'implémentation).** Une version antérieure de ce point affirmait que cliquer sur « Changer le mot de passe » faisait perdre les réglages en attente. C'est **faux** : `useChangePassword` ne touche que ses propres refs et ne recharge rien. La perte réelle survient à la navigation, faute de garde-fou (point 3).
3. **Aucun indicateur de modifications non enregistrées**, ni garde-fou à la navigation. Le risque est réel puisque la page est longue et le bouton de sauvegarde tout en bas.
4. **Validation invisible** : le champ « Fenêtre de projection » est un `input type=number` `min=7 max=60`, mais saisir `999` n'affiche aucune erreur avant l'appel serveur.
5. **Le concept de cycle mensuel est incompréhensible en l'état.** L'explication est un pavé de trois lignes (« Le début s'applique au mois précédent du mois affiché. La fin peut être personnalisée ; sans valeur, elle est calculée automatiquement (jour de début du cycle suivant - 1) »). Il faut un aperçu calculé en direct : « Le mois de septembre ira du 25/08 au 24/09 ».
6. **Changement de mot de passe sans garde-fous** : ni règles de complexité affichées, ni indicateur de force, ni bouton d'affichage.
7. **Aucune section « Mon compte »** : ni nom d'utilisateur, ni e-mail, ni date de création, ni suppression de compte, ni export de données. Or la politique de confidentialité annonce des droits RGPD (accès, effacement, portabilité) — **écart entre l'engagement légal et l'interface**, et la page consentement affirme même « Vous pouvez les consulter à tout moment depuis vos paramètres », ce qui est faux. À noter : **`DELETE /api/user/me` existe déjà côté backend** et n'est appelé par aucun écran ; combler l'écart est donc un chantier frontend seul (`UX-13`).
8. Fautes d'accents dans quasiment tous les libellés de la page (voir §2.10).
9. Le `select` de cycle est limité à `max-width: 110px` dans une grille `minmax(240px, 360px)` — beaucoup d'espace vide à droite.

**Idées.** Découper en onglets (Compte / Apparence / Projection & cycles / Sécurité / Données) · sauvegarde automatique par section avec confirmation inline, plutôt qu'un bouton global · aperçu en direct du cycle mensuel · export RGPD et suppression de compte · préférence de devise et de format de date.

---

### 3.8 `pages/admin/index.vue` — Console d'administration

**Ce qui fonctionne.** Onglets Utilisateurs / Feature Flags, adaptation mobile avec liste de cartes à la place du tableau, badges de rôle, gestion des flags avec `ToggleSwitch` et libellés lisibles, états vides et de chargement traités.

**Problèmes.**

1. **Le bouton « Actions » de chaque ligne utilisateur est mort.** `icon="pi pi-ellipsis-v"` sans aucun `@click`, en version tableau **et** en version mobile. Il ne peut d'ailleurs pas être branché en l'état : `AdminController` n'expose que `POST /users` et `GET /users`, aucun endpoint de modification ou de suppression d'utilisateur. → Le retirer maintenant (`UX-04`), et traiter les actions réelles comme une feature backend (`UX-38`).
2. **Aucune recherche ni tri** sur la liste des utilisateurs — seule la pagination est disponible. Inutilisable au-delà de quelques dizaines de comptes.
3. **Le formulaire de création occupe le haut de la page en permanence** pour une action ponctuelle, repoussant la liste (l'objet principal) sous la ligne de flottaison. Il devrait être un dialog derrière un bouton « Créer un compte ».
4. **« Minimum 6 caractères »** — règle faible pour une console d'administration, et probablement incohérente avec la politique appliquée par le backend.
5. **L'admin saisit le mot de passe en clair** puis doit le transmettre hors bande. Préférer un mot de passe temporaire généré, ou une invitation par e-mail — d'autant que `force-password-change` existe déjà dans l'application.
6. **Les feature flags basculent sans confirmation ni annulation.** Désactiver `USER_REGISTRATION` coupe l'inscription pour tout le monde en un clic. Il n'y a pas non plus de trace de qui a modifié quoi et quand.
7. Le compteur d'utilisateurs est masqué sur mobile (`v-if="!isMobileWidth"`) sans raison apparente.

**Idées.** Recherche + filtre par rôle · actions par ligne réellement implémentées · confirmation sur les flags à impact global, avec description de l'effet · journal d'audit · statistiques d'usage (comptes actifs, dernière connexion).

---

### 3.9 `consent.vue`, `verify-email.vue`, `force-password-change.vue` — Parcours d'entrée

**Ce qui fonctionne.** Les pages les plus propres du projet : design system respecté (`heading-2`, `body-base`, `btn-primary`), `aria-label` systématiques, `verify-email` gère **cinq états distincts** (chargement, lien absent, succès, expiré, inconnu) avec une icône et une action adaptées à chacun, cooldown de renvoi, mention RGPD sur la page de consentement.

**Problèmes.**

1. **Le layout `centercard` affiche une flèche « retour » fixe en bas à droite qui pointe vers `/`.** Sur `/consent` et `/force-password-change`, ce sont précisément des murs qui ne doivent pas être contournés. Même si le middleware redirige, l'affordance invite à s'échapper et produit une boucle visuelle. À masquer sur ces deux pages.
2. **`NHeader` affiche littéralement « Clair » / « Sombre » en `text-8` (2 rem) en haut à gauche** — un libellé de débogage, très visible, sur une page de consentement juridique.
3. **`centercard` : `w-100 sm:w-125 lg:w-170` avec `p-10`** — `w-100` vaut 400 px sans `max-width`. Sous 400 px de large (iPhone SE, 375 px), débordement horizontal probable. Ajouter `max-w-[calc(100vw-2rem)]`.
4. `force-password-change` n'indique **ni les règles du nouveau mot de passe, ni la raison** du changement forcé (expiration ? compte créé par un admin ? incident de sécurité ?).
5. `verify-email` : il manque un lien « contacter le support » sur les états d'échec.

---

### 3.10 `privacy.vue` / `terms.vue` — Pages légales

**Ce qui fonctionne.** Les seules pages à utiliser pleinement l'échelle typographique (`heading-1/3/4`, `body-base`, `text-caption`). Tableaux avec conteneur `overflow-x-auto` (respect de la règle « le corps de page ne défile jamais horizontalement »), versionnage affiché, contact du responsable de traitement, `aria-hidden` sur les icônes décoratives.

**Problèmes.**

1. **23 sections sans sommaire ancré ni bouton « retour en haut »** — navigation pénible sur un document de cette longueur, surtout sur mobile.
2. **Le lien de retour du layout `legal` pointe en dur vers `/login`.** Un utilisateur connecté qui ouvre les CGU depuis la sidebar est renvoyé à l'écran de connexion. Il faut un retour contextuel (`router.back()` ou `/` si authentifié).
3. **Deux comportements d'ouverture** : `target="_blank"` depuis le formulaire d'inscription, navigation interne depuis la sidebar.
4. Pas d'historique des versions ni de résumé des changements — utile pour un document dont l'acceptation est horodatée et versionnée côté serveur.

---

### 3.11 `[...all].vue` — Page 404

```vue
<template>
  <h1>Error 404</h1>
</template>
```

Sans layout, sans style, sans lien de retour, en anglais alors que toute l'application est en français. **Toute URL erronée aboutit à une page qui ressemble à une application cassée.** À reconstruire avec le layout `centercard` : illustration, message en français, bouton « Retour au tableau de bord ».

---

### 3.12 `user/[id].vue` — Profil utilisateur

```vue
<template>
  <h1>User {{ route.params.id }}</h1>
</template>
```

Route accessible en production, protégée par le middleware d'authentification, mais vide. À supprimer ou à implémenter.

---

## 4. Plan d'action priorisé

> **Le backlog d'exécution complet — 63 items chiffrés, séquencés en 7 lots, avec fiches
> d'exécution et critères d'acceptation — se trouve dans [`UX_BACKLOG.md`](./UX_BACKLOG.md).**
> Les tableaux ci-dessous en sont le résumé.

Trois réserves issues de l'inspection du backend, qui modifient les priorités brutes :

- **« Mot de passe oublié » n'a aucun endpoint** (`forgot` / `reset-password` : 0 occurrence dans le backend). Ce n'est pas un correctif mais une feature full-stack — traitée en P1 (`UX-21`), pas en P0.
- **Le menu « Actions » de l'admin ne peut pas être branché en l'état** : `AdminController` n'expose que `POST /users` et `GET /users`. Le correctif immédiat est de **retirer** le contrôle mort ; les actions réelles sont une feature backend (`UX-38`).
- **`DELETE /api/user/me` existe déjà** côté backend et n'est appelé par aucun écran. L'écart RGPD des Paramètres se comble donc **en frontend seul** (`UX-13`) — c'est un des meilleurs rapports impact/coût du chantier.
- **`GET /booklet/{id}/transactions` accepte déjà `startDate` / `endDate`.** Le filtre par plage de dates du livret est également frontend seul (`UX-14`).

### P0 — Défauts fonctionnels (à corriger en premier)

| # | Sujet | Fichier(s) |
|---|---|---|
| 1 | Le hamburger recouvre le contenu sur 6 pages en mobile | `layouts/sidebar-layout.vue` |
| 2 | Retirer le bouton « Actions » mort de la console admin | `pages/admin/index.vue` |
| 3 | Page 404 nue | `pages/[...all].vue` |
| 4 | Moyenne journalière divisée par 30 quelle que soit la période | `pages/index.vue` |
| 5 | Flèche de sortie sur `/consent` et `/force-password-change` | `layouts/centercard.vue` |
| 6 | Scroll-jacking des graphiques du dashboard | `pages/index.vue` |
| 7 | Deux boutons « Enregistrer » ambigus, perte de saisie | `pages/settings/index.vue` |
| 8 | Focus clavier invisible (`outline: none !important`) | `components/booklet/*` |
| 9 | Fautes d'accents visibles par l'utilisateur | §2.10 |
| 10 | Suppression du code mort (5 fichiers) | §2.9 |

### P1, P2, P3

Détaillés, chiffrés et séquencés dans [`UX_BACKLOG.md`](./UX_BACKLOG.md) — 53 items supplémentaires
répartis en :

- **P1 — UX majeure** (`UX-11` → `UX-27`, ~14 j) : recherche de transactions, « Mon compte » / RGPD, découvrabilité des actions sur les régulières, tokens sémantiques, budget côté serveur, refonte du dashboard.
- **P2 — Cohérence et finition** (`UX-28` → `UX-50`) : migration vers le design system, i18n, accessibilité du drag & drop, console admin, parcours d'authentification.
- **P3 — Roadmap produit** (`UX-51` → `UX-63`) : recherche globale ⌘K, budgets par tag, pointage bancaire, saisie assistée, PWA.

---

## 5. Idées produit (au-delà de la correction)

1. **Recherche globale (⌘K)** — transactions, tags, livrets, actions.
2. **Filtres avancés persistants** dans un livret : texte, plage de dates, plage de montants, type, tag, avec chips supprimables.
3. **Solde courant par ligne** + courbe de solde du mois.
4. **Vue « Tous les comptes »** réellement agrégée sur le dashboard.
5. **Budgets par tag, côté serveur**, avec jauges et alertes — remplace le budget global en `localStorage`.
6. **Pointage bancaire** : marquer une transaction comme rapprochée du relevé.
7. **Saisie assistée** : autocomplétion du libellé depuis l'historique, tag et montant pré-remplis, duplication en un clic.
8. **Annulation (undo) via le toast** après suppression, à la place d'une confirmation modale systématique.
9. **Onboarding en trois étapes** au premier login (livret → tag → première transaction) plutôt que quatre états vides indépendants.
10. **Exports enrichis** : PDF/Excel, et export multi-mois.
11. **Raccourcis clavier** sur le tableau : `n` nouvelle transaction, `/` recherche, `j/k` navigation.
12. **PWA installable** — l'application est déjà `ssr: false` et fortement pensée mobile.
13. **Rapport mensuel** : récapitulatif automatique par e-mail (l'infrastructure e-mail existe déjà).
