package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.OtpTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.domain.entity.OtpToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.OtpTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidOtpFormatException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OtpTokenService {

    private final OtpTokenRepository otpTokenRepository;

    public OtpTokenService(OtpTokenRepository otpTokenRepository) {
        this.otpTokenRepository = otpTokenRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new OTP token for email verification
     * @param customerEmail the customer email entity
     * @param otpCode the OTP code
     * @param expiresAt when the OTP expires
     * @param maxAttempts maximum verification attempts allowed
     * @return the created OtpToken entity
     * @throws NullFieldException if any required parameter is null
     * @throws EmptyFieldException if otpCode is empty
     * @throws FieldTooShortException if otpCode is too short
     * @throws FieldTooLongException if otpCode is too long
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public OtpToken createEmailVerificationOtp(CustomerEmail customerEmail, String otpCode,
                                               OffsetDateTime expiresAt, Integer maxAttempts) throws ServiceException {
        return createOtp(customerEmail, null, otpCode, OtpPurpose.EMAIL_VERIFICATION,
                OtpDeliveryMethod.EMAIL, expiresAt, maxAttempts);
    }

    /**
     * Creates a new OTP token for phone verification
     * @param customerPhone the customer phone entity
     * @param otpCode the OTP code
     * @param expiresAt when the OTP expires
     * @param maxAttempts maximum verification attempts allowed
     * @return the created OtpToken entity
     * @throws NullFieldException if any required parameter is null
     * @throws EmptyFieldException if otpCode is empty
     * @throws FieldTooShortException if otpCode is too short
     * @throws FieldTooLongException if otpCode is too long
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public OtpToken createPhoneVerificationOtp(CustomerPhone customerPhone, String otpCode,
                                               OffsetDateTime expiresAt, Integer maxAttempts) throws ServiceException {
        return createOtp(null, customerPhone, otpCode, OtpPurpose.PHONE_VERIFICATION,
                OtpDeliveryMethod.SMS, expiresAt, maxAttempts);
    }

    /**
     * Creates a new OTP token for password reset via email
     * @param customerEmail the customer email entity
     * @param otpCode the OTP code
     * @param expiresAt when the OTP expires
     * @param maxAttempts maximum verification attempts allowed
     * @return the created OtpToken entity
     * @throws NullFieldException if any required parameter is null
     * @throws EmptyFieldException if otpCode is empty
     * @throws FieldTooShortException if otpCode is too short
     * @throws FieldTooLongException if otpCode is too long
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public OtpToken createPasswordResetEmailOtp(CustomerEmail customerEmail, String otpCode,
                                                OffsetDateTime expiresAt, Integer maxAttempts) throws ServiceException {
        return createOtp(customerEmail, null, otpCode, OtpPurpose.PASSWORD_RESET,
                OtpDeliveryMethod.EMAIL, expiresAt, maxAttempts);
    }

    /**
     * Creates a new OTP token for password reset via phone
     * @param customerPhone the customer phone entity
     * @param otpCode the OTP code
     * @param expiresAt when the OTP expires
     * @param maxAttempts maximum verification attempts allowed
     * @return the created OtpToken entity
     * @throws NullFieldException if any required parameter is null
     * @throws EmptyFieldException if otpCode is empty
     * @throws FieldTooShortException if otpCode is too short
     * @throws FieldTooLongException if otpCode is too long
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public OtpToken createPasswordResetPhoneOtp(CustomerPhone customerPhone, String otpCode,
                                                OffsetDateTime expiresAt, Integer maxAttempts) throws ServiceException {
        return createOtp(null, customerPhone, otpCode, OtpPurpose.PASSWORD_RESET,
                OtpDeliveryMethod.SMS, expiresAt, maxAttempts);
    }

    // ===== EMAIL VERIFICATION OPERATIONS =====

    /**
     * Finds valid email verification OTP
     * @param otpCode the OTP code to search for
     * @param email the email address
     * @return the OtpToken entity if found and valid
     * @throws NullFieldException if otpCode or email is null
     * @throws EmptyFieldException if otpCode or email is empty
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws OtpTokenNotFoundException if valid OTP token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public OtpToken findValidEmailVerificationOtp(String otpCode, String email) throws ServiceException {
        DataValidator.validateOtpCode(otpCode, "otpCode");
        DataValidator.validateEmail(email, "email");

        try {
            return otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                            otpCode.trim(), email.trim(), OtpPurpose.EMAIL_VERIFICATION, OffsetDateTime.now(), 3)
                    .orElseThrow(() -> new OtpTokenNotFoundException(otpCode));
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds latest email verification OTP for rate limiting
     * @param email the email address
     * @return the latest OtpToken entity if found
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Optional<OtpToken> findLatestEmailVerificationOtp(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            return otpTokenRepository.findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(email.trim(), OtpPurpose.EMAIL_VERIFICATION);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts email verification OTPs in time window
     * @param email the email address
     * @param since the time window start
     * @return count of OTPs
     * @throws NullFieldException if email or since is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countEmailVerificationOtpsInWindow(String email, OffsetDateTime since) throws ServiceException {
        DataValidator.validateEmail(email, "email");
        DataValidator.checkNotNull(since, "since");

        try {
            return otpTokenRepository.countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(email.trim(), OtpPurpose.EMAIL_VERIFICATION, since);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Invalidates active email verification OTPs
     * @param email the email address
     * @return number of OTPs invalidated
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int invalidateActiveEmailVerificationOtps(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            List<OtpToken> activeOtps = otpTokenRepository.findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(
                    email.trim(), OtpPurpose.EMAIL_VERIFICATION);

            if (!activeOtps.isEmpty()) {
                return otpTokenRepository.markOtpsAsUsed(activeOtps, OffsetDateTime.now());
            }
            return 0;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== PHONE VERIFICATION OPERATIONS =====

    /**
     * Finds valid phone verification OTP
     * @param otpCode the OTP code to search for
     * @param phone the phone number
     * @return the OtpToken entity if found and valid
     * @throws NullFieldException if otpCode or phone is null
     * @throws EmptyFieldException if otpCode or phone is empty
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws OtpTokenNotFoundException if valid OTP token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public OtpToken findValidPhoneVerificationOtp(String otpCode, String phone) throws ServiceException {
        DataValidator.validateOtpCode(otpCode, "otpCode");
        DataValidator.validatePhone(phone, "phone");

        try {
            return otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                            otpCode.trim(), phone.trim(), OtpPurpose.PHONE_VERIFICATION, OffsetDateTime.now(), 3)
                    .orElseThrow(() -> new OtpTokenNotFoundException(otpCode));
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds latest phone verification OTP for rate limiting
     * @param phone the phone number
     * @return the latest OtpToken entity if found
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Optional<OtpToken> findLatestPhoneVerificationOtp(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            return otpTokenRepository.findTopByCustomerPhonePhoneAndPurposeOrderByCreatedDateDesc(phone.trim(), OtpPurpose.PHONE_VERIFICATION);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts phone verification OTPs in time window
     * @param phone the phone number
     * @param since the time window start
     * @return count of OTPs
     * @throws NullFieldException if phone or since is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countPhoneVerificationOtpsInWindow(String phone, OffsetDateTime since) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");
        DataValidator.checkNotNull(since, "since");

        try {
            return otpTokenRepository.countByCustomerPhonePhoneAndPurposeAndCreatedDateAfter(phone.trim(), OtpPurpose.PHONE_VERIFICATION, since);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Invalidates active phone verification OTPs
     * @param phone the phone number
     * @return number of OTPs invalidated
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int invalidateActivePhoneVerificationOtps(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            List<OtpToken> activeOtps = otpTokenRepository.findByCustomerPhonePhoneAndPurposeAndUsedAtIsNull(
                    phone.trim(), OtpPurpose.PHONE_VERIFICATION);

            if (!activeOtps.isEmpty()) {
                return otpTokenRepository.markOtpsAsUsed(activeOtps, OffsetDateTime.now());
            }
            return 0;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== PASSWORD RESET VIA EMAIL OPERATIONS =====

    /**
     * Finds valid password reset email OTP
     * @param otpCode the OTP code to search for
     * @param email the email address
     * @return the OtpToken entity if found and valid
     * @throws NullFieldException if otpCode or email is null
     * @throws EmptyFieldException if otpCode or email is empty
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws OtpTokenNotFoundException if valid OTP token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public OtpToken findValidPasswordResetEmailOtp(String otpCode, String email) throws ServiceException {
        DataValidator.validateOtpCode(otpCode, "otpCode");
        DataValidator.validateEmail(email, "email");

        try {
            return otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                            otpCode.trim(), email.trim(), OtpPurpose.PASSWORD_RESET, OffsetDateTime.now(), 3)
                    .orElseThrow(() -> new OtpTokenNotFoundException(otpCode));
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds latest password reset email OTP for rate limiting
     * @param email the email address
     * @return the latest OtpToken entity if found
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Optional<OtpToken> findLatestPasswordResetEmailOtp(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            return otpTokenRepository.findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(email.trim(), OtpPurpose.PASSWORD_RESET);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts password reset email OTPs in time window
     * @param email the email address
     * @param since the time window start
     * @return count of OTPs
     * @throws NullFieldException if email or since is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countPasswordResetEmailOtpsInWindow(String email, OffsetDateTime since) throws ServiceException {
        DataValidator.validateEmail(email, "email");
        DataValidator.checkNotNull(since, "since");

        try {
            return otpTokenRepository.countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(email.trim(), OtpPurpose.PASSWORD_RESET, since);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Invalidates active password reset email OTPs
     * @param email the email address
     * @return number of OTPs invalidated
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int invalidateActivePasswordResetEmailOtps(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            List<OtpToken> activeOtps = otpTokenRepository.findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(
                    email.trim(), OtpPurpose.PASSWORD_RESET);

            if (!activeOtps.isEmpty()) {
                return otpTokenRepository.markOtpsAsUsed(activeOtps, OffsetDateTime.now());
            }
            return 0;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== PASSWORD RESET VIA PHONE OPERATIONS =====

    /**
     * Finds valid password reset phone OTP
     * @param otpCode the OTP code to search for
     * @param phone the phone number
     * @return the OtpToken entity if found and valid
     * @throws NullFieldException if otpCode or phone is null
     * @throws EmptyFieldException if otpCode or phone is empty
     * @throws InvalidOtpFormatException if otpCode format is invalid
     * @throws OtpTokenNotFoundException if valid OTP token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public OtpToken findValidPasswordResetPhoneOtp(String otpCode, String phone) throws ServiceException {
        DataValidator.validateOtpCode(otpCode, "otpCode");
        DataValidator.validatePhone(phone, "phone");

        try {
            return otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                            otpCode.trim(), phone.trim(), OtpPurpose.PASSWORD_RESET, OffsetDateTime.now(), 3)
                    .orElseThrow(() -> new OtpTokenNotFoundException(otpCode));
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds latest password reset phone OTP for rate limiting
     * @param phone the phone number
     * @return the latest OtpToken entity if found
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Optional<OtpToken> findLatestPasswordResetPhoneOtp(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            return otpTokenRepository.findTopByCustomerPhonePhoneAndPurposeOrderByCreatedDateDesc(phone.trim(), OtpPurpose.PASSWORD_RESET);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts password reset phone OTPs in time window
     * @param phone the phone number
     * @param since the time window start
     * @return count of OTPs
     * @throws NullFieldException if phone or since is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countPasswordResetPhoneOtpsInWindow(String phone, OffsetDateTime since) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");
        DataValidator.checkNotNull(since, "since");

        try {
            return otpTokenRepository.countByCustomerPhonePhoneAndPurposeAndCreatedDateAfter(phone.trim(), OtpPurpose.PASSWORD_RESET, since);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Invalidates active password reset phone OTPs
     * @param phone the phone number
     * @return number of OTPs invalidated
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int invalidateActivePasswordResetPhoneOtps(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            List<OtpToken> activeOtps = otpTokenRepository.findByCustomerPhonePhoneAndPurposeAndUsedAtIsNull(
                    phone.trim(), OtpPurpose.PASSWORD_RESET);

            if (!activeOtps.isEmpty()) {
                return otpTokenRepository.markOtpsAsUsed(activeOtps, OffsetDateTime.now());
            }
            return 0;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== BASIC READ OPERATIONS =====

    /**
     * Retrieves all OTP tokens
     * @return list of all OtpToken entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<OtpToken> getAllOtpTokens() throws ServiceException {
        try {
            return otpTokenRepository.findAll();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an OTP token by ID
     * @param otpTokenId the ID to search for
     * @return the OtpToken entity
     * @throws NullFieldException if otpTokenId is null
     * @throws OtpTokenNotFoundException if OTP token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public OtpToken findById(Long otpTokenId) throws ServiceException {
        DataValidator.checkNotNull(otpTokenId, "otpTokenId");

        try {
            return otpTokenRepository.findById(otpTokenId)
                    .orElseThrow(() -> new OtpTokenNotFoundException(otpTokenId));
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Increments attempt count for an OTP token
     * @param otpTokenId the ID of the OTP token to update
     * @return number of rows affected
     * @throws NullFieldException if otpTokenId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int incrementAttemptCount(Long otpTokenId) throws ServiceException {
        DataValidator.checkNotNull(otpTokenId, "otpTokenId");

        try {
            return otpTokenRepository.incrementAttemptCount(otpTokenId, OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Marks an OTP token as used
     * @param otpTokenId the ID of the OTP token to mark as used
     * @return number of rows affected
     * @throws NullFieldException if otpTokenId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public int markOtpAsUsed(Long otpTokenId) throws ServiceException {
        DataValidator.checkNotNull(otpTokenId, "otpTokenId");

        try {
            return otpTokenRepository.markOtpAsUsed(otpTokenId, OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing OTP token entity
     * @param otpToken the OTP token entity to save
     * @return the saved OtpToken entity
     * @throws NullFieldException if otpToken is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public OtpToken saveOtpToken(OtpToken otpToken) throws ServiceException {
        DataValidator.checkNotNull(otpToken, "otpToken");

        try {
            otpToken.setLastModifiedDate(OffsetDateTime.now());
            return otpTokenRepository.save(otpToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes an OTP token by ID
     * @param otpTokenId the ID of the OTP token to delete
     * @throws NullFieldException if otpTokenId is null
     * @throws OtpTokenNotFoundException if OTP token with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteById(Long otpTokenId) throws ServiceException {
        DataValidator.checkNotNull(otpTokenId, "otpTokenId");

        try {
            if (!otpTokenRepository.existsById(otpTokenId)) {
                throw new OtpTokenNotFoundException(otpTokenId);
            }
        } catch (OtpTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            otpTokenRepository.deleteById(otpTokenId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes an OTP token entity
     * @param otpToken the OTP token entity to delete
     * @throws NullFieldException if otpToken is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteOtpToken(OtpToken otpToken) throws ServiceException {
        DataValidator.checkNotNull(otpToken, "otpToken");

        try {
            otpTokenRepository.delete(otpToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UTILITY OPERATIONS =====

    /**
     * Checks if an OTP token exists by ID
     * @param otpTokenId the OTP token ID to check
     * @return true if OTP token exists, false otherwise
     * @throws NullFieldException if otpTokenId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean otpTokenExists(Long otpTokenId) throws ServiceException {
        DataValidator.checkNotNull(otpTokenId, "otpTokenId");

        try {
            return otpTokenRepository.existsById(otpTokenId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts all OTP tokens
     * @return total number of OTP tokens
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countAllOtpTokens() throws ServiceException {
        try {
            return otpTokenRepository.count();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Private helper method for creating OTP tokens
     */
    private OtpToken createOtp(CustomerEmail customerEmail, CustomerPhone customerPhone, String otpCode,
                               OtpPurpose purpose, OtpDeliveryMethod deliveryMethod,
                               OffsetDateTime expiresAt, Integer maxAttempts) throws ServiceException {
        DataValidator.validateOtpCode(otpCode, "otpCode");
        DataValidator.checkNotNull(purpose, "purpose");
        DataValidator.checkNotNull(deliveryMethod, "deliveryMethod");
        DataValidator.checkNotNull(expiresAt, "expiresAt");
        DataValidator.checkNotNull(maxAttempts, "maxAttempts");

        // Validate that exactly one contact method is provided
        if ((customerEmail == null && customerPhone == null) ||
                (customerEmail != null && customerPhone != null)) {
            throw new IllegalArgumentException("Exactly one of customerEmail or customerPhone must be provided");
        }

        try {
            OtpToken otpToken = new OtpToken();
            otpToken.setCustomerEmail(customerEmail);
            otpToken.setCustomerPhone(customerPhone);
            otpToken.setOtpCode(otpCode.trim());
            otpToken.setPurpose(purpose);
            otpToken.setDeliveryMethod(deliveryMethod);
            otpToken.setExpiresAt(expiresAt);
            otpToken.setMaxAttempts(maxAttempts);
            otpToken.setAttemptsCount(0);
            otpToken.setCreatedDate(OffsetDateTime.now());
            otpToken.setLastModifiedDate(OffsetDateTime.now());

            return otpTokenRepository.save(otpToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }
}