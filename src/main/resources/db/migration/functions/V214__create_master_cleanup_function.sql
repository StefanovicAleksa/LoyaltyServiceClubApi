-- V214__update_master_cleanup_function_with_reset_tokens.sql
-- Updates the master cleanup function to include the new password reset token cleanup.

CREATE OR REPLACE FUNCTION run_all_cleanup_jobs()
    RETURNS VOID AS $$
DECLARE
    start_time TIMESTAMPTZ;
    end_time TIMESTAMPTZ;
    execution_time_ms INTEGER;
    error_message TEXT;
    total_errors INTEGER := 0;
    jobs_to_run INTEGER := 4; -- Updated to 4 jobs
BEGIN
    start_time := CURRENT_TIMESTAMP;

    RAISE NOTICE 'Starting all database cleanup jobs at %', start_time;

    -- Run OTP token cleanup
    BEGIN
        PERFORM cleanup_otp_tokens();
        RAISE NOTICE '✅ OTP token cleanup completed successfully';
    EXCEPTION WHEN OTHERS THEN
        total_errors := total_errors + 1;
        error_message := SQLERRM;
        RAISE NOTICE '❌ OTP token cleanup failed: %', error_message;
    END;

    -- NEW: Run password reset token cleanup
    BEGIN
        PERFORM cleanup_password_reset_tokens();
        RAISE NOTICE '✅ Password reset token cleanup completed successfully';
    EXCEPTION WHEN OTHERS THEN
        total_errors := total_errors + 1;
        error_message := SQLERRM;
        RAISE NOTICE '❌ Password reset token cleanup failed: %', error_message;
    END;

    -- Run account status audit cleanup
    BEGIN
        PERFORM cleanup_account_status_audit();
        RAISE NOTICE '✅ Account status audit cleanup completed successfully';
    EXCEPTION WHEN OTHERS THEN
        total_errors := total_errors + 1;
        error_message := SQLERRM;
        RAISE NOTICE '❌ Account status audit cleanup failed: %', error_message;
    END;

    -- Run unverified account cleanup
    BEGIN
        PERFORM cleanup_unverified_accounts();
        RAISE NOTICE '✅ Unverified account cleanup completed successfully';
    EXCEPTION WHEN OTHERS THEN
        total_errors := total_errors + 1;
        error_message := SQLERRM;
        RAISE NOTICE '❌ Unverified account cleanup failed: %', error_message;
    END;

    -- Calculate total execution time
    end_time := CURRENT_TIMESTAMP;
    execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

    -- Log master job completion
    IF total_errors = 0 THEN
        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, execution_time_ms
        ) VALUES (
                     'run_all_cleanup_jobs', CURRENT_DATE, TRUE, jobs_to_run, execution_time_ms
                 );

        RAISE NOTICE '🎉 All database cleanup jobs completed successfully in %.3f seconds',
            execution_time_ms / 1000.0;
    ELSE
        INSERT INTO job_execution_audit (
            job_name, execution_date, success, records_processed, error_message, execution_time_ms
        ) VALUES (
                     'run_all_cleanup_jobs', CURRENT_DATE, FALSE, jobs_to_run - total_errors,
                     format('%s cleanup job(s) failed', total_errors), execution_time_ms
                 );

        RAISE NOTICE '⚠️  Database cleanup jobs completed with % error(s) in %.3f seconds',
            total_errors, execution_time_ms / 1000.0;
    END IF;

END;
$$ LANGUAGE plpgsql;
