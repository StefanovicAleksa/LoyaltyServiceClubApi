-- V209__create_otp_tokens_cleanup_function.sql
-- OTP tokens cleanup function (replaces cleanup_password_reset_tokens)

CREATE OR REPLACE FUNCTION cleanup_otp_tokens()
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
        FROM business_config WHERE key = 'otp_token_cleanup_days';

        SELECT value::INTEGER INTO batch_size
        FROM business_config WHERE key = 'cleanup_batch_size';

        IF cleanup_days IS NULL OR batch_size IS NULL THEN
            RAISE EXCEPTION 'Missing cleanup configuration: otp_token_cleanup_days or cleanup_batch_size';
        END IF;

        RAISE NOTICE 'Starting OTP tokens cleanup: cleanup_days=%, batch_size=%',
            cleanup_days, batch_size;

        -- Delete expired and used OTP tokens in batches
        LOOP
            DELETE FROM otp_tokens
            WHERE id IN (
                SELECT id FROM otp_tokens
                WHERE created_date < CURRENT_TIMESTAMP - (cleanup_days || ' days')::INTERVAL
                LIMIT batch_size
            );

            GET DIAGNOSTICS current_batch_deleted = ROW_COUNT;
            total_deleted := total_deleted + current_batch_deleted;

            EXIT WHEN current_batch_deleted = 0;

            RAISE NOTICE 'Deleted % OTP tokens (total: %)',
                current_batch_deleted, total_deleted;
        END LOOP;

        -- Log success
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, execution_time_ms
        ) VALUES (
                     'cleanup_otp_tokens', CURRENT_DATE, TRUE, total_deleted, execution_time_ms
                 );

        RAISE NOTICE 'OTP tokens cleanup completed: % tokens deleted in %.3f seconds',
            total_deleted, execution_time_ms / 1000.0;

    EXCEPTION WHEN OTHERS THEN
        error_message := SQLERRM;
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, error_message, execution_time_ms
        ) VALUES (
                     'cleanup_otp_tokens', CURRENT_DATE, FALSE, total_deleted, error_message, execution_time_ms
                 );

        RAISE NOTICE 'OTP tokens cleanup failed: % (processed % tokens before failure)',
            error_message, total_deleted;
        RAISE;
    END;
END;
$$ LANGUAGE plpgsql;