-- V301__schedule_account_inactivity_job.sql
-- Schedule account inactivity job using pg_cron with updated function
-- NOTE: Requires pg_cron extension to be already installed by superuser

-- Remove any existing job with same name (in case of re-deployment)
SELECT cron.unschedule('account-inactivity-check');

-- Schedule the updated account inactivity job to run daily at 2:00 AM
SELECT cron.schedule(
               'account-inactivity-check',  -- Job name
               '0 2 * * *',                -- Cron expression: 2 AM daily
               'SELECT mark_inactive_accounts_batched();'  -- SQL to execute (uses updated function)
       );