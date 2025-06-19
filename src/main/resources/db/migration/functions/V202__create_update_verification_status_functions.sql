-- V202__create_update_verification_status_functions.sql
-- Functions to update customer account verification status on phone/email change

-- email verification change
CREATE OR REPLACE FUNCTION update_account_verification_on_email_status_change()
RETURNS TRIGGER AS $$
DECLARE
    account_id BIGINT;
    current_status customer_account_verification_status_enum;
    new_status customer_account_verification_status_enum;
BEGIN
    SELECT customer_account_id, verification_status
    INTO account_id, current_status
    FROM customer_verification_lookup
    WHERE email_id = NEW.id;

    -- if no account found exit
    IF current_status IS NULL THEN
         RETURN NEW;
    END IF;

    IF NEW.is_verified THEN
        CASE current_status
            WHEN 'UNVERIFIED' THEN
                new_status := 'EMAIL_VERIFIED';
            WHEN 'PHONE_VERIFIED' THEN
                new_status := 'FULLY_VERIFIED';
            ELSE
                RETURN NEW; -- Already EMAIL_VERIFIED/FULLY_VERIFIED, no change needed
        END CASE;
    ELSE
        CASE current_status
            WHEN 'EMAIL_VERIFIED' THEN
                new_status := 'UNVERIFIED';
            WHEN 'FULLY_VERIFIED' THEN
                new_status := 'PHONE_VERIFIED';
            ELSE
                RETURN NEW; -- Already UNVERIFIED/PHONE_VERIFIED, no change needed
        END CASE;
    END IF;

    -- Update account status for the required account
    UPDATE customer_accounts ca
    SET verification_status = new_status
    WHERE ca.id = account_id;

    -- exit
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- phone verification change
CREATE OR REPLACE FUNCTION update_account_verification_on_phone_status_change()
    RETURNS TRIGGER AS $$
DECLARE
    account_id BIGINT;
    current_status customer_account_verification_status_enum;
    new_status customer_account_verification_status_enum;
BEGIN
    SELECT customer_account_id, verification_status
    INTO account_id, current_status
    FROM customer_verification_lookup
    WHERE phone_id = NEW.id;

    -- if no account found exit
    IF current_status IS NULL THEN
        RETURN NEW;
    END IF;

    IF NEW.is_verified THEN
        CASE current_status
            WHEN 'UNVERIFIED' THEN
                new_status := 'PHONE_VERIFIED';
            WHEN 'EMAIL_VERIFIED' THEN
                new_status := 'FULLY_VERIFIED';
            ELSE
                RETURN NEW; -- Already PHONE_VERIFIED/FULLY_VERIFIED, no change needed
            END CASE;
    ELSE
        CASE current_status
            WHEN 'PHONE_VERIFIED' THEN
                new_status := 'UNVERIFIED';
            WHEN 'FULLY_VERIFIED' THEN
                new_status := 'EMAIL_VERIFIED';
            ELSE
                RETURN NEW; -- Already UNVERIFIED/EMAIL_VERIFIED, no change needed
            END CASE;
    END IF;

    -- Update account status for the required account
    UPDATE customer_accounts ca
    SET verification_status = new_status
    WHERE ca.id = account_id;

    -- exit
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;