-- V011__create_customer_phones_table.sql
-- Customer phone numbers with verification status

CREATE TABLE customer_phones
(
    id                 BIGSERIAL PRIMARY KEY,
    phone              VARCHAR(12) NOT NULL UNIQUE,
    is_verified        BOOLEAN     NOT NULL DEFAULT false,

    -- spring audit columns
    created_date       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);