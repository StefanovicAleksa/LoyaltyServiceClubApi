-- V059__create_account_status_audit_table.sql
-- Account status change audit trail

CREATE TABLE account_status_audit
(
    id           BIGSERIAL PRIMARY KEY,
    account_id   BIGINT                                      NOT NULL,
    old_status   customer_account_activity_status_enum       NOT NULL,
    new_status   customer_account_activity_status_enum       NOT NULL,

    -- spring audit columns
    created_date TIMESTAMPTZ                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_status_audit_account
        FOREIGN KEY (account_id)
            REFERENCES customer_accounts (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);