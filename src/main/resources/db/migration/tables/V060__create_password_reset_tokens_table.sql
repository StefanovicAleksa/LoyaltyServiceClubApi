-- V060__create_password_reset_tokens_table.sql
-- Single-use, high-entropy tokens for authorizing the final password reset step

CREATE TABLE password_reset_tokens
(
    id                  BIGSERIAL PRIMARY KEY,
    token               VARCHAR(36)  NOT NULL UNIQUE, -- For UUIDs
    customer_account_id BIGINT       NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,        -- Short-lived (e.g., 10 minutes)
    used_at             TIMESTAMPTZ,                  -- When the token was successfully used (null = unused)

    -- spring audit columns
    created_date        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to the customer account
    CONSTRAINT fk_reset_token_customer_account
        FOREIGN KEY (customer_account_id)
            REFERENCES customer_accounts (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE, -- If the account is deleted, its reset tokens are irrelevant

    -- Logical time constraints
    CONSTRAINT chk_reset_token_expiry_after_creation
        CHECK (expires_at > created_date),

    CONSTRAINT chk_reset_token_used_after_creation
        CHECK (used_at IS NULL OR used_at > created_date)
);
