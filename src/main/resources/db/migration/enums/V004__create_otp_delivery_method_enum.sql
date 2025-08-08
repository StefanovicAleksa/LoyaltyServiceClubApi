-- V004__create_otp_delivery_method_enum.sql
-- Enum for OTP delivery methods

CREATE TYPE otp_delivery_method_enum AS ENUM (
    'EMAIL',
    'SMS'
    );