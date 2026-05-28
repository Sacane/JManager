-- RGPD Art. 5(1)(e) — Limitation de la conservation.
-- Index partiel sur creation_date filtré par l'absence de consentement (tos_accepted_at IS NULL).
-- Seules les lignes éligibles à la purge sont indexées, ce qui minimise la taille de l'index
-- et garantit un Index Scan efficace pour la requête de rétention nocturne.
-- Une fois qu'un utilisateur accepte les CGU (tos_accepted_at non nul), sa ligne sort
-- automatiquement de l'index — aucune maintenance nécessaire.

CREATE INDEX IF NOT EXISTS idx_user_unconsented_creation_date
    ON user_resource (creation_date)
    WHERE tos_accepted_at IS NULL;
