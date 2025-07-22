-- V153__create_account_activity_data.sql
-- Account activity information for inactivity processing

CREATE VIEW account_activity_data AS
SELECT
    ca.id as account_id,
    ca.customer_id,
    ca.activity_status,
    ca.last_login_at,

    -- Calculated inactivity status (functions will provide config values)
    ca.last_login_at IS NOT NULL as has_logged_in,

    -- Days since last login
    CASE
        WHEN ca.last_login_at IS NOT NULL
            THEN EXTRACT(DAYS FROM (CURRENT_TIMESTAMP - ca.last_login_at))::INTEGER
        ELSE NULL
        END as days_since_login

FROM customer_accounts ca;