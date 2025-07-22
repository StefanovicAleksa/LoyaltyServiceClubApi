-- V208__update_mark_inactive_accounts_function_fixed.sql
-- Updated inactivity function with proper variable scoping

CREATE OR REPLACE FUNCTION mark_inactive_accounts_batched()
    RETURNS VOID AS $$
DECLARE
    -- Configuration variables
    inactivity_days INTEGER;
    batch_size INTEGER;

    -- Processing variables
    total_processed INTEGER := 0;
    batch_count INTEGER := 0;
    current_batch_size INTEGER;

    -- Timing variables
    start_time TIMESTAMPTZ;
    end_time TIMESTAMPTZ;
    execution_time_ms INTEGER;

    -- Error handling
    error_occurred BOOLEAN := FALSE;
    error_message TEXT;

    -- Query string for dynamic cursor
    cursor_query TEXT;
    account_record RECORD;
BEGIN
    -- Record start time
    start_time := CURRENT_TIMESTAMP;

    BEGIN
        -- Read configuration values FIRST
        SELECT value::INTEGER INTO inactivity_days
        FROM business_config
        WHERE key = 'account_inactivity_days';

        SELECT value::INTEGER INTO batch_size
        FROM business_config
        WHERE key = 'inactivity_batch_size';

        -- Validate configuration
        IF inactivity_days IS NULL THEN
            RAISE EXCEPTION 'Configuration missing: account_inactivity_days';
        END IF;

        IF batch_size IS NULL THEN
            RAISE EXCEPTION 'Configuration missing: inactivity_batch_size';
        END IF;

        IF inactivity_days <= 0 THEN
            RAISE EXCEPTION 'Invalid configuration: account_inactivity_days must be > 0, got %', inactivity_days;
        END IF;

        IF batch_size <= 0 THEN
            RAISE EXCEPTION 'Invalid configuration: inactivity_batch_size must be > 0, got %', batch_size;
        END IF;

        RAISE NOTICE 'Starting account inactivity job: inactivity_days=%, batch_size=%',
            inactivity_days, batch_size;

        -- Build dynamic query with loaded config values
        cursor_query := format('
            SELECT account_id, activity_status
            FROM account_activity_data
            WHERE activity_status = ''ACTIVE''
              AND has_logged_in = true
              AND days_since_login > %s
            ORDER BY account_id
            FOR UPDATE SKIP LOCKED',
                               inactivity_days
                        );

        -- Process accounts in batches using dynamic cursor
        current_batch_size := 0;

        FOR account_record IN EXECUTE cursor_query
            LOOP
                -- Mark account as inactive
                UPDATE customer_accounts
                SET activity_status = 'INACTIVE'::customer_account_activity_status_enum,
                    last_modified_date = CURRENT_TIMESTAMP
                WHERE id = account_record.account_id;

                current_batch_size := current_batch_size + 1;
                total_processed := total_processed + 1;

                -- Process in batches
                IF current_batch_size >= batch_size THEN
                    batch_count := batch_count + 1;
                    RAISE NOTICE 'Completed batch %: % accounts processed (total: %)',
                        batch_count, current_batch_size, total_processed;
                    current_batch_size := 0;
                END IF;
            END LOOP;

        -- Handle final partial batch
        IF current_batch_size > 0 THEN
            batch_count := batch_count + 1;
            RAISE NOTICE 'Completed final batch %: % accounts processed (total: %)',
                batch_count, current_batch_size, total_processed;
        END IF;

        -- Calculate execution time
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        -- Log successful completion
        INSERT INTO job_execution_audit (
            job_name,
            execution_date,
            success,
            records_processed,
            execution_time_ms
        ) VALUES (
                     'mark_inactive_accounts',
                     CURRENT_DATE,
                     TRUE,
                     total_processed,
                     execution_time_ms
                 );

        RAISE NOTICE 'Account inactivity job completed successfully: % accounts processed in % batches (%.3f seconds)',
            total_processed, batch_count, execution_time_ms / 1000.0;

    EXCEPTION WHEN OTHERS THEN
        -- Handle errors
        error_occurred := TRUE;
        error_message := SQLERRM;

        -- Calculate execution time even for failed runs
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        -- Log failed execution
        INSERT INTO job_execution_audit (
            job_name,
            execution_date,
            success,
            records_processed,
            error_message,
            execution_time_ms
        ) VALUES (
                     'mark_inactive_accounts',
                     CURRENT_DATE,
                     FALSE,
                     total_processed,
                     error_message,
                     execution_time_ms
                 );

        RAISE NOTICE 'Account inactivity job failed: % (processed % accounts before failure)',
            error_message, total_processed;

        -- Re-raise the exception
        RAISE;
    END;
END;
$$ LANGUAGE plpgsql;