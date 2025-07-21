-- V301__schedule_account_inactivity_job.sql
-- Schedule account inactivity job using pg_cron
-- NOTE: Requires pg_cron extension to be already installed by superuser

-- Schedule the account inactivity job to run daily at 2:00 AM
SELECT cron.schedule(
               'account-inactivity-check',  -- Job name
               '0 2 * * *',                -- Cron expression: 2 AM daily
               'SELECT mark_inactive_accounts_batched();'  -- SQL to execute
       );