-- V254__add_account_status_audit_trigger.sql
-- Trigger to automatically audit account status changes

CREATE TRIGGER audit_account_status_changes
    AFTER UPDATE ON customer_accounts
    FOR EACH ROW
    WHEN (OLD.activity_status IS DISTINCT FROM NEW.activity_status)
EXECUTE FUNCTION audit_account_status_change();