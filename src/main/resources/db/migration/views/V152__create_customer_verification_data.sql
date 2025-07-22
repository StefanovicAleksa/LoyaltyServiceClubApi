-- V152__create_customer_verification_data.sql
-- Comprehensive verification status calculation

CREATE VIEW customer_verification_data AS
SELECT
    ca.id as account_id,
    ca.customer_id,
    ca.verification_status as current_status,
    c.email_id,
    c.phone_id,

    -- Verification states
    COALESCE(ce.is_verified, false) as email_verified,
    COALESCE(cp.is_verified, false) as phone_verified,

    -- Contact availability
    ce.email IS NOT NULL as has_email,
    cp.phone IS NOT NULL as has_phone,

    -- Calculated verification status based on current state
    CASE
        WHEN COALESCE(ce.is_verified, false) AND COALESCE(cp.is_verified, false) THEN 'FULLY_VERIFIED'
        WHEN COALESCE(ce.is_verified, false) THEN 'EMAIL_VERIFIED'
        WHEN COALESCE(cp.is_verified, false) THEN 'PHONE_VERIFIED'
        ELSE 'UNVERIFIED'
        END as calculated_status,

    -- Status change indicators
    ca.verification_status != (CASE
                                   WHEN COALESCE(ce.is_verified, false) AND COALESCE(cp.is_verified, false) THEN 'FULLY_VERIFIED'
                                   WHEN COALESCE(ce.is_verified, false) THEN 'EMAIL_VERIFIED'
                                   WHEN COALESCE(cp.is_verified, false) THEN 'PHONE_VERIFIED'
                                   ELSE 'UNVERIFIED'
        END)::customer_account_verification_status_enum as status_needs_update

FROM customer_accounts ca
         JOIN customers c ON ca.customer_id = c.id
         LEFT JOIN customer_emails ce ON c.email_id = ce.id
         LEFT JOIN customer_phones cp ON c.phone_id = cp.id;