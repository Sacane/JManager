import process from 'node:process'
import runtimeConfig from './env.config.json'
import devRuntimeConfig from './env.dev.config.json'

const isDev = process.env.NODE_ENV === 'development'
// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  app: {
    head: {
      title: 'JManager',
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap' },
      ],
    },
  },
  primevue: {
    options: {
      ripple: true,
      tooltip: true,
      locale: {
        accept: 'Oui',
        addRule: 'Ajouter une règle',
        am: 'am',
        apply: 'Appliquer',
        cancel: 'Annuler',
        choose: 'Choisir',
        chooseDate: 'Choisir une date',
        chooseMonth: 'Choisir un mois',
        chooseYear: 'Choisir une année',
        clear: 'Effacer',
        completed: 'Terminé',
        contains: 'Contient',
        custom: 'Personnalisé',
        dateAfter: 'Après le',
        dateBefore: 'Avant le',
        dateFormat: 'dd/mm/yy',
        dateIs: 'La date est',
        dateIsNot: 'La date n\'est pas',
        dayNames: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
        dayNamesMin: ['Di', 'Lu', 'Mar', 'Mer', 'Je', 'Ve', 'Sa'],
        dayNamesShort: ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'],
        emptyFilterMessage: 'Aucun résultat trouvé',
        emptyMessage: 'Aucune option disponible',
        emptySearchMessage: 'Aucun résultat trouvé',
        emptySelectionMessage: 'Aucun élément sélectionné',
        endsWith: 'Se termine par',
        equals: 'Égal à',
        fileSizeTypes: ['o', 'Ko', 'Mo', 'Go', 'To', 'Po', 'Eo', 'Zo', 'Yo'],
        filter: 'Filtre',
        firstDayOfWeek: 1,
        gt: 'Supérieur à',
        gte: 'Supérieur ou égal à',
        lt: 'Inférieur à',
        lte: 'Inférieur ou égal à',
        matchAll: 'Correspond à tous',
        matchAny: 'Au moins un Correspond',
        medium: 'Moyen',
        monthNames: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
        monthNamesShort: ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Jun', 'Jui', 'Août', 'Sept', 'Oct', 'Nov', 'Dec'],
        nextDecade: 'Décennie suivante',
        nextHour: 'Heure suivante',
        nextMinute: 'Minute suivante',
        nextMonth: 'Mois suivant',
        nextSecond: 'Seconde suivante',
        nextYear: 'Année suivante',
        noFilter: 'Aucun filtre',
        notContains: 'Ne contient pas',
        notEquals: 'Différent de',
        now: 'Maintenant',
        passwordPrompt: 'Saisissez un mot de passe',
        pending: 'En attente',
        pm: 'pm',
        prevDecade: 'Décennie précédente',
        prevHour: 'Heure précédente',
        prevMinute: 'Minute précédente',
        prevMonth: 'Mois précédent',
        prevSecond: 'Seconde précédente',
        prevYear: 'Année précédente',
        reject: 'Non',
        removeRule: 'Retirer une règle',
        searchMessage: '{0} résultats disponibles',
        selectionMessage: '{0} éléments sélectionnés',
        showMonthAfterYear: false,
        startsWith: 'Commence par',
        strong: 'Fort',
        today: 'Aujourd\'hui',
        upload: 'Envoyer',
        weak: 'Faible',
        weekHeader: 'Sem',
        aria: {
          cancelEdit: 'Annule l\'édition',
          close: 'Fermer',
          collapseLabel: 'Effondrement',
          collapseRow: 'Ligne repliée',
          editRow: 'Édite une ligne',
          expandLabel: 'Développer',
          expandRow: 'Ligne dépliée',
          falseLabel: 'Faux',
          filterConstraint: 'Contrainte de filtrage',
          filterOperator: 'Opérateur de filtrage',
          firstPageLabel: 'Première Page',
          gridView: 'Vue en grille',
          hideFilterMenu: 'Masque le menu des filtres',
          jumpToPageDropdownLabel: 'Aller à la page',
          jumpToPageInputLabel: 'Aller à la page',
          lastPageLabel: 'Dernière Page',
          listView: 'Vue en liste',
          moveAllToSource: 'Tout déplacer vers la source',
          moveAllToTarget: 'Tout déplacer vers la cible',
          moveBottom: 'Déplacer tout en bas',
          moveDown: 'Déplacer vers le bas',
          moveTop: 'Déplacer tout en haut',
          moveToSource: 'Déplacer vers la source',
          moveToTarget: 'Déplacer vers la cible',
          moveUp: 'Déplacer vers le haut',
          navigation: 'Navigation',
          next: 'Suivant',
          nextPageLabel: 'Page Suivante',
          nullLabel: 'Aucune sélection',
          otpLabel: 'Veuillez saisir le caractère de mot de passe à usage unique {0}',
          pageLabel: 'Page {page}',
          passwordHide: 'Masquer le mot de passe',
          passwordShow: 'Montrer le mot de passe',
          previous: 'Précédent',
          previousPageLabel: 'Page précédente',
          rotateLeft: 'Tourner vers la gauche',
          rotateRight: 'Tourner vers la droite',
          rowsPerPageLabel: 'Lignes par page',
          saveEdit: 'Sauvegarde l\'édition',
          scrollTop: 'Défiler tout en haut',
          selectAll: 'Tous éléments sélectionnés',
          selectLabel: 'Sélectionner',
          selectRow: 'Ligne sélectionnée',
          showFilterMenu: 'Montre le menu des filtres',
          slide: 'Diapositive',
          slideNumber: '{slideNumber}',
          star: '1 étoile',
          stars: '{star} étoiles',
          trueLabel: 'Vrai',
          unselectAll: 'Tous éléments désélectionnés',
          unselectLabel: 'Désélectionner',
          unselectRow: 'Ligne désélectionnée',
          zoomImage: 'Zoomer l\'image',
          zoomIn: 'Zoomer',
          zoomOut: 'Dézoomer',
        },
      },
    },
  },
  runtimeConfig: isDev ? devRuntimeConfig : runtimeConfig,

  imports: { // add folders here to auto-import them in your application
    dirs: [
      'stores',
      'composables/**',
    ],
  },

  components: [{ path: '~/components', pathPrefix: false }],

  typescript: {
    tsConfig: {
      compilerOptions: {
        moduleResolution: 'bundler',
      },
    },
  },

  vite: {
    vue: {
      script: {
        defineModel: true,
        propsDestructure: true,
      },
    },
  },

  experimental: {
    typedPages: true,
  },

  // uncomment to disable SSR. This will basically make the app a SPA, like a normal Vue app, but with all the Nuxt goodies
  // ssr: false,

  // global CSS files
  css: [
    'primevue/resources/themes/lara-light-purple/theme.css',
  ],

  // plugin configurations
  modules: [
    '@nuxtjs/i18n',
    '@vueuse/nuxt',
    '@unocss/nuxt',
    '@nuxtjs/critters',
    '@nuxtjs/color-mode',
    '@pinia/nuxt',
    'nuxt-primevue',
  ],

  i18n: {

  },

  colorMode: {
    preference: 'system',
    fallback: 'light',
    classPrefix: '',
    classSuffix: '',
    storageKey: 'color-scheme',
  },

  ssr: false,
  compatibilityDate: '2024-09-09',
})
