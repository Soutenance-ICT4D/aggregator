-- Idempotency key : passage d'une unicité GLOBALE à une unicité PAR APPLICATION.
--
-- Problème corrigé : deux applications distinctes ne pouvaient pas réutiliser la même
-- clé d'idempotence (collision inter-marchands), ce qui n'est pas conforme au standard
-- (une clé d'idempotence est choisie par le client et n'a de sens que dans son propre scope).
--
-- Sécurité migration : la nouvelle contrainte (application_id, idempotency_key) est
-- STRICTEMENT plus permissive que l'ancienne (globale). Toute donnée valide sous l'ancienne
-- contrainte l'est donc aussi sous la nouvelle -> aucun risque d'échec sur les données existantes.
-- Les lignes avec idempotency_key NULL restent autorisées et non conflictuelles (NULL distincts en SQL).

-- 1) Suppression robuste de l'ancienne contrainte UNIQUE globale.
--    On la cible par sa STRUCTURE (unique sur la seule colonne idempotency_key) et non par
--    son nom, pour garantir sa suppression quel que soit le nom auto-généré selon l'environnement.
DO $$
DECLARE
    v_attnum  smallint;
    v_conname text;
BEGIN
    SELECT a.attnum INTO v_attnum
    FROM pg_attribute a
    JOIN pg_class rel ON rel.oid = a.attrelid
    WHERE rel.relname = 'transactions_in'
      AND a.attname = 'idempotency_key'
      AND a.attnum > 0
      AND NOT a.attisdropped;

    FOR v_conname IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'transactions_in'
          AND con.contype = 'u'
          AND con.conkey = ARRAY[v_attnum]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE transactions_in DROP CONSTRAINT %I', v_conname);
    END LOOP;
END $$;

-- 2) Nouvelle contrainte : unicité de la clé d'idempotence PAR application.
ALTER TABLE transactions_in
    ADD CONSTRAINT uq_transactions_in_application_idempotency
    UNIQUE (application_id, idempotency_key);
