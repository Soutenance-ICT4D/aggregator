-- Remplace la contrainte d'unicité (user_id, name) par un index partiel
-- qui exclut les applications supprimées (status = 'DELETED').
-- Permet de recréer une app avec un nom déjà utilisé par une app supprimée.

ALTER TABLE applications DROP CONSTRAINT unique_user_app_name;

CREATE UNIQUE INDEX unique_user_app_name
    ON applications(user_id, name)
    WHERE status != 'DELETED';
