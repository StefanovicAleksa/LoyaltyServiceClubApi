-- V151__create_customer_verification_lookup_view.sql
-- Unified view for customer verification status lookups by email or phone

CREATE VIEW customer_verification_lookup AS
SELECT c.email_id,
       c.phone_id,
       ca.verification_status,
       ca.customer_id,
       ca.id as customer_account_id
FROM customer_accounts ca
JOIN customers c ON ca.customer_id = c.id;