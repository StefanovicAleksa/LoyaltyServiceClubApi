-- V210__create_job_execution_audit_cleanup_function.sql
-- Job execution audit cleanup function

CREATE OR REPLACE FUNCTION cleanup_job_execution_audit()
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
        FROM business_config WHERE key = 'job_execution_audit_cleanup_days';

        SELECT value::INTEGER INTO batch_size
        FROM business_config WHERE key = 'cleanup_batch_size';

        IF cleanup_days IS NULL OR batch_size IS NULL THEN
            RAISE EXCEPTION 'Missing cleanup configuration: job_execution_audit_cleanup_days or cleanup_batch_size';
        END IF;

        RAISE NOTICE 'Starting job execution audit cleanup: cleanup_days=%, batch_size=%',
            cleanup_days, batch_size;

        -- Delete in batches to avoid long locks
        LOOP
            DELETE FROM job_execution_audit
            WHERE id IN (
                SELECT id FROM job_execution_audit
                WHERE created_date < CURRENT_TIMESTAMP - (cleanup_days || ' days')::INTERVAL
                LIMIT batch_size
            );

            GET DIAGNOSTICS current_batch_deleted = ROW_COUNT;
            total_deleted := total_deleted + current_batch_deleted;

            EXIT WHEN current_batch_deleted = 0;

            RAISE NOTICE 'Deleted % job audit records (total: %)',
                current_batch_deleted, total_deleted;
        END LOOP;

        -- Note: We don't log this cleanup to job_execution_audit to avoid circular logging
        -- Instead, we log to PostgreSQL logs via RAISE NOTICE
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        RAISE NOTICE 'Job execution audit cleanup completed: % records deleted in %.3f seconds',
            total_deleted, execution_time_ms / 1000.0;

    EXCEPTION WHEN OTHERS THEN
        error_message := SQLERRM;
        end_time := CURRENT_TIMESTAMP;
        execution_time_ms := EXTRACT(EPOCH FROM (end_time - start_time)) * 1000;

        RAISE NOTICE 'Job execution audit cleanup failed: % (processed % records before failure in %.3f seconds)',
            error_message, total_deleted, execution_time_ms / 1000.0;
        RAISE;
    END;
END;
$$ LANGUAGE plpgsql;