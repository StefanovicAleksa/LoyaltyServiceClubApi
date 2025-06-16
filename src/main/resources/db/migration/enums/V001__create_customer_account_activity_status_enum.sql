-- V001__create_customer_account_activity_status_enum.sql
-- Enums for customer account status management

CREATE TYPE customer_account_activity_status_enum AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED'
    );