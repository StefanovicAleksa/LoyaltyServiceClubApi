-- V252__add_verification_status_triggers.sql
-- Triggers to update customer account verification status when email/phone verification changes

-- Trigger for email verification status changes
CREATE TRIGGER update_account_status_on_email_verification
    AFTER UPDATE ON customer_emails
    FOR EACH ROW
    WHEN (OLD.is_verified IS DISTINCT FROM NEW.is_verified)
EXECUTE FUNCTION update_account_verification_on_email_status_change();

-- Trigger for phone verification status changes
CREATE TRIGGER update_account_status_on_phone_verification
    AFTER UPDATE ON customer_phones
    FOR EACH ROW
    WHEN (OLD.is_verified IS DISTINCT FROM NEW.is_verified)
EXECUTE FUNCTION update_account_verification_on_phone_status_change();