-- V002__create_customer_account_verification_status_enum.sql
-- Customer login accounts with authentication credentials

CREATE TYPE customer_account_verification_status_enum AS ENUM (
    'UNVERIFIED',
    'EMAIL_VERIFIED',
    'PHONE_VERIFIED',
    'FULLY_VERIFIED'
    );