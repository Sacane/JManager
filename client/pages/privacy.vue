<script setup lang="ts">
definePageMeta({ layout: 'legal' })
</script>

<template>
  <article class="flex flex-col gap-10">

    <!-- En-tête -->
    <header class="flex flex-col gap-3 pb-6 border-b border-[var(--card-border)]">
      <div class="flex items-center gap-2">
        <i class="pi pi-shield text-[var(--primary)] text-xl" aria-hidden="true" />
        <span class="text-caption">Document légal</span>
      </div>
      <h1 class="heading-1">
        Politique de Confidentialité
      </h1>
      <p class="body-sm">
        Version 1.1 &mdash; En vigueur à compter du 4 juin 2026
      </p>
    </header>

    <!-- 1. Responsable -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        1. Identité du responsable de traitement
      </h2>
      <div class="card p-5 flex flex-col gap-1 body-base">
        <p><strong class="text-label">Responsable</strong> : RAMAROSON RAKOTOMIHAMINA Johan</p>
        <p><strong class="text-label">Adresse</strong> : 1 avenue Haroun Tazieff, 77600 Bussy-Saint-Georges</p>
        <p>
          <strong class="text-label">Contact</strong> :
          <a
            href="mailto:contact@jmanager.sacane.fr"
            class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline"
          >
            contact@jmanager.sacane.fr
          </a>
        </p>
      </div>
    </section>

    <!-- 2. Données collectées -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        2. Données personnelles collectées
      </h2>

      <h3 class="heading-4">
        2.1 Données d'identification et d'authentification
      </h3>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Donnée</th>
              <th class="legal-th">Finalité</th>
              <th class="legal-th">Obligatoire</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td">Adresse e-mail</td>
              <td class="legal-td">Identification du compte, notifications transactionnelles</td>
              <td class="legal-td">Oui</td>
            </tr>
            <tr>
              <td class="legal-td">Nom d'utilisateur</td>
              <td class="legal-td">Authentification, affichage dans l'interface</td>
              <td class="legal-td">Oui</td>
            </tr>
            <tr>
              <td class="legal-td">Mot de passe</td>
              <td class="legal-td">Authentification — stocké sous forme de condensat BCrypt, jamais en clair</td>
              <td class="legal-td">Oui</td>
            </tr>
            <tr>
              <td class="legal-td">Date de création du compte</td>
              <td class="legal-td">Gestion administrative</td>
              <td class="legal-td">Automatique</td>
            </tr>
          </tbody>
        </table>
      </div>

      <h3 class="heading-4">
        2.2 Données financières personnelles
      </h3>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Donnée</th>
              <th class="legal-th">Finalité</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td">Libellés de livrets et soldes</td>
              <td class="legal-td">Suivi budgétaire</td>
            </tr>
            <tr>
              <td class="legal-td">Libellés de transactions, montants, dates</td>
              <td class="legal-td">Analyse des dépenses et revenus</td>
            </tr>
            <tr>
              <td class="legal-td">Transactions récurrentes (labels, montants, fréquences)</td>
              <td class="legal-td">Projection financière</td>
            </tr>
            <tr>
              <td class="legal-td">Étiquettes (tags) associées aux transactions</td>
              <td class="legal-td">Catégorisation des dépenses</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="card p-4 flex gap-3 items-start">
        <i class="pi pi-info-circle text-[var(--primary)] mt-0.5 shrink-0" aria-hidden="true" />
        <p class="body-sm">
          Les libellés de transactions sont en texte libre. Nous vous recommandons de ne pas y mentionner de données sensibles
          (état de santé, situation judiciaire, etc.). Si de telles données y figurent, elles bénéficient des mêmes protections
          que le reste de vos données.
        </p>
      </div>

      <h3 class="heading-4">
        2.3 Données de consentement (Art. 7 RGPD)
      </h3>
      <p class="body-base">
        Nous enregistrons la date et l'heure d'acceptation des présentes Conditions d'Utilisation, la version des conditions
        acceptées, ainsi que la date et l'heure d'acceptation de la présente Politique de confidentialité.
      </p>

      <h3 class="heading-4">
        2.4 Données techniques
      </h3>
      <p class="body-base">
        Tokens d'authentification (JWT, stockés en cookie HttpOnly), plan d'abonnement, paramètres d'affichage
        (fenêtre de projection, cycles mensuels). Ces données ne permettent pas d'identifier votre activité en dehors
        de l'application.
      </p>
    </section>

    <!-- 3. Bases légales -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        3. Bases légales du traitement (Art. 6 RGPD)
      </h2>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Traitement</th>
              <th class="legal-th">Base légale</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td">Fourniture du service de gestion financière</td>
              <td class="legal-td">Exécution du contrat (Art. 6.1.b)</td>
            </tr>
            <tr>
              <td class="legal-td">Enregistrement du consentement RGPD</td>
              <td class="legal-td">Consentement explicite (Art. 6.1.a)</td>
            </tr>
            <tr>
              <td class="legal-td">Sécurité : limitation des tentatives de connexion</td>
              <td class="legal-td">Intérêt légitime (Art. 6.1.f)</td>
            </tr>
            <tr>
              <td class="legal-td">Envoi d'e-mails transactionnels (bienvenue)</td>
              <td class="legal-td">Exécution du contrat (Art. 6.1.b)</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 4. Destinataires -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        4. Destinataires des données
      </h2>
      <p class="body-base">
        Vos données ne sont <strong>ni vendues, ni cédées, ni partagées</strong> avec des tiers à des fins commerciales.
        Elles peuvent être accessibles à :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>L'équipe technique de JManager dans le cadre de la maintenance et du support.</li>
        <li>
          Le prestataire d'hébergement <strong>PulseHeberg</strong>, lié par des clauses contractuelles
          conformes au RGPD.
        </li>
        <li>
          Le prestataire d'envoi d'e-mails <strong>Resend</strong> (Resend Inc., États-Unis), qui reçoit
          uniquement les adresses e-mail nécessaires à l'acheminement des messages transactionnels
          (ex. : e-mail de bienvenue). Un accord de traitement des données (DPA) incluant les
          Clauses Contractuelles Types (CCT) de la Commission européenne est en place.
        </li>
      </ul>
    </section>

    <!-- 5. Transferts -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        5. Transferts hors Union européenne
      </h2>
      <p class="body-base">
        Les données sont hébergées sur des serveurs situés dans l'Union européenne (hébergeur : PulseHeberg).
      </p>
      <p class="body-base">
        L'envoi des e-mails transactionnels implique la transmission de l'adresse e-mail du destinataire
        à <strong>Resend Inc.</strong> (États-Unis). Ce transfert est encadré par les
        <strong>Clauses Contractuelles Types (CCT)</strong> approuvées par la Commission européenne
        (Art. 46 RGPD), incluses dans le DPA signé avec Resend. Les données traitées par Resend
        sont hébergées sur des serveurs AWS situés dans l'Union européenne (région eu-west-1, Irlande).
      </p>
    </section>

    <!-- 6. Conservation -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        6. Durée de conservation
      </h2>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Données</th>
              <th class="legal-th">Durée</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td">Comptes inactifs (jamais activés)</td>
              <td class="legal-td">30 jours</td>
            </tr>
            <tr>
              <td class="legal-td">Données de compte et données financières après suppression</td>
              <td class="legal-td">1 an (obligations légales)</td>
            </tr>
            <tr>
              <td class="legal-td">Logs d'audit contenant des données personnelles</td>
              <td class="legal-td">90 jours glissants</td>
            </tr>
            <tr>
              <td class="legal-td">Tokens d'authentification et sessions</td>
              <td class="legal-td">À expiration du token (24 h pour l'accès, 7 j pour le rafraîchissement)</td>
            </tr>
            <tr>
              <td class="legal-td">Données de consentement</td>
              <td class="legal-td">Durée de la relation contractuelle + 5 ans</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="body-sm">
        À l'expiration de ces délais, les données sont supprimées de manière irréversible.
        Vous pouvez demander la suppression immédiate de votre compte à tout moment (cf. section 7).
      </p>
    </section>

    <!-- 7. Droits -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        7. Vos droits (Art. 15 à 22 RGPD)
      </h2>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Droit</th>
              <th class="legal-th">Comment l'exercer</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td font-medium">Accès (Art. 15)</td>
              <td class="legal-td">Contacter contact@jmanager.sacane.fr</td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Rectification (Art. 16)</td>
              <td class="legal-td">Contacter contact@jmanager.sacane.fr</td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Effacement (Art. 17 — droit à l'oubli)</td>
              <td class="legal-td">Directement depuis l'application : <em>Paramètres → Supprimer mon compte</em></td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Portabilité (Art. 20)</td>
              <td class="legal-td">En cours de développement — disponible prochainement</td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Opposition (Art. 21)</td>
              <td class="legal-td">Contacter contact@jmanager.sacane.fr</td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Retrait du consentement (Art. 7.3)</td>
              <td class="legal-td">La suppression du compte vaut retrait. Sinon, contacter contact@jmanager.sacane.fr</td>
            </tr>
            <tr>
              <td class="legal-td font-medium">Réclamation auprès de la CNIL</td>
              <td class="legal-td">
                <a
                  href="https://www.cnil.fr"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline"
                >
                  www.cnil.fr
                </a>
                — 3 place de Fontenoy, 75007 Paris
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="body-sm">
        Les demandes sont traitées dans un délai d'<strong>un mois</strong> (Art. 12 RGPD),
        prorogeable de deux mois pour les demandes complexes.
      </p>
    </section>

    <!-- 8. Sécurité -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        8. Sécurité des données
      </h2>
      <p class="body-base">
        Nous mettons en œuvre les mesures techniques et organisationnelles suivantes :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li><strong>Mots de passe</strong> : hachés via BCrypt (coût 12) avec dérivation SHA-512 — jamais stockés en clair.</li>
        <li><strong>Transport</strong> : chiffrement TLS/HTTPS obligatoire en production.</li>
        <li><strong>Authentification</strong> : cookies JWT <code>HttpOnly</code>, <code>Secure</code>, <code>SameSite=Strict</code> — inaccessibles depuis JavaScript.</li>
        <li><strong>Protection contre les attaques</strong> : limitation du nombre de tentatives de connexion par adresse IP.</li>
        <li><strong>Accès à la base de données</strong> : authentification requise, connexions chiffrées.</li>
      </ul>
      <p class="body-base">
        En cas de violation de données susceptible d'engendrer un risque élevé pour vos droits et libertés,
        vous en serez informé dans les délais prévus par l'Art. 34 du RGPD.
      </p>
    </section>

    <!-- 9. Cookies -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        9. Cookies
      </h2>
      <p class="body-base">
        JManager utilise uniquement des cookies <strong>strictement nécessaires</strong> au fonctionnement du service.
        Aucun cookie publicitaire, analytique ou de suivi tiers n'est déposé.
      </p>
      <div class="overflow-x-auto">
        <table class="w-full text-sm border-collapse">
          <thead>
            <tr>
              <th class="legal-th">Cookie</th>
              <th class="legal-th">Type</th>
              <th class="legal-th">Durée</th>
              <th class="legal-th">Finalité</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="legal-td"><code>token</code></td>
              <td class="legal-td">HttpOnly, Secure, SameSite=Strict</td>
              <td class="legal-td">24 heures</td>
              <td class="legal-td">Authentification (JWT d'accès)</td>
            </tr>
            <tr>
              <td class="legal-td"><code>refresh_token</code></td>
              <td class="legal-td">HttpOnly, Secure, SameSite=Strict</td>
              <td class="legal-td">7 jours</td>
              <td class="legal-td">Renouvellement de session</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 10. Modifications -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        10. Modifications de la politique
      </h2>
      <p class="body-base">
        Toute modification substantielle sera notifiée par e-mail et nécessitera un nouvel accord explicite
        de votre part lors de votre prochaine connexion. La version en vigueur est toujours accessible
        depuis l'application et à l'adresse <code>/privacy</code>.
      </p>
    </section>

    <!-- 11. Contact -->
    <section class="flex flex-col gap-3 pb-8 border-t border-[var(--card-border)] pt-6">
      <h2 class="heading-3">
        11. Contact
      </h2>
      <p class="body-base">
        Pour toute question relative à cette politique ou à l'exercice de vos droits :
        <a
          href="mailto:contact@jmanager.sacane.fr"
          class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline font-medium"
        >
          contact@jmanager.sacane.fr
        </a>
      </p>
    </section>

  </article>
</template>

<style scoped>
.legal-th {
  @apply text-left p-3 font-medium bg-[var(--bg-secondary)] text-[var(--text-primary)] border border-[var(--card-border)];
}
.legal-td {
  @apply p-3 border border-[var(--card-border)] text-[var(--text-secondary)] align-top;
}
code {
  @apply font-mono text-xs bg-[var(--bg-secondary)] px-1.5 py-0.5 rounded text-[var(--text-primary)];
}
</style>
