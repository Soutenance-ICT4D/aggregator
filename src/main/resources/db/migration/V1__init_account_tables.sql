-- USERS
CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  full_name         VARCHAR(255) NOT NULL,
  email             VARCHAR(255) NOT NULL UNIQUE,
  phone             VARCHAR(20),
  country           VARCHAR(3),
  avatar_url        TEXT,
  
  password_hash     VARCHAR(255),
  provider          VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
  provider_id       VARCHAR(255),
  
  role              VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
  status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  kyc_level         VARCHAR(20) NOT NULL DEFAULT 'NONE',
  
  email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
  phone_verified    BOOLEAN NOT NULL DEFAULT FALSE,
  
  CONSTRAINT unique_provider_user UNIQUE(provider, provider_id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_provider ON users(provider, provider_id);

-- REFRESH TOKENS
CREATE TABLE refresh_tokens (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash    VARCHAR(255) NOT NULL UNIQUE,
  family_id     UUID NOT NULL,
  
  is_revoked    BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at    TIMESTAMPTZ NOT NULL,
  
  ip_address    VARCHAR(45),
  user_agent    TEXT
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

-- OTP CODES
CREATE TABLE otp_codes (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  
  code_hash     VARCHAR(255) NOT NULL,
  
  purpose       VARCHAR(50) NOT NULL,
  
  expires_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_otp_user_purpose ON otp_codes(user_id, purpose);
CREATE INDEX idx_otp_expires_at ON otp_codes(expires_at);
