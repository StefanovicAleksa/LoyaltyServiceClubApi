-- V207__create_update_username_on_contact_change_function.sql
-- Username update function for customer contact changes

CREATE OR REPLACE FUNCTION update_username_on_contact_change()
    RETURNS TRIGGER AS $$
DECLARE
    new_username TEXT;
    account_id_value BIGINT;
BEGIN
    -- Only process if email_id OR phone_id actually changed
    IF (OLD.email_id IS DISTINCT FROM NEW.email_id) OR
       (OLD.phone_id IS DISTINCT FROM NEW.phone_id) THEN

        -- Get account ID for this customer
        SELECT id INTO account_id_value
        FROM customer_accounts
        WHERE customer_id = NEW.id;

        -- Only proceed if account exists
        IF FOUND THEN

            -- Get new preferred username using view (handles email/phone priority automatically)
            SELECT preferred_username INTO new_username
            FROM customer_contact_lookup
            WHERE customer_id = NEW.id;

            -- Update username if we have contact info
            IF new_username IS NOT NULL THEN
                UPDATE customer_accounts
                SET username = new_username,
                    last_modified_date = CURRENT_TIMESTAMP
                WHERE id = account_id_value;
            END IF;

        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;