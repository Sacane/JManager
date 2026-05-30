# Feature Flag HTTP Mapping — FEATURE_DISABLED → HTTP 404

> **Topic**: REST API — mapping d'un état domaine vers un statut HTTP
> **Date**: 2026-05-30
> **Author**: Technical Backend

---

## Context

Le domaine renvoie désormais `ResultState.FEATURE_DISABLED` lorsque la clé `USER_REGISTRATION`
est absente ou désactivée. La couche application doit mapper cet état vers un code HTTP cohérent
dans `toHttpResponse()` (`ApiMappingExtensions.kt`). Ce rapport justifie le choix HTTP et décrit
l'ensemble des adaptations nécessaires dans la couche application et ses tests.

## Current State

`toHttpResponse()` est un `when` exhaustif sur `ResultState`. Tout nouvel état ajouté à l'enum
provoque une erreur de compilation (`'when' expression must be exhaustive`), ce qui garantit
qu'aucun état ne sera silencieusement ignoré. C'est un filet de sécurité architectural à
conserver — ne jamais ajouter un `else` dans ce `when`.

`AuthenticatedUserTest.beforeEach()` appelle `registerUserUseCase.handle(...)` directement.
Le `FeatureFlagSeeder` insère tous les flags en état `disabled = false` au démarrage du contexte
Spring de test. Après l'ajout de la garde, l'appel dans `beforeEach()` échouera avec
`FEATURE_DISABLED` tant que `USER_REGISTRATION` n'est pas activé dans la base de test.

## Analysis

### Choix du code HTTP

| Option | Sémantique | Risque |
|---|---|---|
| **404 Not Found** | L'endpoint semble inexistant quand la feature est off | Légère ambiguïté (ressource vs feature) |
| 403 Forbidden | La feature existe, l'accès est refusé | Révèle l'existence de l'endpoint — moins sûr pour une feature publique désactivée |
| 503 Service Unavailable | Indisponibilité temporaire | Incorrect : la désactivation est intentionnelle, pas une panne |

**Décision : HTTP 404.** L'endpoint `/api/user/create` est `permitAll` (public, non authentifié).
Retourner 404 quand la feature est désactivée ne révèle pas l'existence de la route à un
attaquant qui scanne les endpoints. C'est la posture la plus défensive pour un endpoint
d'inscription public.

### Impact sur les tests d'intégration

`AuthenticatedUserTest` est la base de tous les tests Spring Boot qui nécessitent un utilisateur
authentifié. Son `@BeforeEach` appelle `registerUserUseCase`. Avec la garde en place, ce setup
échoue si `USER_REGISTRATION` est désactivé. La solution : injecter `FeatureFlagJpaRepository`
dans `AuthenticatedUserTest` et activer le flag avant l'inscription.

L'ordre d'exécution de `FeatureFlagControllerTest` (qui étend `AuthenticatedUserTest`) sera :
1. `AuthenticatedUserTest.beforeEach()` → active `USER_REGISTRATION`, inscrit l'utilisateur test
2. `FeatureFlagControllerTest.setupFlags()` → réinitialise tous les flags à `disabled`
3. Tests feature flags — flag `USER_REGISTRATION` est alors désactivé à nouveau

Ce séquençage est correct : l'utilisateur test est inscrit avant la réinitialisation des flags.

## Recommended Approach

### 1. `ApiMappingExtensions.kt` — ajouter la branche `FEATURE_DISABLED`

```kotlin
ResultState.FEATURE_DISABLED -> throw NotFoundException(
    this.errorInfo?.code ?: this.status.code,
    this.errorInfo?.detail ?: this.message,
    this.errorInfo?.key,
)
```

Grouper avec les autres `NOT_FOUND` existants : l'exception `NotFoundException` est déjà gérée
dans `ProblemDetailHandler` et renvoie `HttpStatus.NOT_FOUND` (HTTP 404).

### 2. `AuthenticatedUserTest.kt` — seeder le flag avant l'inscription

```kotlin
@Autowired
private lateinit var featureFlagJpaRepository: FeatureFlagJpaRepository

@BeforeEach
fun enableRegistrationFlag() {
    featureFlagJpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = true))
}
```

### 3. Tests — deux niveaux

**Unitaire** (`ApiMappingExtensionsTest`) — exhaustivité garantie sans démarrer Spring :
```kotlin
@Test
fun `Result toHttpResponse should throw NotFoundException for FEATURE_DISABLED status`() {
    val result = failure<String>(ResultState.FEATURE_DISABLED, "Feature disabled")
    assertThrows<NotFoundException> { result.toHttpResponse() }
}
```

**Intégration** (`SessionControllerTest`) — deux scénarios HTTP :
- `POST /api/user/create` retourne 201 quand `USER_REGISTRATION` est activé
- `POST /api/user/create` retourne 404 quand `USER_REGISTRATION` est désactivé

### Why this approach

Le `when` exhaustif existant garantit la couverture à la compilation. Aucun `else` n'est
introduit — on reste dans le modèle "tout état domaine doit avoir une réponse HTTP explicite."
Le choix de `NotFoundException` (→ 404) plutôt qu'une nouvelle exception dédiée maintient la
cohérence : un seul handler dans `ProblemDetailHandler` pour tous les cas 404, sans multiplier
les branches.

## Implementation Notes

Fichiers à modifier, dans l'ordre :

1. `application/src/main/kotlin/.../api/ApiMappingExtensions.kt`
   — ajouter `ResultState.FEATURE_DISABLED` dans le bloc `NOT_FOUND` du `when`

2. `application/src/test/kotlin/.../api/AuthenticatedUserTest.kt`
   — injecter `FeatureFlagJpaRepository`, activer `USER_REGISTRATION` en `@BeforeEach`

3. `application/src/test/kotlin/.../api/ApiMappingExtensionsTest.kt`
   — ajouter le test unitaire `FEATURE_DISABLED → NotFoundException`

4. `application/src/test/kotlin/.../api/SessionControllerTest.kt`
   — ajouter les tests d'intégration endpoint avec flag on/off

Aucune dépendance Gradle à ajouter. `FeatureFlagJpaRepository` est déjà importé dans les tests
existants (`FeatureFlagControllerTest`).

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| 404 ambigu (ressource vs feature) | Faible | Le `errorKey` dans le `ProblemDetail` (`domain.user.register.feature_disabled`) permet au client de distinguer les cas |
| `AuthenticatedUserTest` dépend du flag | Moyen | Le `@BeforeEach` d'activation est explicite et documenté. Si un autre use case est un jour gatté, la même pattern s'applique |
| `FeatureFlagControllerTest.setupFlags()` désactive USER_REGISTRATION | Faible | L'inscription se fait avant `setupFlags()` — aucun impact sur les tests feature flag existants |

## References
- [RFC 9110 §15.5.5 — 404 Not Found](https://httpwg.org/specs/rfc9110.html#status.404)
- [OWASP API Security Top 10 — API3:2023 Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/)
