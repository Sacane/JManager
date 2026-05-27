-- RGPD Art. 6 — Base légale du traitement (consentement / exécution du contrat).
-- Ajoute la traçabilité du consentement aux CGU et à la politique de confidentialité.
-- Ces colonnes permettent de démontrer à la CNIL la date, l'heure et la version
-- des documents acceptés par chaque utilisateur lors de son inscription.

ALTER TABLE user_resource
    ADD COLUMN IF NOT EXISTS tos_accepted_at      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tos_version          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS privacy_accepted_at  TIMESTAMP;
