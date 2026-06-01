# Backlog — Corriger le mapping HTTP de USER_UNAUTHORIZED

> **Détecté le** : 2026-06-01
> **Contexte** : couche application, feature `feat/email-based-login`
> **Priorité** : low

## Problème

`ResultState.USER_UNAUTHORIZED` (mauvais mot de passe à la connexion) est actuellement mappé sur
`ForbiddenException` dans `ApiMappingExtensions.toHttpResponse()`, ce qui produit **HTTP 403**.

La sémantique HTTP correcte est **401 Unauthorized** : l'utilisateur n'a pas prouvé son identité
(credentials invalides), ce qui est distinct du 403 qui signifie "identité connue mais accès refusé".

## Localisation

`application/src/main/kotlin/fr/sacane/jmanager/application/api/ApiMappingExtensions.kt` ligne 94 :

```kotlin
ResultState.TAG_LABEL_ALREADY_TAKEN, ResultState.FORBIDDEN, ResultState.USER_UNAUTHORIZED -> throw ForbiddenException(...)
```

## Correction attendue

Déplacer `ResultState.USER_UNAUTHORIZED` dans la branche `UnauthorizedRequestException` (HTTP 401) :

```kotlin
ResultState.UNAUTHORIZED, ResultState.USER_NOT_AUTHENTICATED,
ResultState.PASSWORD_NOT_MATCH, ResultState.USER_UNAUTHORIZED -> throw UnauthorizedRequestException(...)
```

## Impact

- Le test `Login with wrong password returns 403` dans `SessionControllerTest` devra être mis à jour en `401`.
- Aucun impact domaine.
