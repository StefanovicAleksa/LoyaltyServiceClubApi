-- V253__create_customer_contact_change_trigger.sql
-- Customer contact change trigger (handles both email and phone changes)

CREATE TRIGGER update_username_on_contact_change
    AFTER UPDATE ON customers
    FOR EACH ROW
    WHEN (OLD.email_id IS DISTINCT FROM NEW.email_id OR
          OLD.phone_id IS DISTINCT FROM NEW.phone_id)
EXECUTE FUNCTION update_username_on_contact_change();