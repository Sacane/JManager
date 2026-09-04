# Validation manuelle — lots 1 et 2

> Cas d'usage à dérouler sur le client web pour valider les 16 items livrés.
> Branches : `fix/ux-lot-1-assainissement` (10 items P0) et `fix/ux-lot-2-socle-visuel` (6 items).
> La suite automatisée (512 tests) tourne sur happy-dom, **sans moteur de rendu** : tout ce qui
> relève du visuel, du focus ou du dépassement de largeur n'est pas couvert et n'existe que dans
> cette liste.

---

## 0. Préparation

**Lancer l'application**

```bash
./gradlew :application:bootRun
```

```bash
cd client && pnpm dev
```

**Jeu de données nécessaire**

| Besoin | Pourquoi |
|---|---|
| Un compte avec **au moins 2 livrets**, dont un avec des transactions sur le mois courant | Dashboard, détail livret, liste des livrets |
| Au moins **une recette et une dépense** sur le même livret | Vérifier le code couleur côté à côté |
| Au moins **une transaction prévisionnelle** | Distinguer prévisionnel et dépense |
| Au moins **2 transactions régulières** | Page Régulières |
| Au moins **un tag personnel avec un sous-tag** | Page Tags |
| Un compte **administrateur** | Console admin |
| Un compte avec **e-mail non vérifié** | Section vérification dans les Paramètres (facultatif) |

**Priorité** : les parcours 1, 2 et 6 sont ceux que la suite ne couvre pas du tout. Si le temps
manque, faire ceux-là.

---

## 1. 🔴 Mobile 375 px — le plus à risque

Ouvrir les DevTools, mode responsive, largeur **375 px** (iPhone SE). Recharger après le changement
de largeur.

