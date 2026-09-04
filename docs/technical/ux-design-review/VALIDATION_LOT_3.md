# Validation manuelle — lot 3

> Cas d'usage à dérouler sur le client web au fur et à mesure des livraisons du lot 3.
> Branche : `fix/ux-lot-3-conformite`.
> Ce document est **complété à chaque poussée**, pas à la fin du lot.

**Avancement** — 8 items sur 9 livrés.

| Item | Statut |
|---|---|
| UX-46 · autocomplete sur les formulaires d'auth | ✅ à valider |
| UX-48 · e-mail conservé à la bascule | ✅ à valider |
| UX-49 · liens légaux sur la connexion | ✅ à valider |
| UX-47 · champ mot de passe partagé | ✅ à valider |
| UX-45 · limite de 6 livrets | ✅ à valider |
| UX-43 · accueil dashboard vide | ⏳ |
| UX-17 · confirmation forte de suppression | ✅ à valider |
| UX-14 · filtre par plage de dates | ✅ à valider |
| UX-13 · Mon compte / RGPD | ✅ à valider |

---

## Préparation

```bash
./gradlew :application:bootRun
```

```bash
cd client && pnpm dev
```

**Parcours 1 à 4** : se déconnecter avant de commencer, ils se jouent sur la page de connexion.

**Parcours 5 et 6** : il faut un compte avec **au moins un livret contenant des transactions**, et
pour le cas 5.4 un compte ayant atteint **les 6 livrets**. Si tu ne peux pas monter jusqu'à 6,
note le cas comme non testé plutôt que de le déclarer OK.

