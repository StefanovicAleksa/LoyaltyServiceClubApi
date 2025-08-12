package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.PasswordResetTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PasswordResetTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Service layer for managing PasswordResetToken entities.
 * This service handles the data access logic and translates repository exceptions
 * into service-level exceptions.
 */
@Service
@Transactional
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new password reset token for a customer account.
     *
     * @param customerAccount The account for which to create the token.
     * @param expiresAt       The timestamp when the token should expire.
     * @return The newly created PasswordResetToken entity.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional
    public PasswordResetToken createToken(CustomerAccount customerAccount, OffsetDateTime expiresAt) throws ServiceException {
        DataValidator.checkNotNull(customerAccount, "customerAccount");
        DataValidator.checkNotNull(expiresAt, "expiresAt");

        try {
            PasswordResetToken token = new PasswordResetToken();
            token.setCustomerAccount(customerAccount);
            token.setToken(PasswordResetToken.generateToken());
            token.setExpiresAt(expiresAt);
            token.setCreatedDate(OffsetDateTime.now());
            token.setLastModifiedDate(OffsetDateTime.now());

            return passwordResetTokenRepository.save(token);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== READ OPERATIONS =====

    /**
     * Finds a password reset token by its unique token string.
     *
     * @param token The token string to search for.
     * @return The PasswordResetToken entity.
     * @throws PasswordResetTokenNotFoundException if no token is found.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional(readOnly = true)
    public PasswordResetToken findByToken(String token) throws ServiceException {
        DataValidator.validatePasswordResetToken(token, "token");

        try {
            return passwordResetTokenRepository.findByToken(token)
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(token));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds a token by its ID.
     *
     * @param id The ID of the token.
     * @return The PasswordResetToken entity.
     * @throws PasswordResetTokenNotFoundException if no token is found.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional(readOnly = true)
    public PasswordResetToken findById(Long id) throws ServiceException {
        DataValidator.checkNotNull(id, "id");

        try {
            return passwordResetTokenRepository.findById(id)
                    .orElseThrow(() -> new PasswordResetTokenNotFoundException(id));
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an active (unused) token for a specific customer account.
     *
     * @param accountId The ID of the customer account.
     * @return An Optional containing the token if an active one exists.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findActiveTokenByAccountId(Long accountId) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");

        try {
            return passwordResetTokenRepository.findByCustomerAccountIdAndUsedAtIsNull(accountId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Marks a password reset token as used.
     *
     * @param token The token entity to mark as used.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional
    public void markTokenAsUsed(PasswordResetToken token) throws ServiceException {
        DataValidator.checkNotNull(token, "token");
        DataValidator.checkNotNull(token.getId(), "token.id");

        try {
            passwordResetTokenRepository.markAsUsed(token.getId(), OffsetDateTime.now());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes a password reset token by its ID.
     *
     * @param id The ID of the token to delete.
     * @throws PasswordResetTokenNotFoundException if the token does not exist.
     * @throws ServiceException if the repository operation fails.
     */
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        DataValidator.checkNotNull(id, "id");

        try {
            if (!passwordResetTokenRepository.existsById(id)) {
                throw new PasswordResetTokenNotFoundException(id);
            }
            passwordResetTokenRepository.deleteById(id);
        } catch (PasswordResetTokenNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }
}
