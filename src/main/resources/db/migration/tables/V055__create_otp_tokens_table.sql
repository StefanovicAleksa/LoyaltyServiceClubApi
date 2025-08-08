-- V055__create_otp_tokens_table.sql
-- Unified OTP tokens table for all verification purposes (replaces password_reset_tokens)

CREATE TABLE otp_tokens
(
    id                 BIGSERIAL PRIMARY KEY,
    customer_email_id  BIGINT,                           -- References customer_emails.id (nullable)
    customer_phone_id  BIGINT,                           -- References customer_phones.id (nullable)
    otp_code          VARCHAR(6)              NOT NULL,  -- The actual OTP code (6 digits)
    purpose           otp_purpose_enum        NOT NULL,  -- What this OTP is for
    delivery_method   otp_delivery_method_enum NOT NULL, -- How the OTP was delivered
    expires_at        TIMESTAMPTZ             NOT NULL,  -- When the OTP expires
    used_at           TIMESTAMPTZ,                       -- When the OTP was successfully used (null = unused)
    attempts_count    INTEGER                 DEFAULT 0, -- How many times verification was attempted
    max_attempts      INTEGER                 DEFAULT 3, -- Maximum allowed attempts

    -- Spring audit columns
    created_date      TIMESTAMPTZ             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMPTZ            NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints: exactly one of email_id OR phone_id must be set
    CONSTRAINT chk_otp_single_contact
        CHECK ((customer_email_id IS NOT NULL AND customer_phone_id IS NULL) OR
               (customer_email_id IS NULL AND customer_phone_id IS NOT NULL)),

    -- Constraint: delivery method must match contact type
    CONSTRAINT chk_otp_delivery_contact_match
        CHECK ((delivery_method = 'EMAIL' AND customer_email_id IS NOT NULL) OR
               (delivery_method = 'SMS' AND customer_phone_id IS NOT NULL)),

    -- Foreign key constraints
    CONSTRAINT fk_otp_customer_email
        FOREIGN KEY (customer_email_id)
            REFERENCES customer_emails (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_otp_customer_phone
        FOREIGN KEY (customer_phone_id)
            REFERENCES customer_phones (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);