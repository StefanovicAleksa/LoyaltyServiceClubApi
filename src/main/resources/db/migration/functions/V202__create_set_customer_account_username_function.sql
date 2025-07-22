-- V202__create_set_customer_account_username_function.sql
-- Username setting function using customer_contact_lookup view

CREATE OR REPLACE FUNCTION set_customer_account_username()
    RETURNS TRIGGER AS
$$
DECLARE
    username_value TEXT;
BEGIN
    -- Use view to get preferred username
    SELECT preferred_username
    INTO username_value
    FROM customer_contact_lookup
    WHERE customer_id = NEW.customer_id;

-- Validate username is available
    IF username_value IS NULL THEN
        RAISE EXCEPTION 'Cannot create account: customer has no email or phone contact information';
    END IF;

-- Set username
    NEW.username = username_value;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;