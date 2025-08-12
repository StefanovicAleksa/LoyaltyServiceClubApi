-- V215__create_otp_verification_trigger_function.sql
-- This function is called by a trigger on the otp_tokens table.
-- When an OTP is successfully used for verification purposes, this function
-- updates the corresponding contact method (email or phone) to mark it as verified.

CREATE OR REPLACE FUNCTION handle_contact_verification_on_otp_use()
    RETURNS TRIGGER AS $$
BEGIN
    -- This function assumes the trigger's WHEN clause has already checked that
    -- used_at was changed from NULL to a non-NULL value.

    -- If the OTP was for verifying an email, update the customer_emails table.
    IF NEW.purpose = 'EMAIL_VERIFICATION' AND NEW.customer_email_id IS NOT NULL THEN
        UPDATE customer_emails
        SET is_verified = true,
            last_modified_date = CURRENT_TIMESTAMP
        WHERE id = NEW.customer_email_id
          -- Optimization: Only perform the update if it's not already verified.
          -- This prevents the subsequent trigger on customer_emails from firing unnecessarily.
          AND is_verified = false;

        -- If the OTP was for verifying a phone, update the customer_phones table.
    ELSIF NEW.purpose = 'PHONE_VERIFICATION' AND NEW.customer_phone_id IS NOT NULL THEN
        UPDATE customer_phones
        SET is_verified = true,
            last_modified_date = CURRENT_TIMESTAMP
        WHERE id = NEW.customer_phone_id
          -- Optimization: Only perform the update if it's not already verified.
          AND is_verified = false;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;