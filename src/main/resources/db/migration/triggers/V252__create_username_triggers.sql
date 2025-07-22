-- V252__create_username_trigger.sql
-- Username setting trigger for customer account creation

CREATE TRIGGER set_username_on_account_creation
    BEFORE INSERT ON customer_accounts
    FOR EACH ROW
EXECUTE FUNCTION set_customer_account_username();