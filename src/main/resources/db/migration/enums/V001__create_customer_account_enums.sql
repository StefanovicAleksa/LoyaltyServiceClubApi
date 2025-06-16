-- V001__create_customer_account_enums.sql
-- Enums for customer account status management

CREATE TYPE customer_account_activity_status_enum AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED'
    );

CREATE TYPE customer_account_verification_status_enum AS ENUM (
    'UNVERIFIED',
    'EMAIL_VERIFIED',
    'PHONE_VERIFIED',
    'FULLY_VERIFIED'
    );