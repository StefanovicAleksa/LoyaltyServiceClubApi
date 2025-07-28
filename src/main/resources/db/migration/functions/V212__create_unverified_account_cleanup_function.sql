-- V212__create_unverified_account_cleanup_function.sql
-- Unverified account cleanup function (most important for business value)

CREATE OR REPLACE FUNCTION cleanup_unverified_accounts()
    RETURNS VOID AS $$
DECLARE
    cleanup_days INTEGER;
    batch_size INTEGER;
    total_deleted INTEGER := 0;
    current_batch_deleted INTEGER;
    start_time TIMESTAMPTZ;
    end_time TIMESTAMPTZ;
    execution_time_ms INTEGER;
    error_message TEXT;
BEGIN
    start_time := CURRENT_TIMESTAMP;

    BEGIN
        -- Get configuration
        SELECT value::INTEGER INTO cleanup_days
        FROM business_config WHERE key = 'unverified_account_cleanup_days';

        SELECT value::INTEGER INTO batch_size
        FROM business_config WHERE key = 'cleanup_batch_size';

        IF cleanup_days IS NULL OR batch_size IS NULL THEN
            RAISE EXCEPTION 'Missing cleanup configuration: unverified_account_cleanup_days or cleanup_batch_size';
        END IF;

        RAISE NOTICE 'Starting unverified account cleanup: cleanup_days=%, batch_size=%',
            cleanup_days, batch_size;

        -- Delete unverified accounts that never logged in, in batches
        -- This will cascade delete the customer and associated email/phone records
        LOOP
            DELETE FROM customer_accounts
            WHERE id IN (
                SELECT ca.id
                FROM customer_accounts ca
                WHERE ca.verification_status = 'UNVERIFIED'
                  AND ca.last_login_at IS NULL
                  AND ca.created_date < CURRENT_TIMESTAMP - (cleanup_days || ' days')::INTERVAL
                LIMIT batch_size
                    FOR UPDATE SKIP LOCKED
            );

            GET DIAGNOSTICS current_batch_deleted = ROW_COUNT;
            total_deleted := total_deleted + current_batch_deleted;

            EXIT WHEN current_batch_deleted = 0;

            RAISE NOTICE 'Deleted % unverified accounts (total: %)',
                current_batch_deleted, total_deleted;
        END LOOP;

        -- Log success
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, execution_time_ms
        ) VALUES (
                     'cleanup_unverified_accounts', CURRENT_DATE, TRUE, total_deleted, execution_time_ms
                 );

        RAISE NOTICE 'Unverified account cleanup completed: % accounts deleted in %.3f seconds',
            total_deleted, execution_time_ms / 1000.0;

    EXCEPTION WHEN OTHERS THEN
        error_message := SQLERRM;
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, error_message, execution_time_ms
        ) VALUES (
                     'cleanup_unverified_accounts', CURRENT_DATE, FALSE, total_deleted, error_message, execution_time_ms
                 );

        RAISE NOTICE 'Unverified account cleanup failed: % (processed % accounts before failure)',
            error_message, total_deleted;
        RAISE;
    END;
END;
$$ LANGUAGE plpgsql;