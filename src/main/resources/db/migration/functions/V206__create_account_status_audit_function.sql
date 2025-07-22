-- V206__create_account_status_audit_function.sql
-- Account status change audit function

CREATE OR REPLACE FUNCTION create_account_status_audit_record()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Create audit record for activity status changes
    INSERT INTO account_status_audit (account_id, old_status, new_status)
    VALUES (NEW.id, OLD.activity_status, NEW.activity_status);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;