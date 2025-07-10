package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.PasswordResetTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PasswordResetTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private static final Duration DEFAULT_TOKEN_EXPIRY = Duration.ofHours(24);
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new password reset token with default expiry (24 hours)
     * @param customerAccount the customer account to create token for
     * @return the created PasswordResetToken entity
     * @throws NullFieldException if customerAccount is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public PasswordResetToken createToken(CustomerAccount customerAccount) throws ServiceException {
        return createTokenWithExpiry(customerAccount, DEFAULT_TOKEN_EXPIRY);
    }

    /**
     * Creates a new password reset token with custom expiry
     * @param customerAccount the customer account to create token for
     * @param expiry the duration until token expires
     * @return the created PasswordResetToken entity
     * @throws NullFieldException if customerAccount or expiry is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public PasswordResetToken createTokenWithExpiry(CustomerAccount customerAccount, Duration expiry) throws ServiceException {
        DataValidator.checkNotNull(customerAccount, "customerAccount");
        DataValidator.checkNotNull(expiry, "expiry");

        try {
            String token = generateSecureToken();
            OffsetDateTime now = OffsetDateTime.now();

            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setCustomerAccount(customerAccount);
            passwordResetToken.setToken(token);
            passwordResetToken.setExpiresAt(now.plus(expiry));
            passwordResetToken.setUsed(false);
            passwordResetToken.setCreatedDate(now);
            passwordResetToken.setLastModifiedDate(now);

            return passwordResetTokenRepository.save(passwordResetToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== READ OPERATIONS =====

    /**
     * Finds a password reset token by token string
     * @param token the token string to search for
     * @return the PasswordResetToken entity
     * @throws NullFieldException if token is null
     * @throws EmptyFieldException if token is empty
     * @throws PasswordResetTokenNotFoundException if token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public PasswordResetToken findByToken(String token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotEmptyString(token, "token");

        try {
            return passwordResetTokenRepository.findByToken(token.trim())
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(token));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds a password reset token by ID
     * @param tokenId the ID to search for
     * @return the PasswordResetToken entity
     * @throws NullFieldException if tokenId is null
     * @throws PasswordResetTokenNotFoundException if token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public PasswordResetToken findById(Long tokenId) throws ServiceException {
        DataValidator.checkNotNull(tokenId, "tokenId");

        try {
            return passwordResetTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(tokenId));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds all active (unused and not expired) tokens for a customer account
     * @param customerAccountId the customer account ID to search for
     * @return list of active PasswordResetToken entities
     * @throws NullFieldException if customerAccountId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<PasswordResetToken> findActiveTokensForAccount(Long customerAccountId) throws ServiceException {
        DataValidator.checkNotNull(customerAccountId, "customerAccountId");

        try {
            return passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    customerAccountId, OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds all expired tokens for cleanup purposes
     * @return list of expired PasswordResetToken entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<PasswordResetToken> findExpiredTokens() throws ServiceException {
        try {
            return passwordResetTokenRepository.findByExpiresAtBefore(OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a token exists by token string
     * @param token the token string to check
     * @return true if token exists, false otherwise
     * @throws NullFieldException if token is null
     * @throws EmptyFieldException if token is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean tokenExists(String token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotEmptyString(token, "token");

        try {
            return passwordResetTokenRepository.existsByToken(token.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a token is valid (exists, not used, not expired)
     * @param token the token string to validate
     * @return true if token is valid, false otherwise
     * @throws NullFieldException if token is null
     * @throws EmptyFieldException if token is empty
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotEmptyString(token, "token");

        try {
            return passwordResetTokenRepository.findByToken(token.trim())
                    .map(this::isTokenActive)
                    .orElse(false);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts active tokens for a customer account
     * @param customerAccountId the customer account ID
     * @return count of active tokens
     * @throws NullFieldException if customerAccountId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countActiveTokensForAccount(Long customerAccountId) throws ServiceException {
        DataValidator.checkNotNull(customerAccountId, "customerAccountId");

        try {
            return passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    customerAccountId, OffsetDateTime.now()).size();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Marks a token as used
     * @param token the token string to mark as used
     * @return the updated PasswordResetToken entity
     * @throws NullFieldException if token is null
     * @throws EmptyFieldException if token is empty
     * @throws PasswordResetTokenNotFoundException if token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public PasswordResetToken markTokenAsUsed(String token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotEmptyString(token, "token");

        PasswordResetToken passwordResetToken;
        try {
            passwordResetToken = passwordResetTokenRepository.findByToken(token.trim())
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(token));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            passwordResetToken.setUsed(true);
            passwordResetToken.setLastModifiedDate(OffsetDateTime.now());
            return passwordResetTokenRepository.save(passwordResetToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing password reset token entity
     * @param passwordResetToken the token entity to save
     * @return the saved PasswordResetToken entity
     * @throws NullFieldException if passwordResetToken is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public PasswordResetToken saveToken(PasswordResetToken passwordResetToken) throws ServiceException {
        DataValidator.checkNotNull(passwordResetToken, "passwordResetToken");

        try {
            passwordResetToken.setLastModifiedDate(OffsetDateTime.now());
            return passwordResetTokenRepository.save(passwordResetToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes a password reset token entity
     * @param passwordResetToken the token entity to delete
     * @throws NullFieldException if passwordResetToken is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteToken(PasswordResetToken passwordResetToken) throws ServiceException {
        DataValidator.checkNotNull(passwordResetToken, "passwordResetToken");

        try {
            passwordResetTokenRepository.delete(passwordResetToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes a password reset token by token string
     * @param token the token string to delete
     * @throws NullFieldException if token is null
     * @throws EmptyFieldException if token is empty
     * @throws PasswordResetTokenNotFoundException if token doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteByToken(String token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotEmptyString(token, "token");

        PasswordResetToken passwordResetToken;
        try {
            passwordResetToken = passwordResetTokenRepository.findByToken(token.trim())
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(token));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            passwordResetTokenRepository.delete(passwordResetToken);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes all expired tokens (cleanup operation)
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteExpiredTokens() throws ServiceException {
        try {
            passwordResetTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes all tokens for a customer account (security measure)
     * @param customerAccountId the customer account ID
     * @throws NullFieldException if customerAccountId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteAllTokensForAccount(Long customerAccountId) throws ServiceException {
        DataValidator.checkNotNull(customerAccountId, "customerAccountId");

        try {
            List<PasswordResetToken> tokens = passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    customerAccountId, OffsetDateTime.now());
            passwordResetTokenRepository.deleteAll(tokens);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== BUSINESS LOGIC OPERATIONS =====

    /**
     * Invalidates all active tokens for a customer account by marking them as used
     * @param customerAccountId the customer account ID
     * @throws NullFieldException if customerAccountId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void invalidateAllAccountTokens(Long customerAccountId) throws ServiceException {
        DataValidator.checkNotNull(customerAccountId, "customerAccountId");

        try {
            List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    customerAccountId, OffsetDateTime.now());

            OffsetDateTime now = OffsetDateTime.now();
            for (PasswordResetToken token : activeTokens) {
                token.setUsed(true);
                token.setLastModifiedDate(now);
            }

            passwordResetTokenRepository.saveAll(activeTokens);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Cleanup expired tokens (intended for scheduled execution)
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void cleanupExpiredTokens() throws ServiceException {
        deleteExpiredTokens();
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Generates a cryptographically secure random token
     * @return a base64-encoded secure token string
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Checks if a token is active (not used and not expired)
     * @param token the token to check
     * @return true if token is active, false otherwise
     */
    private boolean isTokenActive(PasswordResetToken token) {
        return !token.isUsed() && token.getExpiresAt().isAfter(OffsetDateTime.now());
    }
}