package fr.sacane.jmanager.infrastructure.spi.adapters.mail

/**
 * Produces HTML email content for each registration context.
 * Each template returns a [EmailContent] carrying both the subject line and the HTML body.
 * Adding a new email type means adding a new internal function — no other class needs to change.
 */
internal object EmailTemplates {

    data class EmailContent(val subject: String, val htmlBody: String)

    fun betaTester(username: String, loginUrl: String): EmailContent = EmailContent(
        subject = "Bienvenue dans la bêta JManager 🎉",
        htmlBody = betaTesterHtml(username, loginUrl),
    )

    fun freeUser(username: String, loginUrl: String): EmailContent = EmailContent(
        subject = "Bienvenue sur JManager !",
        htmlBody = freeUserHtml(username, loginUrl),
    )

    fun premiumUser(username: String, loginUrl: String): EmailContent = EmailContent(
        subject = "Bienvenue dans JManager Premium ✨",
        htmlBody = premiumHtml(username, loginUrl),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Private HTML builders
    // ─────────────────────────────────────────────────────────────────────────

    private fun betaTesterHtml(username: String, loginUrl: String): String = baseLayout(
        badge = """<span style="display:inline-block;background:#822acc;color:#ffffff;padding:3px 14px;border-radius:20px;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">✦ Accès Bêta Exclusif</span>""",
        body = """
            <h2 style="margin:0 0 6px;color:#1a1a2e;font-size:22px;font-weight:700;">Bonjour $username 👋</h2>
            <p style="margin:0 0 24px;color:#6b7280;font-size:14px;line-height:1.6;">
              Vous faites partie de nos tout premiers utilisateurs — et c'est immense pour nous.
              Merci d'avoir accepté de faire partie de cette aventure.
            </p>

            <div style="background:#f8f0ff;border:1px solid #e8d5ff;border-radius:10px;padding:20px 24px;margin-bottom:28px;">
              <p style="margin:0 0 12px;color:#822acc;font-weight:700;font-size:14px;">En tant que bêta-testeur, vous bénéficiez de :</p>
              <table width="100%" cellpadding="0" cellspacing="0" role="presentation">
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Accès complet à toutes les fonctionnalités dès aujourd'hui</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Compte Premium offert pour toute la durée de la bêta</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Ligne directe avec l'équipe pour vos retours et suggestions</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Impact réel sur les prochaines évolutions du produit</td></tr>
              </table>
            </div>

            <p style="margin:0 0 28px;color:#4b5563;font-size:14px;line-height:1.7;">
              JManager est construit avec soin pour vous aider à reprendre le contrôle de vos finances personnelles.
              Chaque retour que vous nous ferez — bug, suggestion, coup de cœur — sera lu et pris en compte personnellement.
            </p>
        """.trimIndent(),
        ctaLabel = "Accéder à JManager →",
        loginUrl = loginUrl,
    )

    private fun freeUserHtml(username: String, loginUrl: String): String = baseLayout(
        badge = null,
        body = """
            <h2 style="margin:0 0 6px;color:#1a1a2e;font-size:22px;font-weight:700;">Bienvenue, $username 👋</h2>
            <p style="margin:0 0 24px;color:#6b7280;font-size:14px;line-height:1.6;">
              Votre compte JManager a été créé avec succès. Nous sommes ravis de vous accueillir.
            </p>

            <div style="background:#f8f0ff;border:1px solid #e8d5ff;border-radius:10px;padding:20px 24px;margin-bottom:28px;">
              <p style="margin:0 0 12px;color:#822acc;font-weight:700;font-size:14px;">Avec JManager, vous pouvez :</p>
              <table width="100%" cellpadding="0" cellspacing="0" role="presentation">
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Suivre vos dépenses et revenus au quotidien</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Visualiser votre situation financière en un coup d'œil</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Gérer plusieurs livrets et comptes</td></tr>
              </table>
            </div>

            <p style="margin:0 0 28px;color:#4b5563;font-size:14px;line-height:1.7;">
              Notre équipe est disponible pour répondre à toutes vos questions. N'hésitez pas à nous contacter.
            </p>
        """.trimIndent(),
        ctaLabel = "Découvrir JManager →",
        loginUrl = loginUrl,
    )

    private fun premiumHtml(username: String, loginUrl: String): String = baseLayout(
        badge = """<span style="display:inline-block;background:linear-gradient(135deg,#f59e0b,#d97706);color:#ffffff;padding:3px 14px;border-radius:20px;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">✦ Membre Premium</span>""",
        body = """
            <h2 style="margin:0 0 6px;color:#1a1a2e;font-size:22px;font-weight:700;">Bienvenue dans le Premium, $username ✨</h2>
            <p style="margin:0 0 24px;color:#6b7280;font-size:14px;line-height:1.6;">
              Merci de votre confiance. Votre abonnement Premium est actif et vous donne accès
              à l'expérience JManager complète.
            </p>

            <div style="background:#fff8f0;border:1px solid #fde8c0;border-radius:10px;padding:20px 24px;margin-bottom:28px;">
              <p style="margin:0 0 12px;color:#d97706;font-weight:700;font-size:14px;">Vos avantages Premium :</p>
              <table width="100%" cellpadding="0" cellspacing="0" role="presentation">
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Transactions régulières et prévisionnelles illimitées</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Statistiques avancées et tendances mensuelles</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Import / export CSV</td></tr>
                <tr><td style="padding:4px 0;color:#4b5563;font-size:13px;">✓&nbsp;&nbsp;Support prioritaire</td></tr>
              </table>
            </div>

            <p style="margin:0 0 28px;color:#4b5563;font-size:14px;line-height:1.7;">
              Votre satisfaction est notre priorité. Si vous avez la moindre question ou besoin d'aide
              pour prendre en main votre compte, notre équipe est là pour vous.
            </p>
        """.trimIndent(),
        ctaLabel = "Accéder à mon espace Premium →",
        loginUrl = loginUrl,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Shared layout
    // ─────────────────────────────────────────────────────────────────────────

    private fun baseLayout(
        badge: String?,
        body: String,
        ctaLabel: String,
        loginUrl: String,
    ): String {
        val badgeRow = if (badge != null) """
            <tr>
              <td style="background:#f8f0ff;padding:10px 40px;text-align:center;border-bottom:1px solid #e8d5ff;">
                $badge
              </td>
            </tr>
        """.trimIndent() else ""

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background-color:#f0e8ff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" role="presentation" style="background-color:#f0e8ff;padding:32px 16px;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" role="presentation" style="max-width:560px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(130,42,204,0.12);">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#822acc 0%,#5b1a9e 100%);padding:36px 40px 28px;text-align:center;">
                        <div style="display:inline-block;background:rgba(255,255,255,0.15);border-radius:10px;padding:6px 18px;margin-bottom:14px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:800;letter-spacing:-0.3px;">JManager</span>
                        </div>
                        <p style="color:rgba(255,255,255,0.72);margin:0;font-size:12px;letter-spacing:0.5px;">Votre gestionnaire de finances personnelles</p>
                      </td>
                    </tr>

                    $badgeRow

                    <!-- Body -->
                    <tr>
                      <td style="padding:40px 40px 32px;">
                        $body
                        <!-- CTA -->
                        <table width="100%" cellpadding="0" cellspacing="0" role="presentation">
                          <tr>
                            <td align="center">
                              <a href="$loginUrl"
                                 style="display:inline-block;background:linear-gradient(135deg,#822acc,#5b1a9e);color:#ffffff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:15px;font-weight:600;letter-spacing:-0.2px;box-shadow:0 4px 14px rgba(130,42,204,0.3);">
                                $ctaLabel
                              </a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8f9fa;padding:22px 40px;border-top:1px solid #f0e8ff;text-align:center;">
                        <p style="margin:0 0 6px;color:#9ca3af;font-size:12px;">
                          Une question ? Écrivez-nous à
                          <a href="mailto:support@jmanager.sacane.fr" style="color:#822acc;text-decoration:none;">support@jmanager.sacane.fr</a>
                        </p>
                        <p style="margin:0;color:#d1d5db;font-size:11px;">© 2026 JManager — Tous droits réservés</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
