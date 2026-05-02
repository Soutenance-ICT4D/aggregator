-- PAYMENT PROVIDERS — données de référence
-- Montants en XAF (unité de base, pas de centimes pour le FCFA)

INSERT INTO payment_providers (code, name, type, country, currency, is_active, fee_percentage, fee_fixed, min_amount, max_amount)
VALUES
    (
        'MTN_MOMO_CM',
        'MTN Mobile Money Cameroun',
        'MOBILE_MONEY',
        'CM',
        'XAF',
        TRUE,
        1.50,       -- 1,5 % de frais
        0,          -- pas de frais fixes
        10,        -- minimum 100 XAF
        1500000     -- maximum 1 500 000 XAF
    ),
    (
        'ORANGE_MONEY_CM',
        'Orange Money Cameroun',
        'MOBILE_MONEY',
        'CM',
        'XAF',
        TRUE,
        1.50,       -- 1,5 % de frais
        0,          -- pas de frais fixes
        10,        -- minimum 100 XAF
        1500000     -- maximum 1 500 000 XAF
    );
