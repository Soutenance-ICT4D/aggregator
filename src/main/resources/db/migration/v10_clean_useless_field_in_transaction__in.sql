-- Les champs customer_name et customer_email car jamais utiliser dans notre implmentation actuel

ALTER TABLE transaction_in DROP COLUMN customer_name;
ALTER TABLE transaction_in DROP COLUMN customer_email;