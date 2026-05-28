-- RGPD Art. 17 — Droit à l'effacement.
-- Ajoute ON DELETE CASCADE sur toutes les FK référençant user_resource(id_user)
-- qui ne bénéficient pas déjà d'une suppression en cascade via JPA.
--
-- Note : monthly_regular_transaction a été renommée en regular_transaction (V4).
--        monthly_transaction_booklet a été renommée en regular_transaction_booklet (V4).

-- 1. tag_personal_resource.owner_id_user
--    JPA OneToMany sans CascadeType.ALL → le SQL prend le relais.
ALTER TABLE tag_personal_resource
    DROP CONSTRAINT IF EXISTS tag_personal_resource_owner_id_user_fkey;

ALTER TABLE tag_personal_resource
    ADD CONSTRAINT tag_personal_resource_owner_id_user_fkey
        FOREIGN KEY (owner_id_user) REFERENCES user_resource (id_user) ON DELETE CASCADE;

-- 2. regular_recurring_check_entity.id_user
--    Aucune relation JPA dans UserResource → suppression pilotée par SQL.
ALTER TABLE regular_recurring_check_entity
    DROP CONSTRAINT IF EXISTS regular_recurring_check_entity_id_user_fkey;

ALTER TABLE regular_recurring_check_entity
    ADD CONSTRAINT regular_recurring_check_entity_id_user_fkey
        FOREIGN KEY (id_user) REFERENCES user_resource (id_user) ON DELETE CASCADE;

-- 3. regular_transaction.owner_id_user (anciennement monthly_regular_transaction)
--    Aucune relation JPA dans UserResource → suppression pilotée par SQL.
--    Nom de contrainte original conservé après renommage de table.
ALTER TABLE regular_transaction
    DROP CONSTRAINT IF EXISTS monthly_regular_transaction_owner_id_user_fkey;

ALTER TABLE regular_transaction
    ADD CONSTRAINT regular_transaction_owner_id_user_fkey
        FOREIGN KEY (owner_id_user) REFERENCES user_resource (id_user) ON DELETE CASCADE;

-- 4. regular_transaction_booklet.transaction_id (anciennement monthly_transaction_booklet)
--    Nettoie la table de jointure @ManyToMany quand la regular transaction est supprimée.
ALTER TABLE regular_transaction_booklet
    DROP CONSTRAINT IF EXISTS monthly_transaction_booklet_transaction_id_fkey;

ALTER TABLE regular_transaction_booklet
    ADD CONSTRAINT regular_transaction_booklet_transaction_id_fkey
        FOREIGN KEY (transaction_id) REFERENCES regular_transaction (transaction_id) ON DELETE CASCADE;
