-- Migration V8: Convertir regular_transaction_id de VARCHAR(255) vers UUID
-- Cette migration corrige le type de la colonne pour correspondre au type de regular_transaction.transaction_id

-- Étape 1 : Nettoyer les données invalides (IDs qui ne sont pas des UUIDs valides)
-- Supprimer les transactions prévisionnelles avec des IDs invalides ou inexistants
DELETE FROM sheet
WHERE regular_transaction_id IS NOT NULL
AND (
    -- ID n'est pas un UUID valide
    regular_transaction_id !~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    OR
    -- ID ne correspond à aucune transaction régulière
    NOT EXISTS (
        SELECT 1 FROM regular_transaction rt
        WHERE rt.transaction_id::text = regular_transaction_id
    )
);

-- Étape 2 : Créer une nouvelle colonne temporaire de type UUID
ALTER TABLE sheet ADD COLUMN regular_transaction_id_uuid UUID;

-- Étape 3 : Migrer les données valides vers la nouvelle colonne
UPDATE sheet
SET regular_transaction_id_uuid = regular_transaction_id::uuid
WHERE regular_transaction_id IS NOT NULL
AND regular_transaction_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

-- Étape 4 : Supprimer l'ancienne colonne et l'index
DROP INDEX IF EXISTS idx_sheet_regular_transaction_id;
ALTER TABLE sheet DROP COLUMN regular_transaction_id;

-- Étape 5 : Renommer la nouvelle colonne
ALTER TABLE sheet RENAME COLUMN regular_transaction_id_uuid TO regular_transaction_id;

-- Étape 6 : Recréer l'index sur la colonne UUID
CREATE INDEX idx_sheet_regular_transaction_id ON sheet(regular_transaction_id);

-- Étape 7 : Ajouter une contrainte de clé étrangère pour garantir l'intégrité référentielle
-- Cela empêchera la création de transactions prévisionnelles avec un ID inexistant
ALTER TABLE sheet
ADD CONSTRAINT fk_sheet_regular_transaction
FOREIGN KEY (regular_transaction_id)
REFERENCES regular_transaction(transaction_id)
ON DELETE SET NULL;  -- Si la transaction régulière est supprimée, mettre NULL

-- Étape 8 : Ajouter un commentaire explicatif
COMMENT ON COLUMN sheet.regular_transaction_id IS
'UUID de la transaction régulière source (si cette transaction provient d''une transaction régulière). Type UUID pour correspondre à regular_transaction.transaction_id';

-- Vérification après migration
DO $$
DECLARE
    invalid_count INTEGER;
    orphan_count INTEGER;
BEGIN
    -- Compter les transactions avec des IDs invalides (ne devrait pas y en avoir)
    SELECT COUNT(*) INTO invalid_count
    FROM sheet
    WHERE regular_transaction_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM regular_transaction rt
        WHERE rt.transaction_id = regular_transaction_id
    );

    IF invalid_count > 0 THEN
        RAISE WARNING 'Attention: % transaction(s) prévisionnelle(s) avec un regular_transaction_id orphelin détecté(es)', invalid_count;
    ELSE
        RAISE NOTICE 'Migration réussie: Tous les regular_transaction_id sont valides';
    END IF;

    -- Afficher un résumé
    SELECT COUNT(*) INTO orphan_count
    FROM sheet
    WHERE regular_transaction_id IS NOT NULL;

    RAISE NOTICE 'Total de transactions prévisionnelles avec regular_transaction_id: %', orphan_count;
END $$;

