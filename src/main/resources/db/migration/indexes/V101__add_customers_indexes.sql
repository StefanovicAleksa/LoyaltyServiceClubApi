-- V101__add_customers_indexes.sql
-- Indexes for customers table

-- Foreign key indexes (critical for JOIN performance)
CREATE INDEX idx_customers_email_id ON customers(email_id);
CREATE INDEX idx_customers_phone_id ON customers(phone_id);