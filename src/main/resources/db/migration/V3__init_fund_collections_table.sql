-- FUND COLLECTIONS
CREATE TABLE fund_collections (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  application_id            UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,

  slug                      VARCHAR(100) NOT NULL UNIQUE,

  title                     VARCHAR(255) NOT NULL,
  description               TEXT,
  cover_image_url           TEXT,

  currency                  VARCHAR(3) NOT NULL DEFAULT 'XAF',
  is_amount_fixed           BOOLEAN NOT NULL DEFAULT FALSE,
  amount                    BIGINT,

  expires_at                TIMESTAMPTZ,

  status                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

  collect_customer_info     BOOLEAN NOT NULL DEFAULT TRUE,
  thank_you_message         TEXT,

  CONSTRAINT chk_amount_when_fixed CHECK (
    is_amount_fixed = FALSE OR (is_amount_fixed = TRUE AND amount IS NOT NULL AND amount > 0)
  )
);

CREATE INDEX idx_fund_collections_app    ON fund_collections(application_id);
CREATE INDEX idx_fund_collections_slug   ON fund_collections(slug);
CREATE INDEX idx_fund_collections_status ON fund_collections(status);
