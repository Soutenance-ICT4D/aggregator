-- Comptes de retrait du marchand
CREATE TABLE withdrawal_accounts (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  provider_code  VARCHAR(50)  NOT NULL,
  provider_name  VARCHAR(100) NOT NULL,
  account_number VARCHAR(100) NOT NULL,
  account_name   VARCHAR(255) NOT NULL,
  is_default     BOOLEAN      NOT NULL DEFAULT false
);

CREATE INDEX idx_withdrawal_accounts_user ON withdrawal_accounts(user_id);

-- Un seul compte par défaut par marchand (index partiel)
CREATE UNIQUE INDEX idx_unique_default_account
  ON withdrawal_accounts(user_id)
  WHERE is_default = true;

-- Configuration du mode de retrait automatique
CREATE TABLE withdrawal_configs (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  user_id           UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  mode              VARCHAR(20) NOT NULL DEFAULT 'MANUAL',

  -- Compte cible (utilisé pour INSTANT, THRESHOLD, PERIODIC)
  account_id        UUID        REFERENCES withdrawal_accounts(id) ON DELETE SET NULL,

  -- THRESHOLD : seuil en unité de base (ex : 50000 FCFA)
  threshold_amount  BIGINT,

  -- PERIODIC : DAILY | WEEKLY | MONTHLY
  period            VARCHAR(20),

  currency          VARCHAR(3)  NOT NULL DEFAULT 'XAF',

  -- PERIODIC : date du dernier déclenchement automatique
  last_triggered_at TIMESTAMPTZ
);

CREATE INDEX idx_withdrawal_configs_user ON withdrawal_configs(user_id);
CREATE INDEX idx_withdrawal_configs_mode ON withdrawal_configs(mode) WHERE mode <> 'MANUAL';
