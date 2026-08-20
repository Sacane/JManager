<script setup lang="ts">
definePageMeta({ layout: 'legal' })
</script>

<template>
  <article class="flex flex-col gap-10">
    <!-- En-tête -->
    <header class="flex flex-col gap-3 pb-6 border-b border-[var(--card-border)]">
      <div class="flex items-center gap-2">
        <i class="pi pi-file-edit text-[var(--primary)] text-xl" aria-hidden="true" />
        <span class="text-caption">Document légal</span>
      </div>
      <h1 class="heading-1">
        Conditions Générales d'Utilisation
      </h1>
      <p class="body-sm">
        Version 1.0 &mdash; En vigueur à compter du 28 mai 2026
      </p>
    </header>

    <!-- 1. Objet -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        1. Objet
      </h2>
      <p class="body-base">
        Les présentes Conditions Générales d'Utilisation (CGU) régissent l'accès et l'utilisation de
        l'application <strong>JManager</strong>, service de gestion financière personnelle édité par
        RAMAROSON RAKOTOMIHAMINA Johan (ci-après « l'Éditeur »).
      </p>
      <p class="body-base">
        L'utilisation de l'application implique l'acceptation pleine et entière des présentes CGU.
        Toute utilisation partielle ou restriction doit faire l'objet d'une demande expresse.
      </p>
    </section>

    <!-- 2. Éditeur -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        2. Identité de l'Éditeur
      </h2>
      <div class="card p-5 flex flex-col gap-1 body-base">
        <p><strong class="text-label">Éditeur</strong> : RAMAROSON RAKOTOMIHAMINA Johan</p>
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
        <p><strong class="text-label">Hébergeur</strong> : Pulseheberge — serveurs situés dans l'Union européenne</p>
      </div>
    </section>

    <!-- 3. Accès au service -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        3. Accès au service
      </h2>
      <p class="body-base">
        JManager est une application à accès restreint. Les comptes utilisateurs sont créés exclusivement
        par l'administrateur de l'application. Il n'existe pas d'inscription publique en libre-service.
      </p>
      <p class="body-base">
        L'accès au service nécessite :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>Un compte activé fourni par l'administrateur.</li>
        <li>L'acceptation des présentes CGU et de la Politique de confidentialité lors de la première connexion.</li>
        <li>Une connexion Internet stable.</li>
      </ul>
      <p class="body-base">
        L'Éditeur se réserve le droit de suspendre ou de supprimer l'accès de tout utilisateur ne respectant
        pas les présentes CGU, sans préavis ni indemnité.
      </p>
    </section>

    <!-- 4. Description du service -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        4. Description du service
      </h2>
      <p class="body-base">
        JManager est une application de gestion financière personnelle permettant notamment :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>La création et le suivi de livrets d'épargne ou de comptes virtuels.</li>
        <li>L'enregistrement et la catégorisation de transactions financières.</li>
        <li>La planification de transactions récurrentes.</li>
        <li>La visualisation de projections financières sur des périodes configurables.</li>
      </ul>
      <div class="card p-4 flex gap-3 items-start">
        <i class="pi pi-info-circle text-[var(--primary)] mt-0.5 shrink-0" aria-hidden="true" />
        <p class="body-sm">
          JManager est un outil d'aide à la gestion personnelle. Il ne constitue pas un conseil financier,
          bancaire ou d'investissement au sens réglementaire. Les données saisies n'ont qu'une valeur indicative.
        </p>
      </div>
    </section>

    <!-- 5. Obligations de l'utilisateur -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        5. Obligations de l'utilisateur
      </h2>
      <p class="body-base">
        L'utilisateur s'engage à :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>
          Utiliser le service exclusivement à des fins personnelles et licites.
        </li>
        <li>
          Ne pas tenter de contourner les mécanismes de sécurité, d'authentification ou de contrôle d'accès.
        </li>
        <li>
          Ne pas partager ses identifiants de connexion avec des tiers.
        </li>
        <li>
          Signaler immédiatement à l'Éditeur toute utilisation non autorisée de son compte à l'adresse
          <a
            href="mailto:contact@jmanager.sacane.fr"
            class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline"
          >
            contact@jmanager.sacane.fr
          </a>.
        </li>
        <li>
          Ne pas introduire de code malveillant ou tenter d'altérer le fonctionnement de l'application.
        </li>
      </ul>
    </section>

    <!-- 6. Propriété intellectuelle -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        6. Propriété intellectuelle
      </h2>
      <p class="body-base">
        L'ensemble des éléments constituant l'application JManager (code source, design, architecture,
        textes, logotypes) est la propriété exclusive de l'Éditeur et est protégé par les lois françaises
        et européennes relatives à la propriété intellectuelle.
      </p>
      <p class="body-base">
        Toute reproduction, représentation, modification, publication ou adaptation de tout ou partie de
        l'application, quel qu'en soit le moyen ou le procédé, est interdite sans l'autorisation écrite
        préalable de l'Éditeur.
      </p>
      <p class="body-base">
        Les données saisies par l'utilisateur (libellés, montants, etc.) restent sa propriété. L'Éditeur
        n'acquiert aucun droit sur le contenu des données financières personnelles.
      </p>
    </section>

    <!-- 7. Responsabilité -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        7. Responsabilité et limitations
      </h2>

      <h3 class="heading-4">
        7.1 Responsabilité de l'Éditeur
      </h3>
      <p class="body-base">
        L'Éditeur s'engage à mettre en œuvre tous les moyens raisonnables pour assurer la disponibilité
        et la sécurité du service. Cependant, il ne saurait être tenu responsable :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>D'une interruption temporaire de service pour maintenance ou raison technique.</li>
        <li>D'une perte de données résultant d'un événement imprévisible ou de force majeure.</li>
        <li>D'une décision financière prise sur la base des informations affichées dans l'application.</li>
      </ul>

      <h3 class="heading-4">
        7.2 Responsabilité de l'utilisateur
      </h3>
      <p class="body-base">
        L'utilisateur est seul responsable des données qu'il saisit et de l'usage qu'il fait du service.
        Il garantit que l'utilisation de JManager est conforme aux lois et réglementations applicables.
      </p>
    </section>

    <!-- 8. Données personnelles et consentement -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        8. Données personnelles et consentement
      </h2>
      <p class="body-base">
        Le traitement de vos données personnelles est régi par la
        <a
          href="/privacy"
          target="_blank"
          rel="noopener noreferrer"
          class="text-[var(--primary)] dark:text-[var(--primary-lighter)] hover:underline font-medium"
        >
          Politique de confidentialité
        </a>
        de JManager, qui fait partie intégrante des présentes CGU.
      </p>
      <p class="body-base">
        Conformément à l'Art. 7 du RGPD, l'acceptation des présentes CGU vaut consentement explicite
        au traitement de vos données personnelles tel que décrit dans la Politique de confidentialité.
        Ce consentement est librement révocable à tout moment en supprimant votre compte ou en contactant
        l'Éditeur.
      </p>
    </section>

    <!-- 9. Disponibilité et maintenance -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        9. Disponibilité et maintenance
      </h2>
      <p class="body-base">
        L'Éditeur s'efforce d'assurer la disponibilité du service 24 h/24 et 7 j/7, sans toutefois
        s'y engager contractuellement. Des interruptions planifiées ou imprévues peuvent survenir,
        notamment pour :
      </p>
      <ul class="list body-base flex flex-col gap-1 pl-5">
        <li>Des opérations de maintenance corrective ou évolutive.</li>
        <li>Des mises à jour de sécurité urgentes.</li>
        <li>Des incidents d'hébergement indépendants de la volonté de l'Éditeur.</li>
      </ul>
      <p class="body-base">
        L'Éditeur informera les utilisateurs des maintenances planifiées dans les meilleurs délais.
      </p>
    </section>

    <!-- 10. Résiliation -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        10. Résiliation et suppression de compte
      </h2>
      <p class="body-base">
        L'utilisateur peut demander la suppression de son compte à tout moment via l'interface
        (<em>Paramètres → Supprimer mon compte</em>) ou en contactant l'Éditeur. La suppression entraîne
        la désactivation immédiate du compte et la purge des données personnelles selon les délais
        indiqués dans la Politique de confidentialité.
      </p>
      <p class="body-base">
        L'Éditeur peut résilier unilatéralement l'accès d'un utilisateur en cas de non-respect des
        présentes CGU, de comportement frauduleux ou d'inactivité prolongée (comptes jamais activés
        après 30 jours).
      </p>
    </section>

    <!-- 11. Modifications des CGU -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        11. Modifications des CGU
      </h2>
      <p class="body-base">
        L'Éditeur se réserve le droit de modifier les présentes CGU à tout moment. Toute modification
        substantielle sera notifiée par e-mail et nécessitera un nouvel accord explicite de votre part
        lors de votre prochaine connexion. La version en vigueur est toujours accessible depuis
        l'application et à l'adresse <code>/terms</code>.
      </p>
    </section>

    <!-- 12. Droit applicable -->
    <section class="flex flex-col gap-4">
      <h2 class="heading-3">
        12. Droit applicable et juridiction
      </h2>
      <p class="body-base">
        Les présentes CGU sont soumises au droit français. En cas de litige, les parties s'engagent
        à rechercher une solution amiable. À défaut, le litige sera soumis aux tribunaux compétents
        du ressort du domicile de l'Éditeur, sauf disposition légale contraire.
      </p>
    </section>

    <!-- 13. Contact -->
    <section class="flex flex-col gap-3 pb-8 border-t border-[var(--card-border)] pt-6">
      <h2 class="heading-3">
        13. Contact
      </h2>
      <p class="body-base">
        Pour toute question relative aux présentes CGU :
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
