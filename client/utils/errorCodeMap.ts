type ProblemDetailProperties = {
  code?: number
  requestId?: string
  userId?: string
}

export type DiagnosticContext = {
  requestId?: string
  userId?: string
  code?: number
}

export type ApiProblemDetail = {
  code?: number
  properties?: ProblemDetailProperties
  requestId?: string
  userId?: string
}

type ErrorMessage = {
  summary: string
  detail: string
}

const errorMessagesByCode: Record<number, ErrorMessage> = {
  1: { summary: 'Erreur de delai', detail: 'La requete a expire. Merci de reessayer.' },
  2: { summary: 'Requete invalide', detail: 'Les donnees envoyees sont invalides.' },
  3: { summary: 'Action interdite', detail: 'Vous n avez pas les droits pour cette action.' },
  4: { summary: 'Ressource introuvable', detail: 'La ressource demandee est introuvable.' },
  5: { summary: 'Non authentifie', detail: 'Votre session n est plus valide. Merci de vous reconnecter.' },
  6: { summary: 'Erreur interne', detail: 'Une erreur interne est survenue. Merci de reessayer.' },
  7: { summary: 'Mauvaise requete', detail: 'La requete est invalide.' },
  8: { summary: 'Erreur infrastructure', detail: 'Une erreur technique est survenue. Merci de reessayer.' },
  65: { summary: 'Validation', detail: 'Certaines donnees saisies sont invalides.' },
  67: { summary: 'Type invalide', detail: 'Le type d une valeur envoyee est invalide.' },
  68: { summary: 'Date invalide', detail: 'La date fournie est invalide.' },
  111: { summary: 'Erreur interne', detail: 'Une erreur inattendue est survenue.' },
  144: { summary: 'Devise invalide', detail: 'La devise fournie est invalide.' },
  404: { summary: 'Ressource introuvable', detail: 'La ressource demandee est introuvable.' },
  1001: { summary: 'Compte introuvable', detail: 'Le compte demande est introuvable.' },
  1002: { summary: 'Transaction introuvable', detail: 'La transaction demandee est introuvable.' },
  1003: { summary: 'Tag introuvable', detail: 'Le tag demande est introuvable.' },
  1004: { summary: 'Utilisateur introuvable', detail: 'L utilisateur demande est introuvable.' },
  1005: { summary: 'Libelle introuvable', detail: 'Le libelle du compte n existe pas.' },
  1006: { summary: 'Tag manquant', detail: 'Le tag par defaut n est pas defini.' },
  2001: { summary: 'Libelle deja utilise', detail: 'Le libelle de compte existe deja.' },
  2002: { summary: 'Tag deja utilise', detail: 'Le libelle du tag est deja utilise.' },
  2003: { summary: 'Action non autorisee', detail: 'Le tag par defaut ne peut pas etre modifie.' },
  2004: { summary: 'Limite atteinte', detail: 'La taille maximale du compte est atteinte.' },
  2005: { summary: 'Tag utilisé', detail: 'Ce tag est utilisé dans des transactions existantes.' },
  3001: { summary: 'Authentification requise', detail: 'Vous devez etre connecte pour continuer.' },
  3002: { summary: 'Non autorise', detail: 'Vous n etes pas autorise a effectuer cette action.' },
  3003: { summary: 'Identifiants invalides', detail: 'Le mot de passe fourni est invalide.' },
  5000: { summary: 'Ecriture invalide', detail: 'La transaction ne peut pas etre enregistree.' },
  5001: { summary: 'Inscription invalide', detail: 'L inscription a echoue. Merci de verifier les champs.' },
}

const defaultMessage: ErrorMessage = {
  summary: 'Erreur',
  detail: 'Une erreur inconnue est survenue.',
}

export function extractErrorCode(payload: unknown): number | undefined {
  if (!payload || typeof payload !== 'object') return undefined

  const problemDetail = payload as ApiProblemDetail
  if (typeof problemDetail.code === 'number') return problemDetail.code
  if (typeof problemDetail.properties?.code === 'number') return problemDetail.properties.code

  return undefined
}

export function extractDiagnosticContext(payload: unknown): DiagnosticContext {
  if (!payload || typeof payload !== 'object') return {}
  const pd = payload as ApiProblemDetail
  const requestId = typeof pd.requestId === 'string'
    ? pd.requestId
    : typeof pd.properties?.requestId === 'string' ? pd.properties.requestId : undefined
  const userId = typeof pd.userId === 'string'
    ? pd.userId
    : typeof pd.properties?.userId === 'string' ? pd.properties.userId : undefined
  return {
    requestId,
    userId,
    code: extractErrorCode(payload),
  }
}

export function buildDiagnosticText(ctx: DiagnosticContext): string {
  return [
    ctx.requestId ? `requestId: ${ctx.requestId}` : null,
    ctx.userId ? `userId: ${ctx.userId}` : null,
    ctx.code !== undefined ? `code: ${ctx.code}` : null,
    `date: ${new Date().toISOString()}`,
  ].filter(Boolean).join('\n')
}

export function getErrorMessageByCode(code?: number): ErrorMessage {
  if (code === undefined) return defaultMessage
  return errorMessagesByCode[code] ?? defaultMessage
}
