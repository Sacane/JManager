CREATE TABLE IF NOT EXISTS user_role (
    id_user BIGINT NOT NULL REFERENCES user_resource (id_user),
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (id_user, role)
);