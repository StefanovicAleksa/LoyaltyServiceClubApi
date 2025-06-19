-- V060__add_audit_triggers.sql
-- Triggers for setting table fields created_date and last_modified_date

-- customer emails
CREATE TRIGGER audit_customer_emails
BEFORE INSERT OR UPDATE ON customer_emails
FOR EACH ROW EXECUTE FUNCTION update_audit_fields();

-- customer phones
CREATE TRIGGER audit_customer_phones
BEFORE INSERT OR UPDATE ON customer_phones
FOR EACH ROW EXECUTE FUNCTION update_audit_fields();

-- customers
CREATE TRIGGER audit_customers
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION update_audit_fields();

-- customer_accounts
CREATE TRIGGER audit_customer_accounts
BEFORE INSERT OR UPDATE ON customer_accounts
FOR EACH ROW EXECUTE FUNCTION update_audit_fields();

-- password reset tokens
CREATE TRIGGER audit_password_reset_tokens
BEFORE INSERT OR UPDATE ON password_reset_tokens
FOR EACH ROW EXECUTE FUNCTION update_audit_fields();