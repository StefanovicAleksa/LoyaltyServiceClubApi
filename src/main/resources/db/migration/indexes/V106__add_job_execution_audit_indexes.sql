-- V106__add_job_execution_audit_indexes.sql
-- Indexes for job_execution_audit table

CREATE INDEX idx_job_execution_audit_job_date ON job_execution_audit(job_name, execution_date);
CREATE INDEX idx_job_execution_audit_success ON job_execution_audit(success);