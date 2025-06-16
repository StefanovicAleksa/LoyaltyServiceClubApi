-- V015__create_password_reset_tokens_table.sql
-- Password reset tokens for forgot password functionality

CREATE TABLE password_reset_tokens
(
    id                  BIGSERIAL PRIMARY KEY,
    customer_account_id BIGINT       NOT NULL,
    token               VARCHAR(255) NOT NULL UNIQUE,
    expires_at          TIMESTAMPTZ  NOT NULL,
    used                BOOLEAN      NOT NULL DEFAULT false,

    -- spring audit columns
    created_date        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_customer_account
        FOREIGN KEY (customer_account_id)
            REFERENCES customer_accounts (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);