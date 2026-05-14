-- Le webhook secret devient optionnel : les merchants le créent explicitement
-- comme les API Keys, et peuvent le révoquer complètement.
ALTER TABLE applications ALTER COLUMN webhook_secret DROP NOT NULL;

-- Historique et retry des livraisons webhook.
-- status : PENDING (en attente d'envoi) | DELIVERED (envoyé avec succès) | FAILED (échecs)
-- next_retry_at : null = pas de retry planifié (DELIVERED ou abandon après max tentatives)
CREATE TABLE webhook_deliveries (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID        NOT NULL REFERENCES applications(id),
    event_name      VARCHAR(100) NOT NULL,
    payload         TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    http_status     INT,
    attempt_count   INT         NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index pour le scheduler de retry : filtre sur status + next_retry_at + attempt_count
CREATE INDEX idx_webhook_deliveries_retry
    ON webhook_deliveries (status, next_retry_at, attempt_count)
    WHERE status = 'FAILED';

-- Index pour l'endpoint historique du merchant : filtre par application
CREATE INDEX idx_webhook_deliveries_app
    ON webhook_deliveries (application_id, created_at DESC);
