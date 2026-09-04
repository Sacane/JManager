# Validation manuelle — lot 3

> Cas d'usage à dérouler sur le client web au fur et à mesure des livraisons du lot 3.
> Branche : `fix/ux-lot-3-conformite`.
> Ce document est **complété à chaque poussée**, pas à la fin du lot.

**Avancement** — 3 items sur 9 livrés.

| Item | Statut |
|---|---|
| UX-46 · autocomplete sur les formulaires d'auth | ✅ à valider |
| UX-48 · e-mail conservé à la bascule | ✅ à valider |
| UX-49 · liens légaux sur la connexion | ✅ à valider |
| UX-47 · champ mot de passe partagé | 🚧 en cours |
| UX-45 · limite de 6 livrets | ⏳ |
| UX-43 · accueil dashboard vide | ⏳ |
| UX-17 · confirmation forte de suppression | ⏳ |
| UX-14 · filtre par plage de dates | ⏳ |
| UX-13 · Mon compte / RGPD | ⏳ |

---

## Préparation

```bash
./gradlew :application:bootRun
```

```bash
cd client && pnpm dev
```

**Se déconnecter** avant de commencer : tous les cas ci-dessous se jouent sur la page de connexion.

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

## 4. Régressions à surveiller

| # | Vérification | Pourquoi |
|---|---|---|
| 4.1 | Se connecter normalement | `login.vue` a été modifié en profondeur |
| 4.2 | Créer un compte de bout en bout | Idem |
| 4.3 | Vérifier que les cases CGU / Confidentialité bloquent toujours l'inscription tant qu'elles ne sont pas cochées | Les liens légaux ont été ajoutés à côté |
| 4.4 | Saisir des identifiants faux | Le message « Identifiants incorrects » s'affiche toujours |
| 4.5 | Vérifier que le lien « S'inscrire » **disparaît** quand le flag est désactivé | Le `FeatureGate` n'a pas été touché, mais la zone alentour si |

---

## Ce que la suite automatisée couvre déjà

Les 6 valeurs d'`autocomplete`, le report de l'e-mail dans les deux sens, le non-report des mots de
passe, le non-écrasement par un e-mail vide, et la présence des deux liens légaux dans les deux
modes. **21 tests sur `login.vue`.**

Ce qu'elle **ne peut pas** couvrir, et qui justifie ce document : le comportement réel du
gestionnaire de mots de passe du navigateur (parcours 1b), et le rendu du pied de carte.
