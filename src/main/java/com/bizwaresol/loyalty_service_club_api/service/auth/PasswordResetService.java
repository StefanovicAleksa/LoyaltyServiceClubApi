package com.bizwaresol.loyalty_service_club_api.service.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.VerifyCodeResponse;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateActivePasswordResetTokenException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.InvalidOtpCodeException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.ActivePasswordResetTokenExistsException;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerAccountService;
import com.bizwaresol.loyalty_service_club_api.service.data.PasswordResetTokenService;
import com.bizwaresol.loyalty_service_club_api.service.verification.EmailVerificationService;
import com.bizwaresol.loyalty_service_club_api.service.verification.PhoneVerificationService;
import com.bizwaresol.loyalty_service_club_api.util.mappers.PasswordResetErrorMapper;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Orchestration service for the multistep password reset process.
 */
@Service
@Transactional
public class PasswordResetService {

    private final CustomerAccountService customerAccountService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationService emailVerificationService;
    private final PhoneVerificationService phoneVerificationService;

    // We can add a configuration property for token expiry later.
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    public PasswordResetService(CustomerAccountService customerAccountService,
                                PasswordResetTokenService passwordResetTokenService,
                                EmailVerificationService emailVerificationService,
                                PhoneVerificationService phoneVerificationService) {
        this.customerAccountService = customerAccountService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailVerificationService = emailVerificationService;
        this.phoneVerificationService = phoneVerificationService;
    }

    /**
     * Step 1: Initiates a password reset request by sending an OTP to the user's contact method.
     *
     * @param contact The user's email or phone number.
     * @param deliveryMethod The method of contact (EMAIL or SMS).
     * @throws ActivePasswordResetTokenExistsException if the user already has a pending reset request.
     */
    @Transactional
    public void requestPasswordReset(String contact, OtpDeliveryMethod deliveryMethod) {
        CustomerAccount account = findAccountByIdentifier(contact);

        // Business Rule: Prevent spamming by checking for an existing active token first.
        passwordResetTokenService.findActiveTokenByAccountId(account.getId()).ifPresent(token -> {
            throw new ActivePasswordResetTokenExistsException();
        });

        if (deliveryMethod == OtpDeliveryMethod.EMAIL) {
            emailVerificationService.sendPasswordResetCode(contact);
        } else {
            phoneVerificationService.sendPasswordResetCode(contact);
        }
    }

    /**
     * Step 2: Verifies the received OTP and, if successful, creates and returns a secure password reset token.
     *
     * @param contact The user's email or phone number.
     * @param deliveryMethod The method of contact (EMAIL or SMS).
     * @param otpCode The OTP code submitted by the user.
     * @return The generated password reset token string.
     * @throws InvalidOtpCodeException if the OTP verification fails.
     */
    @Transactional
    public String verifyOtpAndCreateResetToken(String contact, OtpDeliveryMethod deliveryMethod, String otpCode) {
        VerifyCodeResponse verificationResponse;
        if (deliveryMethod == OtpDeliveryMethod.EMAIL) {
            verificationResponse = emailVerificationService.verifyPasswordResetCode(contact, otpCode);
        } else {
            verificationResponse = phoneVerificationService.verifyPasswordResetCode(contact, otpCode);
        }

        if (!verificationResponse.success()) {
            throw new InvalidOtpCodeException(contact);
        }

        CustomerAccount account = findAccountByIdentifier(contact);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        try {
            PasswordResetToken token = passwordResetTokenService.createToken(account, expiresAt);
            return token.getToken();
        } catch (DuplicateActivePasswordResetTokenException e) {
            // This handles the rare race condition where a user requests two tokens in parallel.
            throw PasswordResetErrorMapper.mapDuplicateTokenException(e);
        }
    }

    /**
     * Step 3: Resets the user's password using a valid token.
     *
     * @param tokenString The unique password reset token string.
     * @param newPassword The user's desired new password.
     */
    @Transactional
    public void resetPassword(String tokenString, String newPassword) {
        DataValidator.validatePassword(newPassword, "newPassword");

        PasswordResetToken token = passwordResetTokenService.findByToken(tokenString);
        PasswordResetErrorMapper.validateTokenState(token);

        CustomerAccount account = token.getCustomerAccount();
        customerAccountService.updatePassword(account.getId(), newPassword);

        passwordResetTokenService.markTokenAsUsed(token);
    }

    /**
     * Finds a customer account by their username, which could be an email or phone number.
     *
     * @param identifier The email or phone number.
     * @return The found CustomerAccount.
     */
    private CustomerAccount findAccountByIdentifier(String identifier) {
        // The username field in the customer_accounts table is populated by a trigger
        // with either the email or phone number, so we can use findByUsername for both.
        return customerAccountService.findByUsername(identifier);
    }
}
