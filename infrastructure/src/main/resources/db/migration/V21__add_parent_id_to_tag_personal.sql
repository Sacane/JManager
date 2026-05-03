ALTER TABLE tag_personal_resource ADD COLUMN parent_id UUID;

ALTER TABLE tag_personal_resource
    ADD CONSTRAINT fk_tag_personal_parent
    FOREIGN KEY (parent_id) REFERENCES tag_personal_resource(id_tag) ON DELETE CASCADE;
