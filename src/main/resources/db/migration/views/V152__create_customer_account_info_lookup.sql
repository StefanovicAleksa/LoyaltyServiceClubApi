-- V152__create_customer_account_info_lookup.sql
-- View with customer account information for triggers and app logic

CREATE VIEW customer_account_info_lookup AS
SELECT
    -- Core account info
    ca.id as account_id,
    ca.username,
    ca.activity_status,
    ca.verification_status,
    ca.last_login_at,

    -- Customer basic info
    ca.customer_id,
    c.first_name,
    c.last_name,

    -- Contact information
    ce.email,
    cp.phone,

    -- Contact type for business logic
    CASE
        WHEN ce.email IS NOT NULL AND cp.phone IS NOT NULL THEN 'BOTH'
        WHEN ce.email IS NOT NULL THEN 'EMAIL_ONLY'
        WHEN cp.phone IS NOT NULL THEN 'PHONE_ONLY'
        ELSE 'NONE'
        END as contact_type

FROM customer_accounts ca
         INNER JOIN customers c ON ca.customer_id = c.id
         LEFT JOIN customer_emails ce ON c.email_id = ce.id
         LEFT JOIN customer_phones cp ON c.phone_id = cp.id;