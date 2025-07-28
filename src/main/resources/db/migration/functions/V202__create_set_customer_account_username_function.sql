-- V202__create_set_customer_account_username_function.sql
-- Username setting function using customer_contact_lookup view

CREATE OR REPLACE FUNCTION set_customer_account_username()
    RETURNS TRIGGER AS
$$
DECLARE
    username_value TEXT;
BEGIN
    -- Get preferred username from customer contact lookup view
    SELECT preferred_username
    INTO username_value
    FROM customer_contact_lookup
    WHERE customer_id = NEW.customer_id;

    -- Validate that customer has contact information
    IF username_value IS NULL OR trim(username_value) = '' THEN
        RAISE EXCEPTION 'Cannot create account: customer has no email or phone contact information (customer_id: %)', NEW.customer_id;
    END IF;

    -- Set the username
    NEW.username = trim(username_value);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;