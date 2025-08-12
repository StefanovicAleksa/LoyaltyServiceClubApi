-- V213__create_password_reset_tokens_cleanup_function.sql
-- Database function to periodically clean up the password_reset_tokens table.

CREATE OR REPLACE FUNCTION cleanup_password_reset_tokens()
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
        -- Get configuration values
        SELECT value::INTEGER INTO cleanup_days
        FROM business_config WHERE key = 'password_reset_token_cleanup_days';

        SELECT value::INTEGER INTO batch_size
        FROM business_config WHERE key = 'cleanup_batch_size';

        IF cleanup_days IS NULL OR batch_size IS NULL THEN
            RAISE EXCEPTION 'Missing cleanup configuration: password_reset_token_cleanup_days or cleanup_batch_size';
        END IF;

        RAISE NOTICE 'Starting password reset tokens cleanup: cleanup_days=%, batch_size=%',
            cleanup_days, batch_size;

        -- Delete old tokens in batches to avoid long-running transactions
        LOOP
            DELETE FROM password_reset_tokens
            WHERE id IN (
                SELECT id FROM password_reset_tokens
                WHERE created_date < CURRENT_TIMESTAMP - (cleanup_days || ' days')::INTERVAL
                LIMIT batch_size
            );

            GET DIAGNOSTICS current_batch_deleted = ROW_COUNT;
            total_deleted := total_deleted + current_batch_deleted;

            EXIT WHEN current_batch_deleted = 0;

            RAISE NOTICE 'Deleted % password reset tokens (total: %)',
                current_batch_deleted, total_deleted;
        END LOOP;

        -- Log successful execution
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, execution_time_ms
        ) VALUES (
                     'cleanup_password_reset_tokens', CURRENT_DATE, TRUE, total_deleted, execution_time_ms
                 );

        RAISE NOTICE 'Password reset tokens cleanup completed: % tokens deleted in %.3f seconds',
            total_deleted, execution_time_ms / 1000.0;

    EXCEPTION WHEN OTHERS THEN
        error_message := SQLERRM;
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        -- Log failed execution
        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, error_message, execution_time_ms
        ) VALUES (
                     'cleanup_password_reset_tokens', CURRENT_DATE, FALSE, total_deleted, error_message, execution_time_ms
                 );

        RAISE NOTICE 'Password reset tokens cleanup failed: % (processed % tokens before failure)',
            error_message, total_deleted;
        RAISE;
    END;
END;
$$ LANGUAGE plpgsql;
