-- V302__schedule_cleanup_jobs.sql
-- Schedule database cleanup jobs using pg_cron

-- Remove any existing cleanup jobs (in case of re-deployment)
-- Use WHERE EXISTS to avoid errors if jobs don't exist
DO
$$
    BEGIN
        -- Unschedule database-cleanup-main if it exists
        IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'database-cleanup-main') THEN
            PERFORM cron.unschedule('database-cleanup-main');
        END IF;

        -- Unschedule job-audit-cleanup if it exists
        IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'job-audit-cleanup') THEN
            PERFORM cron.unschedule('job-audit-cleanup');
        END IF;
    END
$$;

-- Main cleanup job - runs daily at 3:00 AM (after account inactivity job at 2:00 AM)
-- Includes: password reset tokens, account status audit, unverified accounts
SELECT cron.schedule(
               'database-cleanup-main',
               '0 3 * * *', -- 3:00 AM daily
               'SELECT run_all_cleanup_jobs();'
       );

-- Job execution audit cleanup - runs weekly on Sundays at 4:00 AM
-- Separate schedule to avoid circular logging issues
SELECT cron.schedule(
               'job-audit-cleanup',
               '0 4 * * 0', -- 4:00 AM every Sunday
               'SELECT cleanup_job_execution_audit();'
       );