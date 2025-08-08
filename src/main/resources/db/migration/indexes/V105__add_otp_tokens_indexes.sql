-- V105__add_otp_tokens_indexes.sql
-- Indexes for otp_tokens table (replaces password_reset_tokens indexes)

-- Foreign key indexes for performance
CREATE INDEX idx_otp_tokens_customer_email_id ON otp_tokens(customer_email_id);
CREATE INDEX idx_otp_tokens_customer_phone_id ON otp_tokens(customer_phone_id);

-- Query optimization indexes
CREATE INDEX idx_otp_tokens_expires_at ON otp_tokens(expires_at);
CREATE INDEX idx_otp_tokens_purpose ON otp_tokens(purpose);
CREATE INDEX idx_otp_tokens_delivery_method ON otp_tokens(delivery_method);

-- Composite index for finding active OTPs (most common query)
CREATE INDEX idx_otp_tokens_active ON otp_tokens(used_at, expires_at) WHERE used_at IS NULL;

-- Composite index for cleanup jobs (created_date based cleanup)
CREATE INDEX idx_otp_tokens_cleanup ON otp_tokens(created_date, used_at);

-- Composite index for rate limiting queries (customer + time window)
CREATE INDEX idx_otp_tokens_rate_limiting_email ON otp_tokens(customer_email_id, created_date, purpose);
CREATE INDEX idx_otp_tokens_rate_limiting_phone ON otp_tokens(customer_phone_id, created_date, purpose);

-- Composite index for validation queries (common lookup pattern)
CREATE INDEX idx_otp_tokens_validation ON otp_tokens(otp_code, purpose, expires_at, used_at);