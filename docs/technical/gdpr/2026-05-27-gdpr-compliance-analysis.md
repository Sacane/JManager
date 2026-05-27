# Analyse de conformité RGPD — JManager

> **Topic**: Sécurité des données personnelles & conformité RGPD
> **Date**: 2026-05-27
> **Author**: Technical Backend

---

## Context

JManager est une application de gestion de finances personnelles collectant des données très sensibles : adresses e-mail, données financières détaillées (libellés de transactions, montants, soldes de comptes). Une application manipulant ce type de données est directement visée par le RGPD (Règlement (UE) 2016/679). Ce rapport analyse l'état actuel du schéma de base de données et du code applicatif pour identifier les lacunes de conformité et proposer un plan de remédiation priorisé.

---

## Current State

### Données personnelles stockées

| Table | Colonne | Type de donnée | Sensibilité |
|---|---|---|---|
| `user_resource` | `email` | Identifiant direct | **Haute** |
| `user_resource` | `username` | Pseudonyme ou vrai nom | Moyenne |
| `user_resource` | `password` | Mot de passe (haché BCrypt) | ✅ Correct |
| `user_resource` | `creation_date` | Métadonnée | Basse |
| `user_resource` | `subscription_plan` | Plan commercial | Basse |
| `transactions` | `label_sheet` | Libellé libre (ex: "Dr Martin", "EHPAD Maman") | **Très haute** |
| `transactions` | `expenses`, `account_amount` | Données financières | **Haute** |
| `booklet` | `amount`, `initial_sold` | Soldes financiers | **Haute** |
| `monthly_regular_transaction` | `label`, `amount` | Transactions régulières | **Haute** |

### Ce qui est déjà correct

- ✅ Les mots de passe sont **hachés** via le port `Hasher` (BCrypt Spring Security)
- ✅ Les secrets applicatifs (`JWT_SECRET`, `DB_PASSWORD`) sont en variables d'environnement
- ✅ Les cookies JWT sont `HttpOnly`, `SameSite=Strict`, `Secure` (prod)
- ✅ Rate limiting sur le endpoint `/api/user/auth`
- ✅ Pas de `LocalDate.now()` ou `UUID.randomUUID()` en domaine (Clock/IdGenerator injectés)

---

## Analysis

### 🔴 P1 — BLOQUANT : Absence de droit à l'effacement (Art. 17 RGPD)

Le RGPD Art. 17 impose un **droit à l'effacement** ("droit à l'oubli"). Un utilisateur doit pouvoir demander la suppression de l'intégralité de ses données.

Aucun endpoint de suppression de compte n'existe dans le code. Il n'existe pas non plus de mécanisme d'**anonymisation** (alternative légalement acceptable : remplacer les identifiants par des valeurs génériques tout en conservant des statistiques agrégées).

Sans ce droit, **l'application est en infraction caractérisée** si elle collecte des données d'utilisateurs résidant dans l'UE.

---

### 🔴 P2 — BLOQUANT : Adresse e-mail dans les logs applicatifs

Dans `SpringMailNotificationAdapter.kt` :

```kotlin
LOGGER.severe("Failed to send welcome email to $email: ${e.message}")
```

L'adresse e-mail de l'utilisateur est écrite **en clair dans les logs**. Les logs applicatifs (stdout, fichiers, systèmes de log centralisés type Loki/ELK) ne sont pas des stockages de données personnelles sécurisés. Cela constitue une **fuite de données personnelles** par défaut.

---

### 🟠 P3 — Majeur : E-mail exposé systématiquement dans les réponses API

L'e-mail est retourné à chaque connexion dans `UserStorageDTO` (endpoint `/api/user/auth`) et dans `UserDTO` (endpoint admin `/api/admin/users`). Le frontend ne devrait recevoir l'e-mail que lors d'une consultation explicite des paramètres du compte, pas à chaque appel d'authentification.

