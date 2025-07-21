-- V351__seed_business_config_data.sql
-- Default business configuration values

INSERT INTO business_config (key, value, description)
VALUES ('account_inactivity_days', '60', 'Number of days after last login before marking account as inactive'),
       ('inactivity_batch_size', '1000', 'Batch size for processing inactive accounts');