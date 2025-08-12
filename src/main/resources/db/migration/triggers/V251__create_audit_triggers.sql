-- V251__create_audit_triggers.sql
-- Audit field triggers for all tables with created_date and last_modified_date

-- Customer emails audit trigger
CREATE TRIGGER z_audit_customer_emails
    BEFORE INSERT OR UPDATE
    ON customer_emails
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Customer phones audit trigger
CREATE TRIGGER z_audit_customer_phones
    BEFORE INSERT OR UPDATE
    ON customer_phones
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Customers audit trigger
CREATE TRIGGER z_audit_customers
    BEFORE INSERT OR UPDATE
    ON customers
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Customer accounts audit trigger
CREATE TRIGGER z_audit_customer_accounts
    BEFORE INSERT OR UPDATE
    ON customer_accounts
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- OTP tokens audit trigger
CREATE TRIGGER z_audit_otp_tokens
    BEFORE INSERT OR UPDATE
    ON otp_tokens
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- NEW: Password reset tokens audit trigger
CREATE TRIGGER z_audit_password_reset_tokens
    BEFORE INSERT OR UPDATE
    ON password_reset_tokens
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Business config audit trigger
CREATE TRIGGER z_audit_business_config
    BEFORE INSERT OR UPDATE
    ON business_config
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Job execution audit trigger (INSERT only since records are immutable)
CREATE TRIGGER z_audit_job_execution_audit
    BEFORE INSERT
    ON job_execution_audit
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();

-- Account status audit trigger
CREATE TRIGGER z_audit_account_status_audit
    BEFORE INSERT
    ON account_status_audit
    FOR EACH ROW
EXECUTE FUNCTION update_audit_fields();
