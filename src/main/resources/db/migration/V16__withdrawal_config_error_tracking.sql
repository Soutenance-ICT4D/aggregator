-- Suivi des erreurs de retrait automatique et circuit breaker
ALTER TABLE withdrawal_configs ADD COLUMN consecutive_failures INT NOT NULL DEFAULT 0;
ALTER TABLE withdrawal_configs ADD COLUMN last_error_at TIMESTAMPTZ;
ALTER TABLE withdrawal_configs ADD COLUMN last_error_message TEXT;
