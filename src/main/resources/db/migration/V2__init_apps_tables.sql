-- APPLICATIONS
CREATE TABLE applications (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  name              VARCHAR(255) NOT NULL,
  description       TEXT,
  logo_url          TEXT,
  website_url       VARCHAR(500),

  theme_color       VARCHAR(7),
  currency          VARCHAR(3) NOT NULL DEFAULT 'XAF',

  webhook_url       VARCHAR(500),
  webhook_secret    VARCHAR(255) NOT NULL,
  success_url       VARCHAR(500),
  cancel_url        VARCHAR(500),

  status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

  CONSTRAINT unique_user_app_name UNIQUE(user_id, name)
);

CREATE INDEX idx_applications_user ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);


-- API KEYS
CREATE TABLE api_keys (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  application_id    UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,

  name              VARCHAR(100) NOT NULL,
  key_prefix        VARCHAR(20) NOT NULL,
  key_hash          VARCHAR(255) NOT NULL UNIQUE,

  environment       VARCHAR(10) NOT NULL,

  is_active         BOOLEAN NOT NULL DEFAULT TRUE,

  last_used_at      TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_application ON api_keys(application_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_environment ON api_keys(environment);
CREATE UNIQUE INDEX idx_api_keys_one_active_per_app ON api_keys(application_id) WHERE is_active = TRUE;
