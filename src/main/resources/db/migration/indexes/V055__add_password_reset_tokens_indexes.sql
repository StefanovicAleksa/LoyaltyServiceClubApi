-- V055__add_password_reset_tokens_indexes.sql
-- Indexes for password_reset_tokens table

-- Foreign key index
CREATE INDEX idx_password_reset_tokens_customer_account_id ON password_reset_tokens(customer_account_id);

-- Token validation composite index (for efficient validation queries)
CREATE INDEX idx_password_reset_active ON password_reset_tokens(used, expires_at);

-- Expiration cleanup index
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at);