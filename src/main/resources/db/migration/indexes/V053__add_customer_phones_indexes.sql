-- V053__add_customer_phones_indexes.sql
-- Indexes for customer_phones table

-- Verification status index for admin/marketing queries
CREATE INDEX idx_customer_phones_verified ON customer_phones(is_verified);