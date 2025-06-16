-- V056__add_persistent_logins_indexes.sql
-- Indexes for persistent_logins table

-- Foreign key index for easier cleanup when customer accounts are deleted
CREATE INDEX idx_persistent_logins_customer_account_id ON persistent_logins(customer_account_id);