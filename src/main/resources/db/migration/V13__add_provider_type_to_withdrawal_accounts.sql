ALTER TABLE withdrawal_accounts ADD COLUMN provider_type VARCHAR(20);

UPDATE withdrawal_accounts wa
SET provider_type = pp.type
FROM payment_providers pp
WHERE wa.provider_code = pp.code;

ALTER TABLE withdrawal_accounts ALTER COLUMN provider_type SET NOT NULL;
