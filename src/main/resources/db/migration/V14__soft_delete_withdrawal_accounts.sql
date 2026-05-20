ALTER TABLE withdrawal_accounts ADD COLUMN deleted_at TIMESTAMPTZ;

-- Rebuild partial unique index to exclude soft-deleted accounts
DROP INDEX idx_unique_default_account;
CREATE UNIQUE INDEX idx_unique_default_account
  ON withdrawal_accounts(user_id)
  WHERE is_default = true AND deleted_at IS NULL;
