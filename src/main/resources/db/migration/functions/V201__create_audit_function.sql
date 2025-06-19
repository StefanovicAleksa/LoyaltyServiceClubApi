-- V201__create_audit_function.sql
-- Reusable audit function for created_date and last_modified_date

CREATE OR REPLACE FUNCTION update_audit_fields()
    RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_date = CURRENT_TIMESTAMP;
        NEW.last_modified_date = CURRENT_TIMESTAMP;
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        NEW.last_modified_date = CURRENT_TIMESTAMP;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;