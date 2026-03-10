# Deploy

Ce dépôt contient la pipeline de déploiement distante pour JManager.

## Déclenchement automatique depuis JManager

Le workflow `/.github/workflows/deploy-from-jmanager.yml` écoute l'événement `repository_dispatch` de type `jmanager_deploy`.

Payload attendu :

```json
{
  "event_type": "jmanager_deploy",
  "client_payload": {
    "jar_version": "1.2.3",
    "jar_download_url": "https://.../JManager-1.2.3.jar",
    "jar_file_name": "JManager-1.2.3.jar"
  }
}
```

`jar_file_name` est optionnel. S'il n'est pas fourni, le workflow tente d'utiliser le nom du fichier présent dans `jar_download_url`.

## Secrets / Variables GitHub à configurer (repo Deploy)

- `DEPLOY_HOST` : hôte du serveur distant
- `DEPLOY_USER` : utilisateur SSH
- `DEPLOY_REMOTE_DIR` : dossier de déploiement sur le serveur distant
- `DEPLOY_SSH_PRIVATE_KEY` : clé privée SSH utilisée par GitHub Actions
- `DEPLOY_SSH_KNOWN_HOSTS` : entrée(s) `known_hosts` du serveur distant
- `DEPLOY_REMOTE_PORT` (variable, optionnelle) : port SSH (défaut `22`)

## Exemple de trigger côté JManager

```yaml
- name: Trigger Deploy repository
  env:
    DEPLOY_REPO_PAT: ${{ secrets.DEPLOY_REPO_PAT }}
  run: |
    curl --fail --location \
      --request POST \
      --header "Accept: application/vnd.github+json" \
      --header "Authorization: Bearer ${DEPLOY_REPO_PAT}" \
      https://api.github.com/repos/Sacane/Deploy/dispatches \
      --data @- <<'JSON'
    {
      "event_type": "jmanager_deploy",
      "client_payload": {
        "jar_version": "${{ needs.build.outputs.version }}",
        "jar_download_url": "${{ needs.build.outputs.jar_download_url }}",
        "jar_file_name": "${{ needs.build.outputs.jar_file_name }}"
      }
    }
    JSON
```
