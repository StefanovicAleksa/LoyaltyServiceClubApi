-- V104__add_customer_accounts_indexes.sql
-- Indexes for customer_accounts table

-- Foreign key index (critical for JOINs)
CREATE INDEX idx_customer_accounts_customer_id ON customer_accounts(customer_id);

-- Account status indexes for authentication checks
CREATE INDEX idx_customer_accounts_verification_status ON customer_accounts(verification_status);
CREATE INDEX idx_customer_accounts_activity_status ON customer_accounts(activity_status);

-- Last login index for inactivity tracking (triggers/cleanup jobs)
CREATE INDEX idx_customer_accounts_last_login ON customer_accounts(last_login_at);

-- Composite index for account status queries (marketing/analytics)
CREATE INDEX idx_customer_accounts_status_composite ON customer_accounts(activity_status, verification_status);