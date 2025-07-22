-- V151__create_customer_contact_lookup.sql
-- Contact information and preferred username calculation

CREATE VIEW customer_contact_lookup AS
SELECT
    c.id as customer_id,
    c.email_id,
    c.phone_id,
    ce.email,
    cp.phone,
    ce.is_verified as email_verified,
    cp.is_verified as phone_verified,

    -- Calculated preferred username (email takes precedence)
    CASE
        WHEN ce.email IS NOT NULL THEN ce.email
        WHEN cp.phone IS NOT NULL THEN cp.phone
        ELSE NULL
    END as preferred_username,

    -- Contact availability flags
    ce.email IS NOT NULL as has_email,
    cp.phone IS NOT NULL as has_phone,

    -- Contact type for business logic
    CASE
        WHEN ce.email IS NOT NULL AND cp.phone IS NOT NULL THEN 'BOTH'
        WHEN ce.email IS NOT NULL THEN 'EMAIL_ONLY'
        WHEN cp.phone IS NOT NULL THEN 'PHONE_ONLY'
        ELSE 'NONE'
    END as contact_type

FROM customers c
    LEFT JOIN customer_emails ce ON c.email_id = ce.id
    LEFT JOIN customer_phones cp ON c.phone_id = cp.id;