-- V108__add_password_reset_tokens_indexes.sql
-- Indexes for the password_reset_tokens table

-- Index for quick lookups on the foreign key
CREATE INDEX idx_password_reset_tokens_customer_account_id ON password_reset_tokens(customer_account_id);

-- Index to help the cleanup job find expired tokens efficiently
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

-- Partial unique index to enforce that a user can only have ONE active (unused) password reset token at a time.
-- This is a key data integrity constraint for security and user experience.
CREATE UNIQUE INDEX uq_active_reset_token_per_user ON password_reset_tokens (customer_account_id) WHERE used_at IS NULL;
