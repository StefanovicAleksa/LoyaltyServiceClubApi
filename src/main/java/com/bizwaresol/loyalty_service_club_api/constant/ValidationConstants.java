package com.bizwaresol.loyalty_service_club_api.constant;

import java.util.regex.Pattern;

public final class ValidationConstants {

    private ValidationConstants() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    public static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+381[0-9]{8,9}$"
    );

    public static final Pattern NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s\\-']+$"
    );

    // OTP validation constants
    public static final int OTP_CODE_LENGTH = 6;
    public static final Pattern OTP_CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    // UUID validation for tokens
    public static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 255;

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 30;

    public static final int MIN_EMAIL_LENGTH = 5;
    public static final int MAX_EMAIL_LENGTH = 50;

    public static final int MIN_PHONE_LENGTH = 12;
    public static final int MAX_PHONE_LENGTH = 13;

    public static final int MIN_USERNAME_LENGTH = 5;
    public static final int MAX_USERNAME_LENGTH = 60;

    public static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    public static final String PASSWORD_REQUIREMENTS = "Password must be at least " + MIN_PASSWORD_LENGTH +
            " characters long and contain at least one number";
}
