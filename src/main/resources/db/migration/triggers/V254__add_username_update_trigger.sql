-- V254__add_username_update_trigger.sql
-- Trigger to update username when customer's email_id changes

CREATE TRIGGER update_username_on_customer_email_change
    AFTER UPDATE ON customers
    FOR EACH ROW
    WHEN (OLD.email_id IS DISTINCT FROM NEW.email_id)
EXECUTE FUNCTION update_username_on_email_change();