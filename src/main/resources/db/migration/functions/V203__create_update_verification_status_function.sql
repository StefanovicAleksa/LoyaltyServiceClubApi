-- V203__create_update_verification_status_function.sql
-- Verification status update function using customer_verification_data view

CREATE OR REPLACE FUNCTION update_verification_status_for_customer(customer_id_param BIGINT)
    RETURNS VOID AS
$$
DECLARE
    verification_data RECORD;
BEGIN
    -- Get verification data for the customer's account
    SELECT account_id, calculated_status, status_needs_update
    INTO verification_data
    FROM customer_verification_data
    WHERE customer_id = customer_id_param;

-- Only update if account exists and status needs updating
    IF FOUND AND verification_data.status_needs_update THEN
        UPDATE customer_accounts
        SET verification_status = verification_data.calculated_status::customer_account_verification_status_enum,
            last_modified_date  = CURRENT_TIMESTAMP
        WHERE id = verification_data.account_id;
    END IF;
END;
$$ LANGUAGE plpgsql;