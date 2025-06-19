-- V054__create_customer_accounts_table.sql
-- Customer login accounts with authentication credentials

CREATE TABLE customer_accounts
(
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(60) UNIQUE                        NOT NULL,
    password            VARCHAR(255)                              NOT NULL,
    activity_status     customer_account_activity_status_enum     NOT NULL DEFAULT 'ACTIVE',
    verification_status customer_account_verification_status_enum NOT NULL DEFAULT 'UNVERIFIED',
    last_login_at       TIMESTAMPTZ,
    customer_id         BIGINT UNIQUE                             NOT NULL,

    -- spring audit columns
    created_date        TIMESTAMPTZ                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMPTZ                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_account_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);