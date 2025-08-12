-- V302__schedule_cleanup_jobs.sql
-- Schedule database cleanup jobs using pg_cron.
-- This script is idempotent and ensures the latest function versions are used.

DO
$$
    BEGIN
        -- Unschedule the main cleanup job if it exists to avoid conflicts
        IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'database-cleanup-main') THEN
            PERFORM cron.unschedule('database-cleanup-main');
            RAISE NOTICE 'Unscheduled existing database-cleanup-main job.';
        END IF;

        -- Unschedule the job audit cleanup job if it exists
        IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'job-audit-cleanup') THEN
            PERFORM cron.unschedule('job-audit-cleanup');
            RAISE NOTICE 'Unscheduled existing job-audit-cleanup job.';
        END IF;
    END
$$;

-- Re-schedule the main cleanup job to run daily at 3:00 AM.
-- This will now execute the updated master function which includes password reset token cleanup.
SELECT cron.schedule(
               'database-cleanup-main',
               '0 3 * * *', -- 3:00 AM daily
               'SELECT run_all_cleanup_jobs();'
       );

-- Re-schedule the job execution audit cleanup to run weekly on Sundays at 4:00 AM.
-- This is separate to avoid circular logging issues.
SELECT cron.schedule(
               'job-audit-cleanup',
               '0 4 * * 0', -- 4:00 AM every Sunday
               'SELECT cleanup_job_execution_audit();'
       );