```kotlin
// session/Controller.kt — login response
UserStorageDTO(
    it.user.id.value.toString(),
    username = it.user.username,
    email = it.user.email,  // ← exposé à chaque login
    token = it.token,
    ...
)
```

Le principe de **minimisation des données** (Art. 5.1.c RGPD) exige de ne transmettre que ce qui est strictement nécessaire.

---

### 🟠 P4 — Majeur : Absence de traçabilité du consentement

Le schéma ne contient aucune colonne permettant de prouver que l'utilisateur a :
- Accepté les Conditions Générales d'Utilisation
- Donné son consentement explicite au traitement de ses données
- La date et la version des CGU acceptées

Sans cette traçabilité, il est impossible de démontrer la base légale du traitement (Art. 6 RGPD — consentement ou exécution du contrat).

---

### 🟠 P5 — Majeur : Absence de politique de rétention des données

Les données (transactions, comptes, utilisateurs) sont conservées **indéfiniment**. Le RGPD Art. 5.1.e impose une **limitation de la conservation** : les données ne peuvent être gardées que le temps nécessaire à la finalité. Sans politique de purge automatique des comptes inactifs ou des données anciennes, l'application accumule indéfiniment des profils financiers.

---

### 🟡 P6 — Modéré : Libellés de transactions — données potentiellement sensibles

Le champ `label_sheet` (renommé en `transactions`) est en texte libre. En pratique, des utilisateurs y saisissent :
- `"Dr. Martin consultation"` → révèle un état de santé (catégorie spéciale Art. 9)
- `"EHPAD Maman"` → révèle une situation familiale
- `"Avocat divorce"` → révèle une situation personnelle

Ces données sont stockées en clair, associées à l'UUID utilisateur, sans chiffrement au niveau colonne. Un accès non autorisé à la base constitue une exposition directe de données sensibles.

---

### 🟡 P7 — Modéré : Pas de chiffrement au niveau colonne pour l'e-mail

L'e-mail est un identifiant direct (Art. 4 RGPD). Bien que la sécurité du transport (TLS) et de la base (auth PostgreSQL) soient assurées, un chiffrement au niveau colonne (`pgcrypto` ou chiffrement applicatif) rendrait les données illisibles en cas de dump de base de données ou d'accès non autorisé au storage.

---

### 🟡 P8 — Modéré : Endpoint admin expose tous les e-mails

`GET /api/admin/users` retourne un `Page<UserDTO>` contenant les e-mails de tous les utilisateurs. Même pour un administrateur, le **principe du moindre privilège** recommande de ne pas exposer en masse les identifiants directs sans justification explicite.

---

## Recommended Approach

### Priorité 1 — Droit à l'effacement (P1)

Implémenter un endpoint `DELETE /api/user/me` avec une stratégie de **suppression en cascade** propre, puis une option d'**anonymisation** pour les données de comptabilité historique.

```kotlin
// domain — nouveau use case
data class DeleteAccountCommand(val userId: UserId) : Command<Unit>

@DomainService
class DeleteAccountService(
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository,
    private val transactionRepository: TransactionRepository,
) : DeleteAccountUseCase {
    override fun handle(command: DeleteAccountCommand): Result<Unit> {
        // 1. Supprimer les transactions
        // 2. Supprimer les livrets
        // 3. Supprimer les tags personnels
        // 4. Supprimer l'utilisateur
        // ou : anonymiser (username → "deleted_<uuid>", email → null)
        return userRepository.deleteById(command.userId)
    }
}
```

Migration SQL pour garantir les cascades :

```sql
-- V23__add_on_delete_cascade_user.sql
ALTER TABLE booklet DROP CONSTRAINT fk_booklet_owner;
ALTER TABLE booklet ADD CONSTRAINT fk_booklet_owner
    FOREIGN KEY (owner_id_user) REFERENCES user_resource(id_user) ON DELETE CASCADE;

ALTER TABLE tag_personal_resource DROP CONSTRAINT ...;
ALTER TABLE tag_personal_resource ADD CONSTRAINT fk_tag_owner
    FOREIGN KEY (owner_id_user) REFERENCES user_resource(id_user) ON DELETE CASCADE;

-- Idem pour regular_recurring_check_entity, user_role, etc.
```

