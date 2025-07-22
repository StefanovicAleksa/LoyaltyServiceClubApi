-- V255__create_account_status_audit_trigger.sql
-- Account status change audit trigger

CREATE TRIGGER audit_account_status_changes
    AFTER UPDATE ON customer_accounts
    FOR EACH ROW
    WHEN (OLD.activity_status IS DISTINCT FROM NEW.activity_status)
EXECUTE FUNCTION create_account_status_audit_record();