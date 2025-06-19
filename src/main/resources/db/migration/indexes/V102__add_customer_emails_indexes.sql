-- V102__add_customer_emails_indexes.sql
-- Indexes for customer_emails table

-- Verification status index for admin/marketing queries
CREATE INDEX idx_customer_emails_verified ON customer_emails(is_verified);