### Priorité 2 — Supprimer les PII des logs (P2)

```kotlin
// Avant
LOGGER.severe("Failed to send welcome email to $email: ${e.message}")

// Après — ne jamais logger d'identifiant direct
LOGGER.severe("Failed to send welcome email: ${e.javaClass.simpleName}")
```

Règle générale : **aucun champ personnel** (`email`, `username`, `password`) ne doit apparaître dans un message de log. Auditer tous les `LOGGER.severe/warning/info` du projet.

### Priorité 3 — Minimisation de l'e-mail dans les réponses API (P3)

Retirer l'e-mail de `UserStorageDTO` (réponse de login). Le frontend peut récupérer l'e-mail uniquement depuis `GET /api/user/settings` si nécessaire.

```kotlin
// Avant
data class UserStorageDTO(
    val id: String,
    val username: String,
    val email: String?,   // ← à retirer du token de session
    val token: String,
    val refreshToken: String?,
)

// Après
data class UserStorageDTO(
    val id: String,
    val username: String,
    // email supprimé — récupéré via GET /api/user/settings uniquement
    val token: String,
    val refreshToken: String?,
)
```

### Priorité 4 — Traçabilité du consentement (P4)

```sql
-- V24__add_consent_tracking.sql
ALTER TABLE user_resource
    ADD COLUMN tos_accepted_at    TIMESTAMP,
    ADD COLUMN tos_version        VARCHAR(20),
    ADD COLUMN privacy_accepted_at TIMESTAMP;
```

Collecter ces informations lors de la création du compte (`RegisterUserCommand` / frontend).

### Priorité 5 — Politique de rétention (P5)

```sql
-- V25__add_account_deletion_request.sql
CREATE TABLE account_deletion_request (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_user     UUID NOT NULL REFERENCES user_resource(id_user) ON DELETE CASCADE,
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    scheduled_for TIMESTAMP NOT NULL,  -- NOW() + 30 jours
    reason      VARCHAR(255)
);
```

Ajouter un scheduler Spring qui exécute les suppressions planifiées (délai de grâce de 30 jours, conforme à la pratique courante) :

```kotlin
@Scheduled(cron = "0 0 2 * * *") // 2h du matin tous les jours
fun processScheduledDeletions() {
    deletionRequestRepository.findDue(LocalDateTime.now()).forEach { req ->
        deleteAccountService.handle(DeleteAccountCommand(UserId(req.userId)))
    }
}
```

### Priorité 6 — Chiffrement colonne de l'e-mail (P7)

Option A — **Chiffrement applicatif** (recommandé, hexagonalement propre) :

```kotlin
// infrastructure — port
interface EmailEncryptor {
    fun encrypt(email: String): String
    fun decrypt(ciphertext: String): String
}

// infrastructure — adapter AES-256-GCM via JDK
@Component
class AesEmailEncryptor(
    @Value("\${email.encryption.key}") private val keyBase64: String
) : EmailEncryptor { ... }
```

L'e-mail chiffré est stocké en base ; le déchiffrement se fait côté applicatif uniquement quand nécessaire.

Option B — **pgcrypto** (simpler à court terme, couplage infrastructure) :

```sql
-- Activer l'extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Stocker chiffré
UPDATE user_resource
SET email = pgp_sym_encrypt(email, current_setting('app.email_key'));
```

→ **Option A privilégiée** car elle respecte l'architecture hexagonale et ne crée pas de dépendance à une fonction SQL propriétaire.

---

## Implementation Notes

### Ordre d'implémentation recommandé

