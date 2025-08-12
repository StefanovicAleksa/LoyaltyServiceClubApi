-- V256__create_contact_verification_trigger.sql
-- Creates the trigger on the otp_tokens table to automate contact verification.

CREATE TRIGGER update_contact_verification_on_otp_use
    AFTER UPDATE ON otp_tokens
    FOR EACH ROW
    -- This WHEN clause is a crucial optimization. The trigger only fires
    -- when the 'used_at' field transitions from NULL to a non-NULL value,
    -- which signifies a successful OTP verification.
    WHEN (OLD.used_at IS NULL AND NEW.used_at IS NOT NULL)
EXECUTE FUNCTION handle_contact_verification_on_otp_use();