| # | Action | Attendu | Item |
|---|---|---|---|
| 1.1 | Ouvrir le **tableau de bord** | Le titre « Bonjour, … » est entièrement lisible, **rien n'est caché derrière le bouton de menu** en haut à gauche | UX-01 |
| 1.2 | Ouvrir **Mes livrets** | L'icône et le titre « Mes Livrets » ne sont pas recouverts | UX-01 |
| 1.3 | Ouvrir le **détail d'un livret** | ⚠️ Le **bouton retour** est visible et **cliquable au doigt** — c'était le pire cas avant | UX-01 |
| 1.4 | Ouvrir **Transactions régulières**, **Paramètres**, **Administration** | Aucun titre recouvert | UX-01 |
| 1.5 | Ouvrir **Mes tags** | Le titre est correctement placé, **sans espace vide excessif** en haut (le décalage local a été retiré, il ne doit pas s'additionner à celui du layout) | UX-01 |
| 1.6 | Sur chaque page ci-dessus, tenter de **faire défiler horizontalement** | Aucun défilement latéral | UX-01 |
| 1.7 | Ouvrir une URL inexistante, ex. `/nimportequoi` | La carte tient dans l'écran, **pas de défilement latéral** | UX-33 |
| 1.8 | Se déconnecter puis ouvrir le lien de vérification d'e-mail | Idem : la carte tient dans l'écran | UX-33 |

Repasser en largeur desktop : **les mises en page doivent être inchangées** par rapport à avant.

---

## 2. 🔴 Navigation au clavier — non couvert par les tests

Souris **posée**, uniquement `Tab`, `Maj+Tab` et `Entrée`.

| # | Action | Attendu | Item |
|---|---|---|---|
| 2.1 | Ouvrir le **détail d'un livret**, parcourir la barre de filtres au `Tab` | ⚠️ **Chaque contrôle focalisé montre un anneau violet visible** — c'était totalement invisible avant | UX-10 |
| 2.2 | Continuer jusqu'aux sélecteurs Mois / Année | Anneau visible également | UX-10 |
| 2.3 | Basculer en **thème sombre**, refaire 2.1 | L'anneau reste visible sur fond sombre | UX-10 |
| 2.4 | Aller dans **Paramètres**, `Tab` jusqu'aux sélecteurs de cycle mensuel | Anneau visible | UX-10 |
| 2.5 | Cliquer ces mêmes sélecteurs **à la souris** | **Pas** d'anneau — l'aplatissement au pointeur est conservé | UX-10 |
| 2.6 | Sur n'importe quelle page, `Tab` jusqu'à un bouton puis `Entrée` | L'action se déclenche normalement | UX-10 |

---

## 3. Tableau de bord

| # | Action | Attendu | Item |
|---|---|---|---|
| 3.1 | Lire « Moy. journalière » sur la carte Dépenses en vue **Mois** | Valeur = dépenses ÷ nombre de jours réel du mois | UX-03 |
| 3.2 | Basculer en **Trimestre** | ⚠️ La moyenne **diminue nettement** (÷ ~90 au lieu de ÷ 30). Avant, elle était ~3× trop élevée | UX-03 |
| 3.3 | Basculer en **Année** | La moyenne diminue encore (÷ ~365) | UX-03 |
| 3.4 | Placer le curseur **sur le graphique d'évolution** et faire défiler à la molette | ⚠️ **La page défile normalement**, le graphique ne bouge pas | UX-08 |
| 3.5 | `Ctrl` (ou `⌘`) **+ molette** sur le même graphique | L'échelle de l'axe Y change, la page ne défile pas | UX-08 |
| 3.6 | Après 3.5, regarder l'en-tête du graphique | Un bouton **« Réinitialiser l'échelle »** est apparu | UX-08 |
| 3.7 | Cliquer ce bouton | L'échelle revient en automatique et **le bouton disparaît** | UX-08 |
| 3.8 | Refaire 3.4 → 3.7 sur le graphique **Comparaison de période** | Même comportement, indépendant du premier | UX-08 |
| 3.9 | Regarder les montants des **Prochaines transactions** | Recettes en **vert**, dépenses en **rouge** | UX-12 |
| 3.10 | Regarder les badges de variation (%) des 4 cartes du haut | Vert quand favorable, rouge quand défavorable | UX-12 |
| 3.11 | Basculer en **thème sombre** | ⚠️ **Les courbes du graphique changent de teinte** — elles étaient identiques dans les deux thèmes avant | UX-12 |
| 3.12 | Comparer l'aspect des cartes | ⚠️ Cartes **plus plates** qu'avant : coins moins arrondis, ombre plus légère. **C'est le changement le plus visible du lot 2** — dis-moi si ça ne va pas | UX-30 |
| 3.13 | Vérifier l'espacement autour du contenu | Un peu plus d'air qu'avant (padding 1,25 → 2 rem) | UX-30 |

---

## 4. Livrets et transactions

| # | Action | Attendu | Item |
|---|---|---|---|
| 4.1 | Ouvrir **Mes livrets** | Soldes positifs en **vert**, négatifs en **rouge** | UX-12 |
| 4.2 | Ouvrir le **détail d'un livret** avec recettes et dépenses | ⚠️ Les recettes sont **vertes** — elles étaient **bleues** sur cette page uniquement | UX-12 |
| 4.3 | Regarder une ligne de dépense, colonne **Recettes** | Le tiret est **gris neutre**, pas bleu | UX-12 |
| 4.4 | Repérer une transaction **prévisionnelle** | Toujours distinguée (fond ambré, icône horloge) et **non confondue** avec une dépense | UX-12 |
| 4.5 | En mobile, ouvrir le détail d'un livret | Le bouton de validation d'une prévisionnelle est **vert**, cohérent avec les autres écrans | UX-12 |
| 4.6 | Ouvrir **Transactions régulières** | Recettes vertes préfixées `+`, dépenses rouges préfixées `−` | UX-12 |
| 4.7 | Comparer un montant de recette entre les 3 écrans | **Exactement la même couleur** partout | UX-12 |
| 4.8 | Tenter de supprimer un livret | Le message dit « Cette action **est** irréversible » et « transactions **enregistrées** » | UX-07 |

---

## 5. Paramètres

| # | Action | Attendu | Item |
|---|---|---|---|
| 5.1 | Ouvrir **Paramètres** | Le titre est « **Paramètres** utilisateur », correctement accentué | UX-07 |
| 5.2 | Lire la section Projection | « **Définis** … les **prévisions** » et « **Fenêtre** de projection (7 **à** 60 jours) » | UX-07 |
| 5.3 | Regarder le bouton en bas de page | Il indique « **Enregistrer la projection et les cycles** », pas « les paramètres » | UX-06 |
| 5.4 | Modifier la fenêtre de projection **sans enregistrer** | Un badge **« Non enregistré »** apparaît sur la section Projection, et un indice près du bouton | UX-06 |
| 5.5 | Toujours sans enregistrer, cliquer **Changer le mot de passe** (champs vides) | La modification de projection **est conservée**, le badge reste | UX-06 |
| 5.6 | Toujours sans enregistrer, naviguer vers une autre page | ⚠️ Une **confirmation** apparaît avant de quitter | UX-06 |
| 5.7 | Choisir « Rester sur la page » | On reste, la modification est intacte | UX-06 |
| 5.8 | Refaire 5.6 et choisir « Quitter sans enregistrer » | La navigation se fait | UX-06 |
| 5.9 | Enregistrer, puis regarder la section | Le badge **disparaît**, toast « Paramètres enregistrés » accentué | UX-06 / UX-07 |

---

## 6. 🔴 Écrans d'onboarding — sortie interdite

| # | Action | Attendu | Item |
|---|---|---|---|
| 6.1 | Se connecter avec un compte dont le **consentement est requis** | ⚠️ **Aucune flèche de retour** en bas à droite | UX-05 |
| 6.2 | Se connecter avec un compte marqué **mustChangePassword** | ⚠️ Aucune flèche de retour non plus | UX-05 |
| 6.3 | Ouvrir un lien de **vérification d'e-mail** | La flèche de retour **est toujours là** — cet écran n'est pas un mur | UX-05 |
| 6.4 | Sur ces trois écrans, regarder le coin haut gauche | ⚠️ **Plus de « Clair » / « Sombre » en gros texte** | UX-32 |
| 6.5 | Survoler / focaliser le bouton de thème en haut à droite | Il a un nom accessible (« Passer au thème sombre / clair ») et **bascule bien le thème** | UX-32 |

> 6.1 et 6.2 nécessitent un état de compte particulier. S'ils ne sont pas reproductibles
> facilement, le noter plutôt que de conclure que ça marche.

---

## 7. Page inexistante

| # | Action | Attendu | Item |
|---|---|---|---|
| 7.1 | **Connecté**, ouvrir `/nimportequoi` | Page stylée en français, « Cette page n'existe pas », bouton **« Retour au tableau de bord »** | UX-02 |
| 7.2 | Cliquer ce bouton | Retour au tableau de bord | UX-02 |
| 7.3 | **Déconnecté**, ouvrir `/nimportequoi` | Bouton **« Aller à la page de connexion »** | UX-02 |
| 7.4 | Refaire 7.1 en **thème sombre** | La page suit le thème | UX-02 |

---

## 8. Tags

| # | Action | Attendu | Item |
|---|---|---|---|
| 8.1 | Ouvrir **Mes tags** | ⚠️ Le bouton « Nouveau tag » est **violet de marque**, plus indigo | UX-29 |
| 8.2 | Sélectionner un tag personnel | La bordure de sélection est **violette**, plus indigo | UX-29 |
| 8.3 | Survoler un sous-tag | Le fond de survol est violet | UX-29 |
| 8.4 | Comparer avec le bouton principal d'une autre page | **Même violet** | UX-29 |

---

## 9. Console d'administration

| # | Action | Attendu | Item |
|---|---|---|---|
| 9.1 | Ouvrir **Administration**, onglet Utilisateurs | ⚠️ **Plus de bouton « … » en fin de ligne** | UX-04 |
| 9.2 | Vérifier les colonnes restantes | Nom, e-mail, rôle et date de création toujours présents | UX-04 |
| 9.3 | Passer en mobile | Plus de bouton « … » sur les cartes non plus | UX-04 |
| 9.4 | Comparer le titre de page avec celui des Livrets et des Tags | ⚠️ **Même taille** — ils étaient à 2,5 / 2,25 / 2 rem | UX-30 |

---

## 10. Régressions à surveiller

Ces points n'ont pas changé, mais les modifications les traversent.

| # | Vérification | Lié à |
|---|---|---|
| 10.1 | Créer une transaction depuis le détail d'un livret | UX-07 (libellé « Sélectionner le type ») |
| 10.2 | Créer une transaction régulière | UX-07 |
| 10.3 | Importer un CSV | UX-30 (cadre de page) |
| 10.4 | Trier et paginer le tableau des transactions | UX-10 (`outline` retiré) |
| 10.5 | Basculer clair ↔ sombre sur chaque page visitée | UX-11 (tokens) |
| 10.6 | Vérifier qu'aucune page ne montre de couleur incohérente en sombre | UX-11 / UX-12 |

---

## Ce que la suite automatisée couvre déjà

Inutile de le revérifier à la main : contrastes des tokens (≥ 4.5:1 dans les deux thèmes),
absence de classes de palette brutes sur les 4 écrans monétaires, absence d'indigo hors palette sur
les Tags, existence des shortcuts et absence de cadre bespoke, orthographe des libellés, calcul de
la moyenne journalière sur les 3 périodes, comportement molette / `Ctrl`+molette, garde-fou de
navigation des Paramètres, cible de la page 404 selon l'authentification.
