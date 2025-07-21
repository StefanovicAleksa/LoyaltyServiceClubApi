-- V107__add_account_status_audit_indexes.sql
-- Indexes for account_status_audit table

CREATE INDEX idx_account_status_audit_account_id ON account_status_audit(account_id);
CREATE INDEX idx_account_status_audit_new_status ON account_status_audit(new_status);
CREATE INDEX idx_account_status_audit_created_date ON account_status_audit(created_date);

-- Composite index for common queries (account status changes over time)
CREATE INDEX idx_account_status_audit_account_date ON account_status_audit(account_id, created_date);