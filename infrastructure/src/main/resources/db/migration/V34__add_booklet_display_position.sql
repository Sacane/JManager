-- Where the user placed a booklet among theirs, from 0, until now kept in the browser only.
-- Null until they order their booklets: such a booklet is listed after the others.
ALTER TABLE booklet
    ADD COLUMN IF NOT EXISTS display_position INTEGER NULL;
