-- USER BALANCES
CREATE TABLE user_balances (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  user_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  currency              VARCHAR(3) NOT NULL DEFAULT 'XAF',
  available_amount      BIGINT NOT NULL DEFAULT 0,
  pending_amount        BIGINT NOT NULL DEFAULT 0,

  CONSTRAINT unique_user_currency UNIQUE(user_id, currency),

  CONSTRAINT check_positive_amounts
    CHECK (
      available_amount >= 0 AND
      pending_amount >= 0
    )
);

CREATE INDEX idx_user_balances_user      ON user_balances(user_id);
CREATE INDEX idx_user_balances_available ON user_balances(available_amount) WHERE available_amount > 0;
CREATE INDEX idx_user_balances_currency  ON user_balances(currency);


-- PAYMENT PROVIDERS
CREATE TABLE payment_providers (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  code              VARCHAR(50) NOT NULL UNIQUE,
  name              VARCHAR(100) NOT NULL,

  type              VARCHAR(20) NOT NULL,
  country           VARCHAR(3) NOT NULL,
  currency          VARCHAR(3) NOT NULL,

  is_active         BOOLEAN NOT NULL DEFAULT TRUE,

  fee_percentage    DECIMAL(5, 2),
  fee_fixed         BIGINT,

  min_amount        BIGINT,
  max_amount        BIGINT
);

CREATE INDEX idx_payment_providers_code    ON payment_providers(code);
CREATE INDEX idx_payment_providers_country ON payment_providers(country);
CREATE INDEX idx_payment_providers_active  ON payment_providers(is_active) WHERE is_active = TRUE;


-- TRANSACTIONS IN
CREATE TABLE transactions_in (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  reference                 VARCHAR(100) NOT NULL UNIQUE,

  type                      VARCHAR(20) NOT NULL,         -- CHECKOUT | CHARGE | FUND_COLLECTION

  application_id            UUID NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
  fund_collection_id        UUID REFERENCES fund_collections(id) ON DELETE SET NULL,
  payment_provider_id       UUID REFERENCES payment_providers(id) ON DELETE RESTRICT,

  -- Champs spécifiques au flow CHECKOUT (web)
  session_token             VARCHAR(100) UNIQUE,
  expires_at                TIMESTAMPTZ,

  currency                  VARCHAR(3) NOT NULL,
  amount                    BIGINT NOT NULL,
  fee_amount                BIGINT NOT NULL DEFAULT 0,
  net_amount                BIGINT NOT NULL,

  provider_transaction_id   VARCHAR(255),

  description               TEXT,
  merchant_reference        VARCHAR(255),

  customer_name             VARCHAR(255),
  customer_email            VARCHAR(255),
  customer_phone            VARCHAR(20),

  payer_account             VARCHAR(100),
  payer_name                VARCHAR(255),
  payer_email               VARCHAR(255),

  success_url               VARCHAR(500),
  cancel_url                VARCHAR(500),

  status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING',

  failure_reason            TEXT,
  failure_code              VARCHAR(50),

  idempotency_key           VARCHAR(100) UNIQUE,

  -- fund_collection_id obligatoire si type = FUND_COLLECTION
  CONSTRAINT chk_fund_collection_required
    CHECK (type != 'FUND_COLLECTION' OR fund_collection_id IS NOT NULL)
);

CREATE INDEX idx_transactions_in_reference    ON transactions_in(reference);
CREATE INDEX idx_transactions_in_type         ON transactions_in(type);
CREATE INDEX idx_transactions_in_session      ON transactions_in(session_token) WHERE session_token IS NOT NULL;
CREATE INDEX idx_transactions_in_app          ON transactions_in(application_id);
CREATE INDEX idx_transactions_in_fund_coll    ON transactions_in(fund_collection_id);
CREATE INDEX idx_transactions_in_provider     ON transactions_in(payment_provider_id);
CREATE INDEX idx_transactions_in_status       ON transactions_in(status);
CREATE INDEX idx_transactions_in_payer        ON transactions_in(payer_account);
CREATE INDEX idx_transactions_in_created_at   ON transactions_in(created_at DESC);
CREATE INDEX idx_transactions_in_merchant_ref ON transactions_in(merchant_reference);


-- TRANSACTIONS OUT
CREATE TABLE transactions_out (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  reference                 VARCHAR(100) NOT NULL UNIQUE,

  application_id            UUID NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
  payment_provider_id       UUID REFERENCES payment_providers(id) ON DELETE RESTRICT,

  currency                  VARCHAR(3) NOT NULL,
  amount                    BIGINT NOT NULL,
  fee_amount                BIGINT NOT NULL DEFAULT 0,
  net_amount                BIGINT NOT NULL,

  provider_transaction_id   VARCHAR(255),

  description               TEXT,
  merchant_reference        VARCHAR(255),

  beneficiary_name          VARCHAR(255) NOT NULL,
  beneficiary_email         VARCHAR(255),
  beneficiary_account       VARCHAR(100),

  status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING',

  failure_reason            TEXT,
  failure_code              VARCHAR(50)
);

CREATE INDEX idx_transactions_out_reference    ON transactions_out(reference);
CREATE INDEX idx_transactions_out_app          ON transactions_out(application_id);
CREATE INDEX idx_transactions_out_provider     ON transactions_out(payment_provider_id);
CREATE INDEX idx_transactions_out_status       ON transactions_out(status);
CREATE INDEX idx_transactions_out_created_at   ON transactions_out(created_at DESC);
CREATE INDEX idx_transactions_out_merchant_ref ON transactions_out(merchant_reference);
CREATE INDEX idx_transactions_out_beneficiary  ON transactions_out(beneficiary_account);
