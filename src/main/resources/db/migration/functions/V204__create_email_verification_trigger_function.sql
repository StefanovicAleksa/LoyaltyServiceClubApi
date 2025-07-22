-- V204__create_email_verification_trigger_function.sql
-- Email verification trigger wrapper function

CREATE OR REPLACE FUNCTION handle_email_verification_change()
    RETURNS TRIGGER AS
$$
DECLARE
    customer_id_value BIGINT;
BEGIN
    -- Find customer ID for this email
    SELECT id
    INTO customer_id_value
    FROM customers
    WHERE email_id = NEW.id;

    -- Update verification status if customer found
    IF FOUND THEN
        PERFORM update_verification_status_for_customer(customer_id_value);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;