-- V203__create_auto_username_function.sql
-- Function to automatically set username on customer account creation

CREATE OR REPLACE FUNCTION set_customer_account_username()
    RETURNS TRIGGER AS $$
DECLARE
    customer_email VARCHAR(50);
    customer_phone VARCHAR(12);
BEGIN
    IF NEW.username IS NULL OR NEW.username = '' THEN

        SELECT email, phone
        INTO customer_email, customer_phone
        FROM customer_account_info_lookup
        WHERE customer_id = NEW.customer_id;

        IF customer_email IS NOT NULL THEN
            NEW.username = customer_email;
        ELSIF customer_phone IS NOT NULL THEN
            NEW.username = customer_phone;
        ELSE
            RAISE EXCEPTION 'Account creation failed: No email or phone available';
        END IF;

    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;