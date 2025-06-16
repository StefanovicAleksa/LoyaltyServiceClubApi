-- V012__create_customer_emails_table.sql
-- Customer email addresses with verification status

CREATE TABLE customer_emails
(
    id                 BIGSERIAL PRIMARY KEY,
    email              VARCHAR(50) NOT NULL UNIQUE,
    is_verified        BOOLEAN     NOT NULL DEFAULT false,

    -- spring audit columns
    created_date       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);