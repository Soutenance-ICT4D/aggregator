-- Rendre application_id nullable sur transactions_out (retraits marchand sans application associée)
ALTER TABLE transactions_out ALTER COLUMN application_id DROP NOT NULL;

-- Ajouter user_id directement sur transactions_out
ALTER TABLE transactions_out ADD COLUMN user_id UUID;

-- Peupler user_id depuis application pour les lignes existantes
UPDATE transactions_out t
SET user_id = a.user_id
FROM applications a
WHERE t.application_id = a.id;

-- Rendre user_id obligatoire maintenant qu'il est peuplé
ALTER TABLE transactions_out ALTER COLUMN user_id SET NOT NULL;

-- Contrainte FK
ALTER TABLE transactions_out
    ADD CONSTRAINT fk_transactions_out_user FOREIGN KEY (user_id) REFERENCES users(id);
