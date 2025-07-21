-- V058__create_job_execution_audit_table.sql
-- Universal job execution audit table for monitoring scheduled jobs

CREATE TABLE job_execution_audit
(
    id                   BIGSERIAL PRIMARY KEY,
    job_name             VARCHAR(50)  NOT NULL,
    execution_date       DATE         NOT NULL,
    success              BOOLEAN      NOT NULL,
    records_processed    INTEGER      DEFAULT 0,
    error_message        TEXT,
    execution_time_ms    INTEGER,

    -- audit column
    created_date         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);