-- V057__create_business_config_table.sql
-- Business configuration values table

CREATE TABLE business_config
(
    key                VARCHAR(100) PRIMARY KEY,
    value              TEXT         NOT NULL,
    description        TEXT,

    -- spring audit columns
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);