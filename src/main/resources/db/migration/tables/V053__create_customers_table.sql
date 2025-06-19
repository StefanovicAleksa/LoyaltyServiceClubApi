-- V053__create_customers_table.sql
-- Main customer information table

CREATE TABLE customers
(
    id                 BIGSERIAL PRIMARY KEY,
    first_name         VARCHAR(20) NOT NULL,
    last_name          VARCHAR(30) NOT NULL,
    email_id           BIGINT UNIQUE,
    phone_id           BIGINT UNIQUE,

    -- spring audit columns
    created_date       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_email
        FOREIGN KEY (email_id)
            REFERENCES customer_emails (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL,

    CONSTRAINT fk_customer_phone
        FOREIGN KEY (phone_id)
            REFERENCES customer_phones (id)
            ON UPDATE CASCADE
            ON DELETE SET NULL
);