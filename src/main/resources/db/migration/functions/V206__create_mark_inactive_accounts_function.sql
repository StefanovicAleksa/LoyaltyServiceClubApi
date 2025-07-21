-- V205__create_mark_inactive_accounts_function.sql
-- Function to mark inactive accounts in batches with full logging

CREATE OR REPLACE FUNCTION mark_inactive_accounts_batched()
    RETURNS VOID AS $$
DECLARE
    -- Configuration variables
    inactivity_days INTEGER;
    batch_size INTEGER;
    cutoff_date TIMESTAMPTZ;

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

    -- Cursor for batch processing
    account_cursor CURSOR FOR
        SELECT id, activity_status
        FROM customer_accounts
        WHERE activity_status = 'ACTIVE'::customer_account_activity_status_enum
          AND last_login_at < cutoff_date
        ORDER BY id
            FOR UPDATE SKIP LOCKED; -- Prevents blocking if multiple processes run

    account_record RECORD;
BEGIN
    -- Record start time
    start_time := CURRENT_TIMESTAMP;

    BEGIN
        -- Read configuration values
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

        -- Calculate cutoff date
        cutoff_date := CURRENT_TIMESTAMP - (inactivity_days || ' days')::INTERVAL;

        RAISE NOTICE 'Starting account inactivity job: inactivity_days=%, batch_size=%, cutoff_date=%',
            inactivity_days, batch_size, cutoff_date;

        -- Process accounts in batches
        OPEN account_cursor;

        LOOP
            current_batch_size := 0;

            -- Process one batch
            LOOP
                FETCH account_cursor INTO account_record;
                EXIT WHEN NOT FOUND OR current_batch_size >= batch_size;

                -- Mark account as inactive
                UPDATE customer_accounts
                SET activity_status = 'INACTIVE'::customer_account_activity_status_enum,
                    last_modified_date = CURRENT_TIMESTAMP
                WHERE id = account_record.id;

                current_batch_size := current_batch_size + 1;
                total_processed := total_processed + 1;
            END LOOP;

            -- Exit if no more accounts to process
            EXIT WHEN current_batch_size = 0;

            batch_count := batch_count + 1;

            RAISE NOTICE 'Completed batch %: % accounts processed (total: %)',
                batch_count, current_batch_size, total_processed;

            -- Optional: Add small delay between batches to reduce system load
            -- PERFORM pg_sleep(0.1);
        END LOOP;

        CLOSE account_cursor;

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

        -- Ensure cursor is closed (PostgreSQL doesn't have %ISOPEN)
        BEGIN
            CLOSE account_cursor;
        EXCEPTION WHEN OTHERS THEN
            -- Cursor might already be closed, ignore error
            NULL;
        END;

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