Pour le parcours 1, il faut **un gestionnaire de mots de passe actif** — celui du navigateur suffit
(Chrome : `Paramètres → Saisie automatique → Mots de passe`, activer la proposition
d'enregistrement). Sans lui, ce parcours n'est pas testable : le noter plutôt que de le déclarer OK.

---

## 1. Gestionnaire de mots de passe — UX-46

Le plus difficile à valider, parce que le comportement dépend du navigateur. **Deux niveaux** :
l'inspection du DOM prouve que la déclaration est correcte, l'essai réel prouve que le
navigateur en tire parti.

### 1a. Inspection (fiable, rapide)

Ouvrir `/login`, puis les DevTools → Elements.

| # | Champ | Attendu |
|---|---|---|
| 1.1 | `#email` | `autocomplete="email"` |
| 1.2 | `#password` | `autocomplete="current-password"` |
| 1.3 | Basculer sur l'inscription, `#reg-username` | `autocomplete="username"` |
| 1.4 | `#reg-email` | `autocomplete="email"` |
| 1.5 | `#reg-password` | `autocomplete="new-password"` |
| 1.6 | `#reg-confirm-password` | `autocomplete="new-password"` |

> ⚠️ Les deux champs de mot de passe d'inscription portent **volontairement la même valeur**
> `new-password`. C'est le motif documenté pour un mot de passe et sa confirmation — ce n'est
> pas une erreur de copier-coller.

### 1b. Essai réel

| # | Action | Attendu |
|---|---|---|
| 1.7 | Avoir un identifiant enregistré pour le site, ouvrir `/login`, cliquer le champ e-mail | Le navigateur **propose de remplir** e-mail + mot de passe |
| 1.8 | Accepter la proposition | Les deux champs se remplissent, la connexion fonctionne |
| 1.9 | S'inscrire avec un nouvel e-mail | À la soumission, le navigateur **propose d'enregistrer** le nouvel identifiant |
| 1.10 | Vérifier que la proposition ne porte pas sur le champ de confirmation | Un seul identifiant proposé, pas deux |

---

## 2. Bascule connexion ↔ inscription — UX-48

> Prérequis : le flag `USER_REGISTRATION` doit être **actif**, sinon le lien « S'inscrire » n'est
> pas rendu. Il se bascule depuis la console d'administration, onglet Feature Flags.

| # | Action | Attendu |
|---|---|---|
| 2.1 | Sur la connexion, saisir `test@exemple.fr` dans l'e-mail, **ne rien saisir d'autre** | — |
| 2.2 | Cliquer « S'inscrire » | ⚠️ Le champ e-mail d'inscription est **déjà rempli** avec `test@exemple.fr` |
| 2.3 | Modifier l'e-mail en `autre@exemple.fr`, cliquer « Se connecter » | Le champ e-mail de connexion contient `autre@exemple.fr` |
| 2.4 | Revenir sur l'inscription | L'e-mail est toujours `autre@exemple.fr`, **il n'a pas été effacé** |
| 2.5 | Saisir un mot de passe en connexion, basculer sur l'inscription | ⚠️ Les deux champs de mot de passe d'inscription sont **vides** — un mot de passe ne doit jamais être reporté |
| 2.6 | Saisir un mot de passe en inscription, revenir sur la connexion | Le champ mot de passe de connexion est **vide** |
| 2.7 | Provoquer une erreur de connexion, puis basculer sur l'inscription | Le message d'erreur a disparu |

---

## 3. Documents légaux — UX-49

| # | Action | Attendu |
|---|---|---|
| 3.1 | Ouvrir `/login` en **mode connexion** | ⚠️ Deux liens en pied de carte : « Conditions Générales d'Utilisation » et « Politique de Confidentialité » |
| 3.2 | Basculer sur l'inscription | Les **mêmes liens sont toujours là**, en plus de ceux des cases à cocher |
| 3.3 | Cliquer « Politique de Confidentialité » | La politique s'affiche |
| 3.4 | Utiliser la flèche retour en bas à gauche | Retour sur `/login` |
| 3.5 | Refaire 3.3 avec les CGU | Idem |
| 3.6 | Regarder le pied de carte en **thème sombre** | Liens et séparateur lisibles |
| 3.7 | En mobile 375 px | Les deux liens passent à la ligne proprement, sans débordement |

---

## 4. Champ mot de passe partagé — UX-47

Sept champs sur trois pages passent désormais par le même composant. **Chacun doit être vérifié**,
parce que chaque page l'appelle avec des options différentes.

### 4a. Le contrôle lui-même

Sur `/login`, champ « Mot de passe » :

| # | Action | Attendu |
|---|---|---|
| 4.1 | Saisir un mot de passe | Il s'affiche masqué (points) |
| 4.2 | Cliquer l'icône œil à droite du champ | ⚠️ Le mot de passe devient **lisible en clair**, l'icône passe en œil barré |
| 4.3 | Cliquer à nouveau | Il redevient masqué |
| 4.4 | Saisir un mot de passe **très long** | Le texte ne passe **pas sous l'icône** |
| 4.5 | Cliquer l'icône alors que le formulaire est rempli | ⚠️ Le formulaire **n'est pas soumis** — le bouton est un `type="button"` |
| 4.6 | Survoler l'icône | Une infobulle indique l'action (« Afficher » / « Masquer ») |
| 4.7 | Au clavier : `Tab` jusqu'à l'icône, `Entrée` | La bascule fonctionne, l'anneau de focus est visible |

### 4b. Les sept champs, page par page

| # | Écran | Champs à vérifier | Attendu |
|---|---|---|---|
| 4.8 | `/login` connexion | Mot de passe | Icône présente et fonctionnelle |
| 4.9 | `/login` inscription | Mot de passe + Confirmation | Les deux ont leur icône, **indépendantes l'une de l'autre** |
| 4.10 | Paramètres → Changer le mot de passe | Actuel, Nouveau, Confirmer | Les trois ont leur icône |
| 4.11 | Écran de changement forcé | Nouveau, Confirmer | Les deux ont leur icône |

### 4c. Ce qui ne doit pas avoir régressé

| # | Vérification | Pourquoi |
|---|---|---|
| 4.12 | ⚠️ Dans les **Paramètres**, comparer visuellement les 3 champs mot de passe avec le champ « Fenêtre de projection » juste au-dessus | Les mots de passe sont maintenant rendus par le composant ; leur style scoped a dû être étendu avec `:deep()`. **S'ils ne se ressemblent plus, c'est un défaut à signaler** |
| 4.13 | Refaire le parcours 1a (autocomplete) sur les 4 champs de mot de passe | Les valeurs doivent avoir survécu à l'encapsulation |
| 4.14 | Changer réellement son mot de passe depuis les Paramètres | La liaison de valeur passe par le composant |
| 4.15 | Se connecter réellement | Idem |
| 4.16 | Sur l'écran de changement forcé, saisir deux mots de passe différents | Le message d'erreur s'affiche toujours |
| 4.17 | Mobile 375 px, sur les 4 écrans | L'icône reste dans le champ, rien ne déborde |

---

## 5. Limite de livrets — UX-45

| # | Action | Attendu |
|---|---|---|
| 5.1 | Ouvrir **Mes livrets** avec 4 livrets | Le badge indique `4/6` et la carte d'ajout annonce « 2 emplacements restants » |
| 5.2 | Créer un livret de plus | Le badge passe à `5/6`, « 1 emplacement restant » (**au singulier**) |
| 5.3 | Monter jusqu'à 6 livrets | ⚠️ Le bouton devient « Limite atteinte », **avec sous lui une phrase expliquant** le maximum et invitant à en supprimer un |
| 5.4 | La carte pointillée « Ajouter un livret » | Elle **disparaît** à 6 livrets |
| 5.5 | Supprimer un livret | Le bouton redevient actif, l'explication disparaît |
| 5.6 | En mobile 375 px, à 6 livrets | L'explication passe à la ligne proprement, alignée à gauche |

---

## 6. Suppression d'un livret — UX-17

⚠️ **Le parcours le plus sensible du lot** : il s'agit d'une destruction définitive.
Faire ces essais sur un livret **dont tu te fiches**.

| # | Action | Attendu |
|---|---|---|
| 6.1 | Cliquer la corbeille d'un livret nommé par ex. « Livret A » | Une fenêtre s'ouvre : « Supprimer ce livret ? » |
| 6.2 | Lire le message | Il dit « Cette action **est** irréversible » — et, **si le livret a des transactions**, il en annonce le nombre |
| 6.3 | ⚠️ Livret **sans** transaction | Il ne doit **jamais** afficher « 0 transaction », mais « toutes ses transactions » |
| 6.4 | Sans rien saisir, regarder le bouton rouge | Il est **désactivé** |
| 6.5 | Saisir un nom **différent** (« Livret B ») | Le bouton reste désactivé |
| 6.6 | Saisir le nom exact | Le bouton devient actif |
| 6.7 | Saisir le nom avec des espaces autour et en minuscules | ⚠️ Le bouton devient **quand même** actif — la casse et les espaces sont tolérés |
| 6.8 | Annuler, puis rouvrir la fenêtre sur **un autre** livret | ⚠️ Le champ est **vide** — ce qui avait été tapé pour le précédent n'est pas conservé |
| 6.9 | Confirmer réellement une suppression | Le livret disparaît, toast de succès, la liste se recharge |
| 6.10 | Pendant la suppression | Le bouton passe en chargement, la fenêtre ne se ferme pas au clic extérieur |
| 6.11 | En mobile 375 px | La fenêtre tient dans l'écran |

---

## 7. Mon compte et suppression RGPD — UX-13

🔴 **Le parcours le plus destructeur de tout le chantier.** La suppression efface le compte, les
livrets, les transactions et les tags. **Utiliser un compte jetable**, jamais le compte principal.

### 7a. Affichage de l'identité

| # | Action | Attendu |
|---|---|---|
| 7.1 | Ouvrir **Paramètres** | Une section **« Mon compte »** est présente, avant « Changer le mot de passe » |
| 7.2 | Lire la section | Le **nom d'utilisateur** et l'**adresse e-mail** du compte connecté sont affichés |
| 7.3 | Compte sans e-mail renseigné | La mention « Non renseignée » s'affiche à la place |

### 7b. Le garde-fou

| # | Action | Attendu |
|---|---|---|
| 7.4 | Repérer la zone rouge en bas de la section | Elle explique ce qui sera effacé et porte un bouton « Supprimer mon compte » |
| 7.5 | Cliquer ce bouton | Une fenêtre s'ouvre — ⚠️ **rien n'est supprimé à ce stade** |
| 7.6 | Lire le message | Il dit « irréversible » et énumère compte, livrets, transactions, tags |
| 7.7 | Sans rien saisir | Le bouton rouge est **désactivé** |
| 7.8 | Saisir un nom d'utilisateur **différent** | Il reste désactivé |
| 7.9 | Saisir son propre nom d'utilisateur | Il devient actif |
| 7.10 | Annuler | Rien n'est supprimé, la fenêtre se ferme |
| 7.11 | Rouvrir la fenêtre | Le champ est vide |

### 7c. La suppression réelle — sur un compte jetable

| # | Action | Attendu |
|---|---|---|
| 7.12 | Confirmer la suppression | Le compte est supprimé, **déconnexion** et redirection vers `/login` |
| 7.13 | Tenter de se reconnecter avec ces identifiants | La connexion échoue |
| 7.14 | ⚠️ **Simuler un échec** — couper le backend, puis confirmer | Un message d'erreur s'affiche, la fenêtre **reste ouverte**, et vous êtes **toujours connecté** |
| 7.15 | Après 7.14, naviguer dans l'application | La session fonctionne toujours normalement |

> Le cas **7.14** est le plus important : un échec ne doit jamais ressembler à un succès. S'il ne
> peut pas être reproduit, le noter comme non testé.

### 7d. Non-régression du dialog partagé

Le mécanisme « retaper pour confirmer » est désormais **le même composant** pour la suppression
d'un livret et celle d'un compte.

| # | Vérification |
|---|---|
| 7.16 | Refaire rapidement le parcours 6 (suppression d'un livret) — le comportement doit être inchangé |

---

## 8. Plage de dates sur un livret — UX-14

🔴 **Le parcours le plus important du lot** : la contrainte n'est pas « la liste se filtre », c'est
que **tout chiffre affiché décrive la même période**. C'est ça qu'il faut vérifier.

Prérequis : un livret avec des transactions **réparties sur au moins deux mois**.

### 8a. La plage pilote toute la page

| # | Action | Attendu |
|---|---|---|
| 8.1 | Ouvrir un livret | La barre « Période » indique le mois courant et « tout le mois » |
| 8.2 | Noter les deux soldes de l'en-tête (Réel / Prévis.) et le nombre de transactions | — |
| 8.3 | Choisir une plage **plus courte que le mois**, cliquer « Appliquer » | ⚠️ La liste se réduit **et les deux soldes changent**. Un solde figé sur le mois serait le défaut principal à signaler |
| 8.4 | Vérifier le compteur de transactions de l'en-tête | Il correspond au nombre de lignes de la plage |
| 8.5 | Vérifier la pagination en bas | Le nombre total de pages correspond à la plage, pas au mois |
| 8.6 | Cliquer « Tout le mois » avec la plage active | ⚠️ Le libellé ne dit plus « du mois » mais **affiche la plage**, et les transactions listées sont celles de la plage |
| 8.7 | Sélectionner des lignes | Le total sélectionné ne compte que des transactions de la plage |
| 8.8 | Lancer l'export CSV | ⚠️ Le message de confirmation annonce **la plage**, pas « pour mars 2026 » |
| 8.9 | Choisir une plage **à cheval sur deux mois** | Les transactions des deux mois apparaissent, les soldes les intègrent |

### 8b. Retour et validation

| # | Action | Attendu |
|---|---|---|
| 8.10 | Cliquer « Revenir au mois » | Tout revient à la vue mensuelle : soldes, liste, compteurs, libellés |
| 8.11 | Saisir une date de fin **antérieure** à la date de début, appliquer | Un message d'erreur s'affiche, **rien n'est rechargé** |
| 8.12 | Corriger la date de fin, appliquer | L'erreur disparaît, la plage s'applique |
| 8.13 | Ne renseigner qu'une seule des deux dates, appliquer | Message demandant les deux bornes |
| 8.14 | Changer de mois avec le sélecteur pendant qu'une plage est active | ⚠️ Vérifier le comportement et **signaler s'il est déroutant** — le mois et la plage sont deux navigations concurrentes |

### 8c. La limite assumée

| # | Action | Attendu |
|---|---|---|
| 8.15 | Sur un mois **sans** plage, avec des prévisionnelles supprimées | Le bouton de **régénération** est disponible |
| 8.16 | Appliquer une plage | ⚠️ Le bouton de régénération **disparaît** — c'est voulu : les endpoints de régénération ne connaissent que le mois, l'action porterait sur une autre période que celle affichée |
| 8.17 | Revenir au mois | Le bouton réapparaît |

### 8d. Mobile

| # | Action | Attendu |
|---|---|---|
| 8.18 | En 375 px, appliquer une plage | La barre de période passe à la ligne proprement |
| 8.19 | Faire défiler pour charger plus de transactions | Le chargement paresseux reste dans la plage |

---

## 9. Régressions à surveiller

| # | Vérification | Pourquoi |
|---|---|---|
| 9.1 | Se connecter normalement | `login.vue` a été modifié en profondeur |
| 9.2 | Créer un compte de bout en bout | Idem |
| 9.3 | Vérifier que les cases CGU / Confidentialité bloquent toujours l'inscription tant qu'elles ne sont pas cochées | Les liens légaux ont été ajoutés à côté |
| 9.4 | Saisir des identifiants faux | Le message « Identifiants incorrects » s'affiche toujours |
| 9.5 | Vérifier que le lien « S'inscrire » **disparaît** quand le flag est désactivé | Le `FeatureGate` n'a pas été touché, mais la zone alentour si |

---

## Ce que la suite automatisée couvre déjà

Les 6 valeurs d'`autocomplete`, le report de l'e-mail dans les deux sens, le non-report des mots de
passe, le non-écrasement par un e-mail vide, la présence des deux liens légaux dans les deux modes
(**21 tests sur `login.vue`**), la bascule du type de champ, le nom accessible du contrôle, le
`type="button"`, la liaison de valeur, le relais de `id` / `autocomplete` / `maxlength`, l'état
désactivé et le slot d'extension (**11 tests sur `PasswordField`**), la constante de limite, le
pluriel des emplacements restants, la présence et le rattachement `aria-describedby` de
l'explication (**5 tests**), et tout le comportement de la fenêtre de suppression — bouton
désactivé, tolérance casse/espaces, refus d'un nom différent, remise à zéro à la réouverture,
absence de « 0 transaction » (**15 tests répartis entre `ConfirmByTypingDialog` et `BookletDeleteDialog`**), l'appel de suppression de compte et le maintien de la session en cas d'échec (**4 tests sur `useAuth`**), l'affichage de l'identité et le garde-fou de la fenêtre (**6 tests sur les Paramètres**), et la propagation de la plage aux trois endpoints, son effacement, le refus d'une plage invalide sans requête et la désactivation de la régénération (**9 tests**).

Ce qu'elle **ne peut pas** couvrir, et qui justifie ce document : le comportement réel du
gestionnaire de mots de passe du navigateur (1b), le rendu du pied de carte (3.6, 3.7), et surtout
**l'apparence des champs mot de passe des Paramètres** (4.12) — leur style scoped a dû être étendu
avec `:deep()` pour atteindre l'input rendu par le composant enfant, ce qu'aucun test ne vérifie.
