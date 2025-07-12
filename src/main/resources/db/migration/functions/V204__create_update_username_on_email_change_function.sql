-- V204__create_update_username_on_email_change_function.sql
-- Function to update customer account username when email is added/changed

CREATE OR REPLACE FUNCTION update_username_on_email_change()
    RETURNS TRIGGER AS $$
DECLARE
    customer_email VARCHAR(50);
    account_id BIGINT;
BEGIN
    -- Only proceed if email_id has actually changed
    IF OLD.email_id IS DISTINCT FROM NEW.email_id THEN

        -- Get the customer account ID for this customer
        SELECT id INTO account_id
        FROM customer_accounts
        WHERE customer_id = NEW.id;

        -- If no account exists, nothing to update
        IF account_id IS NULL THEN
            RETURN NEW;
        END IF;

        -- If email was added (email_id changed from NULL to a value)
        IF NEW.email_id IS NOT NULL THEN
            -- Get the actual email address
            SELECT email INTO customer_email
            FROM customer_emails
            WHERE id = NEW.email_id;

            -- Update the username to the email address
            IF customer_email IS NOT NULL THEN
                UPDATE customer_accounts
                SET username = customer_email,
                    last_modified_date = CURRENT_TIMESTAMP
                WHERE id = account_id;
            END IF;

            -- If email was removed (email_id changed from a value to NULL)
        ELSIF OLD.email_id IS NOT NULL AND NEW.email_id IS NULL THEN
            -- Fall back to phone if available
            IF NEW.phone_id IS NOT NULL THEN
                DECLARE
                    customer_phone VARCHAR(13);
                BEGIN
                    SELECT phone INTO customer_phone
                    FROM customer_phones
                    WHERE id = NEW.phone_id;

                    IF customer_phone IS NOT NULL THEN
                        UPDATE customer_accounts
                        SET username = customer_phone,
                            last_modified_date = CURRENT_TIMESTAMP
                        WHERE id = account_id;
                    END IF;
                END;
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;