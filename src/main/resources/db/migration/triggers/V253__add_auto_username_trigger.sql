-- V253__add_auto_username_trigger.sql
-- Trigger to automatically set username on customer account insert

CREATE TRIGGER set_username_on_account_creation
    BEFORE INSERT ON customer_accounts
    FOR EACH ROW
EXECUTE FUNCTION set_customer_account_username();