1. **Sprint immédiat** (bloquants légaux) :
   - [x] Supprimer `$email` des logs (`SpringMailNotificationAdapter`)
   - [ ] Implémenter `DELETE /api/user/me` avec cascade
   - [ ] Retirer `email` de `UserStorageDTO`

2. **Sprint suivant** (conformité prouvable) :
   - [ ] Migration V24 — consentement + V23 — cascades FK
   - [ ] Endpoint de téléchargement des données (`GET /api/user/data-export`) — Art. 20 RGPD (portabilité)

3. **Sprint ultérieur** (hardening) :
   - [ ] Migration V25 — suppression planifiée + scheduler
   - [ ] Chiffrement applicatif de l'e-mail (port `EmailEncryptor`)
   - [ ] Audit de tous les logs pour éliminer les PII

### Modules concernés

| Priorité | Module | Fichier(s) |
|---|---|---|
| P1 | `domain` | Nouveau `DeleteAccountUseCase` |
| P1 | `infrastructure` | Cascades FK (migration SQL) |
| P1 | `application` | `DELETE /api/user/me` controller |
| P2 | `infrastructure` | `SpringMailNotificationAdapter.kt` |
| P3 | `application` | `session/Controller.kt`, `session/DTO.kt` |
| P4 | `infrastructure` | Migration SQL V24 |
| P5 | `infrastructure` | Migration SQL V25 + scheduler |
| P7 | `infrastructure` | Nouveau port + adapter `EmailEncryptor` |

---

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Cascade DELETE peut supprimer des données de façon irréversible | Haut | Implémenter un délai de grâce de 30 jours + soft delete avant purge définitive |
| Chiffrement de l'e-mail casse les requêtes `WHERE email = ?` et les index UNIQUE | Moyen | Stocker aussi un hash déterministe (HMAC-SHA256) pour les lookups d'unicité, déchiffrer uniquement pour l'affichage |
| Retirer `email` de `UserStorageDTO` est un breaking change frontend | Faible | Coordonner avec la migration frontend ; ajouter un champ `email` dans `GET /api/user/settings` si pas déjà présent |
| Audit des logs PII est manuel | Moyen | Ajouter une règle ArchUnit ou un grep CI pour détecter les patterns `LOGGER.*email` ou `LOGGER.*password` |
| Migration des FK en cascade sur table avec données existantes | Moyen | Tester sur dump de prod avec `BEGIN; ... ROLLBACK;` avant d'appliquer |

---

## Résumé de Priorités

```
🔴 BLOQUANT  — P1 : Droit à l'effacement (endpoint DELETE + cascades)
🔴 BLOQUANT  — P2 : E-mail dans les logs → supprimer immédiatement
🟠 MAJEUR    — P3 : E-mail dans UserStorageDTO → minimiser
🟠 MAJEUR    — P4 : Absence de traçabilité du consentement
🟠 MAJEUR    — P5 : Absence de politique de rétention
🟡 MODÉRÉ    — P6 : Libellés de transactions (données sensibles non chiffrées)
🟡 MODÉRÉ    — P7 : Chiffrement colonne e-mail
🟡 MODÉRÉ    — P8 : Endpoint admin expose les e-mails en masse
```

---

## References

- [RGPD — Règlement (UE) 2016/679, texte complet](https://eur-lex.europa.eu/legal-content/FR/TXT/?uri=CELEX%3A32016R0679)
- [Art. 5 — Principes relatifs au traitement](https://www.cnil.fr/fr/reglement-europeen-protection-donnees/chapitre2#Article5)
- [Art. 17 — Droit à l'effacement](https://www.cnil.fr/fr/reglement-europeen-protection-donnees/chapitre3#Article17)
- [Art. 20 — Droit à la portabilité](https://www.cnil.fr/fr/reglement-europeen-protection-donnees/chapitre3#Article20)
- [CNIL — Guide sécurité des données personnelles](https://www.cnil.fr/fr/la-securite-des-donnees-personnelles)
- [OWASP — Logging Cheat Sheet (ne pas logger de PII)](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
