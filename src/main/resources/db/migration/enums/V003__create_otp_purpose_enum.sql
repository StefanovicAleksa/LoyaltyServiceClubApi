-- V003__create_otp_purpose_enum.sql
-- Enum for OTP purposes across the application

CREATE TYPE otp_purpose_enum AS ENUM (
    'EMAIL_VERIFICATION',
    'PHONE_VERIFICATION',
    'PASSWORD_RESET'
    );