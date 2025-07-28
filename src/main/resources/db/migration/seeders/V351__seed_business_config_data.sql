-- V351__seed_business_config_data.sql
-- Default business configuration values including cleanup configurations

INSERT INTO business_config (key, value, description)
VALUES
-- Account inactivity config
('account_inactivity_days', '60', 'Number of days after last login before marking account as inactive'),
('inactivity_batch_size', '1000', 'Batch size for processing inactive accounts'),

-- Aggressive cleanup configs
('password_reset_token_cleanup_days', '7', 'Days to keep password reset tokens after creation'),
('job_execution_audit_cleanup_days', '90', 'Days to keep job execution audit records'),
('account_status_audit_cleanup_days', '365', 'Days to keep account status change audit records'),

-- Customer account cleanup configs
('unverified_account_cleanup_days', '30', 'Days to keep unverified accounts that never logged in'),
('inactive_verified_account_cleanup_days', '730', 'Days to keep verified accounts with no recent activity (2 years)'),

-- Batch processing configs
('cleanup_batch_size', '500', 'Batch size for cleanup operations to avoid long locks');