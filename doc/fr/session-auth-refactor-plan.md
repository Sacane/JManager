# Plan de refactorisation — Gestion de session/authentification (Hexagonal + multi-client)

> Rédigé le 11 avril 2026  
> Périmètre : backend (domain + infra) et frontend web  
> Objectif : préparer un support propre Web + Mobile sans casser l'architecture hexagonale

---

## 1. Contexte

Le projet suit une architecture hexagonale. La logique métier est globalement bien placée dans le domaine
(roles, authorization métier, session manager via port SPI), mais certains mécanismes d'authentification restent
fortement orientés navigateur (cookie HttpOnly uniquement, stockage session web implicite, guards UI dispersés).

Cette situation fonctionne pour le client web actuel, mais devient limitante si un client mobile natif doit consommer
la meme API avec des contraintes de transport et de stockage différentes.

---

## 2. Problem statement

### 2.1 Ce qui est sain et doit être conservé

- Les regles de droits (USER/ADMIN) appartiennent au domaine.
- Le domaine consomme un contrat (`SessionManager`) et non des détails techniques HTTP.
- Le controle d'accès existe déjà a deux niveaux (infra HTTP + domaine) : c'est une bonne défense en profondeur.

### 2.2 Ce qui doit évoluer

- Le transport d'identité est couplé au cookie navigateur.
- Le refresh token est partiellement préparé mais non finalisé de bout en bout.
- Les comportements de session coté frontend sont dispersés (gestion des erreurs, guard admin, stockage local).

---

## 3. Cible d'architecture

### 3.1 Principe directeur

Séparer strictement :

- Domaine : regles d'authentification/autorisation, validité/expiration des tokens, contrats de ports.
- Infrastructure : extraction des credentials (cookie/header), sécurité HTTP, mapping framework.
- Clients (web/mobile) : stratégie de stockage local et stratégie d'injection des credentials.

### 3.2 Mode transitoire recommandé

Pendant la migration, supporter deux transports en parallele :

- Web : cookie HttpOnly (conservé pour sécurité navigateur).
- Mobile/API clients : `Authorization: Bearer <token>`.

Ce mode évite une migration "big bang" et réduit le risque de régression.

---

## 4. Plan de refacto par étapes

## Phase 1 — Stabiliser le contrat domaine (TDD)

### But

Figer les invariants métier de session avant de toucher l'infrastructure.

### Actions

1. Renforcer les tests domaine sur `SessionManager.authenticate` :
   - token valide,
   - token expiré,
   - role insuffisant,
   - session absente/invalide.
2. Clarifier les regles de durée de vie access/refresh token dans les tests et la documentation domaine.
3. Valider que le domaine reste ignorant de cookie/header/framework.

### Critère de sortie

- Tous les tests domaine auth/session sont verts.
- Aucun import infra/framework ajouté dans `domain`.

---

## Phase 2 — Refacto infra backend (transport auth)

### But

Découpler l'authentification du seul cookie pour supporter web et mobile.

### Actions

1. Introduire un extracteur de credentials HTTP (adapter infra) qui sait lire :
   - cookie `token`,
   - header `Authorization: Bearer ...`.
2. Faire évoluer le filtre d'authentification pour utiliser cet extracteur.
3. Ajouter/compléter l'endpoint de refresh token (rotation + invalidation).
4. Harmoniser les réponses d'erreur 401/403 pour faciliter le comportement client.

### Critère de sortie

- Les endpoints protégés fonctionnent en cookie et en bearer.
- Le refresh est testé et opérationnel.

---

## Phase 3 — Refacto frontend web (infrastructure client)

### But

Rendre la gestion de session web plus propre, centralisée et remplaçable.

### Actions

1. Introduire une abstraction de stockage session (ex: `WebSessionStore`) au lieu d'accès direct dispersé.
2. Centraliser la logique auth réseau dans un client HTTP/interceptor unique :
   - injection credentials,
   - gestion 401/403,
   - tentative refresh,
   - retry contrôlé,
   - logout final.
3. Déplacer les guards d'authorization (ex: admin) vers middleware route dédié.

### Critère de sortie

- Plus de logique auth critique directement dans les pages.
- Un seul point d'entrée pour la politique de session HTTP.

---

## Phase 4 — Validation croisée et migration contrôlée

### But

Sécuriser le rollout sans régression fonctionnelle ni sécurité.

### Actions

1. Ajouter/mettre a jour la matrice de tests :
   - login/logout/settings/admin en cookie,
   - login/logout/settings/admin en bearer,
   - token expiré,
   - refresh invalide,
   - rôle insuffisant.
2. Vérifier le comportement d'un client non navigateur (simulation mobile/API client).
3. Documenter officiellement la politique auth multi-client.

### Critère de sortie

- Scénarios critiques validés en CI.
- Stratégie de transport multi-client documentée et stable.

---

## 5. Règles d'architecture a respecter pendant la refacto

1. Le domaine ne dépend jamais de Spring, HTTP, cookies, sessionStorage, SecurityContext.
2. Les rôles/droits restent des concepts domaine.
3. L'infrastructure traduit uniquement les préoccupations techniques vers les ports domaine.
4. Les clients (web/mobile) partagent le meme contrat API, mais pas forcément le meme mécanisme de stockage local.
5. Défense en profondeur maintenue :
   - contrôle infra (route-level),
   - contrôle domaine (use-case-level).

---

## 6. Ordre d'exécution conseillé

1. Domaine/tests (Phase 1)
2. Backend infra cookie + bearer + refresh (Phase 2)
3. Frontend web (Phase 3)
4. Validation complète + documentation + changelog final (Phase 4)

Cet ordre limite le risque et permet des livraisons incrémentales.

---

## 7. Risques principaux et mitigations

- Risque : régression auth web pendant introduction bearer.
  - Mitigation : mode dual-stack + tests API des deux flux.

- Risque : refresh token mal géré (rejeu, session zombie).
  - Mitigation : rotation stricte + invalidation serveur + tests dédiés.

- Risque : duplication de logique auth dans le frontend.
  - Mitigation : interceptor unique + middleware centralisé.

---

## 8. Définition de "Done"

La refactorisation est considérée terminée quand :

1. Les règles auth/roles sont validées coté domaine.
2. L'API supporte cookie et bearer proprement.
3. Le refresh fonctionne de bout en bout.
4. Le frontend web n'a plus de logique auth critique dispersée.
5. Les tests couvrent les parcours critiques web + mobile-like client.
6. La documentation technique est a jour.
