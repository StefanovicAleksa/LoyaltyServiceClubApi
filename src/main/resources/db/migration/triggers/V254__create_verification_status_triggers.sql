-- V254__create_verification_status_triggers.sql
-- Verification status update triggers for email and phone changes

-- Email verification trigger
CREATE TRIGGER update_account_status_on_email_verification
    AFTER UPDATE ON customer_emails
    FOR EACH ROW
    WHEN (OLD.is_verified IS DISTINCT FROM NEW.is_verified)
EXECUTE FUNCTION handle_email_verification_change();

-- Phone verification trigger
CREATE TRIGGER update_account_status_on_phone_verification
    AFTER UPDATE ON customer_phones
    FOR EACH ROW
    WHEN (OLD.is_verified IS DISTINCT FROM NEW.is_verified)
EXECUTE FUNCTION handle_phone_verification_change();