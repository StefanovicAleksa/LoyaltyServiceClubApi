CREATE OR REPLACE FUNCTION update_audit_fields()
    RETURNS TRIGGER AS
$$
DECLARE
    has_created_date BOOLEAN;
    has_last_modified_date BOOLEAN;
BEGIN
    -- Check if columns exist (done once per trigger execution)
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = TG_TABLE_SCHEMA
          AND table_name = TG_TABLE_NAME
          AND column_name = 'created_date'
    ) INTO has_created_date;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = TG_TABLE_SCHEMA
          AND table_name = TG_TABLE_NAME
          AND column_name = 'last_modified_date'
    ) INTO has_last_modified_date;

    IF TG_OP = 'INSERT' THEN
        -- Set created_date if column exists
        IF has_created_date THEN
            NEW.created_date = CURRENT_TIMESTAMP;
        END IF;

        -- Set last_modified_date if column exists
        IF has_last_modified_date THEN
            NEW.last_modified_date = CURRENT_TIMESTAMP;
        END IF;

        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        -- Only update last_modified_date if column exists
        IF has_last_modified_date THEN
            NEW.last_modified_date = CURRENT_TIMESTAMP;
        END IF;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;