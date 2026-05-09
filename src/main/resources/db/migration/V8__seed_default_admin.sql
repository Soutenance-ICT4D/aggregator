-- Insère un compte admin par défaut si aucun admin n'existe.
-- Configurer via les variables d'environnement :
--   ADMIN_EMAIL           → email du compte (défaut : admin@sharepay.com)
--   ADMIN_PASSWORD_HASH   → hash BCrypt(12), ex : new BCryptPasswordEncoder(12).encode("...")
INSERT INTO users (id, full_name, email, password_hash, role, status, kyc_level, email_verified, phone_verified)
SELECT
    gen_random_uuid(),
    'Super Admin',
    '${adminEmail}',
    '${adminPasswordHash}',
    'ADMIN',
    'ACTIVE',
    'VERIFIED',
    true,
    false
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');
