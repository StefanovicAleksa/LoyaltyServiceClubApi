-- V205__create_account_status_audit_function.sql
-- Function to automatically audit account status changes

CREATE OR REPLACE FUNCTION audit_account_status_change()
    RETURNS TRIGGER AS $$
BEGIN
    -- Only audit when activity_status actually changes
    IF OLD.activity_status IS DISTINCT FROM NEW.activity_status THEN
        INSERT INTO account_status_audit (account_id, old_status, new_status)
        VALUES (NEW.id, OLD.activity_status, NEW.activity_status);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;