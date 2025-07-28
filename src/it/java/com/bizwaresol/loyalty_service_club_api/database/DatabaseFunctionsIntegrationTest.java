package com.bizwaresol.loyalty_service_club_api.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.time.OffsetDateTime;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test-postgres")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false"
})
@Component
public class DatabaseFunctionsIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        // Clean all tables except business_config
        jdbcTemplate.execute("TRUNCATE TABLE password_reset_tokens CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE customer_accounts CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE customers CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE customer_emails CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE customer_phones CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE account_status_audit CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE job_execution_audit CASCADE");

        // Reset sequences
        jdbcTemplate.execute("ALTER SEQUENCE customer_emails_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE customer_phones_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE customers_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE customer_accounts_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE account_status_audit_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE job_execution_audit_id_seq RESTART WITH 1");

        // Ensure cleanup configurations exist for new tests
        ensureCleanupConfigsExist();
    }

    private void ensureCleanupConfigsExist() {
        // Insert cleanup configs if they don't exist (safety net for tests)
        jdbcTemplate.update("""
            INSERT INTO business_config (key, value, description) VALUES 
            ('password_reset_token_cleanup_days', '7', 'Days to keep password reset tokens after creation'),
            ('job_execution_audit_cleanup_days', '90', 'Days to keep job execution audit records'),
            ('account_status_audit_cleanup_days', '365', 'Days to keep account status change audit records'),
            ('unverified_account_cleanup_days', '30', 'Days to keep unverified accounts that never logged in'),
            ('cleanup_batch_size', '500', 'Batch size for cleanup operations to avoid long locks')
            ON CONFLICT (key) DO NOTHING
            """);
    }

    // ===== UPDATE_VERIFICATION_STATUS_FOR_CUSTOMER FUNCTION TESTS =====

    @Test
    void updateVerificationStatusForCustomer_WithBothVerified_ShouldUpdateToFullyVerified() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("verified@gmail.com", true);
        Long phoneId = createCustomerPhone("+381611111111", true);
        Long customerId = createCustomer("Verified", "User", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "verified@gmail.com");

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
            stmt.setLong(1, customerId);
            stmt.execute();
        }

        // Assert
        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(verificationStatus).isEqualTo("FULLY_VERIFIED");
    }

    @Test
    void updateVerificationStatusForCustomer_WithEmailOnlyVerified_ShouldUpdateToEmailVerified() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("email@gmail.com", true);
        Long phoneId = createCustomerPhone("+381622222222", false);
        Long customerId = createCustomer("Email", "User", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "email@gmail.com");

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
            stmt.setLong(1, customerId);
            stmt.execute();
        }

        // Assert
        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(verificationStatus).isEqualTo("EMAIL_VERIFIED");
    }

    @Test
    void updateVerificationStatusForCustomer_WithPhoneOnlyVerified_ShouldUpdateToPhoneVerified() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("phone@gmail.com", false);
        Long phoneId = createCustomerPhone("+381633333333", true);
        Long customerId = createCustomer("Phone", "User", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "phone@gmail.com");

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
            stmt.setLong(1, customerId);
            stmt.execute();
        }

        // Assert
        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(verificationStatus).isEqualTo("PHONE_VERIFIED");
    }

    @Test
    void updateVerificationStatusForCustomer_WithNoneVerified_ShouldRemainUnverified() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("none@gmail.com", false);
        Long phoneId = createCustomerPhone("+381644444444", false);
        Long customerId = createCustomer("None", "User", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "none@gmail.com");

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
            stmt.setLong(1, customerId);
            stmt.execute();
        }

        // Assert
        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(verificationStatus).isEqualTo("UNVERIFIED");
    }

    @Test
    void updateVerificationStatusForCustomer_WithNonExistentCustomer_ShouldNotThrowError() {
        // Act & Assert - Should not throw exception
        assertThatCode(() -> {
            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
                stmt.setLong(1, 99999L); // Non-existent customer ID
                stmt.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Database operation failed", e);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void updateVerificationStatusForCustomer_WithCustomerButNoAccount_ShouldNotThrowError() {
        // Arrange - Create customer without account
        Long emailId = createCustomerEmail("noAccount@gmail.com", true);
        Long customerId = createCustomer("No", "Account", emailId, null);

        // Act & Assert - Should not throw exception
        assertThatCode(() -> {
            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("SELECT update_verification_status_for_customer(?)")) {
                stmt.setLong(1, customerId);
                stmt.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Database operation failed", e);
            }
        }).doesNotThrowAnyException();
    }

    // ===== MARK_INACTIVE_ACCOUNTS_BATCHED FUNCTION TESTS =====

    @Test
    void markInactiveAccountsBatched_WithOldAccounts_ShouldMarkInactive() throws SQLException {
        // Arrange - Create accounts with old login dates
        Long emailId1 = createCustomerEmail("old1@gmail.com", true);
        Long emailId2 = createCustomerEmail("old2@gmail.com", true);
        Long customerId1 = createCustomer("Old", "User1", emailId1, null);
        Long customerId2 = createCustomer("Old", "User2", emailId2, null);
        Long accountId1 = createCustomerAccount(customerId1, "old1@gmail.com");
        Long accountId2 = createCustomerAccount(customerId2, "old2@gmail.com");

        // Set last login to 90 days ago (beyond the default 60-day threshold)
        OffsetDateTime oldLogin = OffsetDateTime.now().minusDays(90);
        updateAccountLastLogin(accountId1, oldLogin);
        updateAccountLastLogin(accountId2, oldLogin);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT mark_inactive_accounts_batched()")) {
            stmt.execute();
        }

        // Assert
        String status1 = jdbcTemplate.queryForObject(
                "SELECT activity_status FROM customer_accounts WHERE id = ?",
                String.class, accountId1);
        String status2 = jdbcTemplate.queryForObject(
                "SELECT activity_status FROM customer_accounts WHERE id = ?",
                String.class, accountId2);

        assertThat(status1).isEqualTo("INACTIVE");
        assertThat(status2).isEqualTo("INACTIVE");

        // Verify audit record was created
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_execution_audit WHERE job_name = 'mark_inactive_accounts' AND success = true",
                Integer.class);
        assertThat(auditCount).isGreaterThan(0);
    }

    // ===== CLEANUP_PASSWORD_RESET_TOKENS FUNCTION TESTS =====

    @Test
    void cleanupPasswordResetTokens_WithOldTokens_ShouldDeleteExpiredTokens() throws SQLException {
        // Arrange - Create old and recent tokens
        Long emailId = createCustomerEmail("tokens@gmail.com", true);
        Long customerId = createCustomer("Token", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "tokens@gmail.com");

        // Create old token (9 days old - beyond default 7 day threshold)
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(9);
        Long oldTokenId = createPasswordResetToken(accountId, "old-token", oldDate);

        // Create recent token (5 days old - within threshold)
        OffsetDateTime recentDate = OffsetDateTime.now().minusDays(5);
        Long recentTokenId = createPasswordResetToken(accountId, "recent-token", recentDate);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_password_reset_tokens()")) {
            stmt.execute();
        }

        // Assert - Old token should be deleted, recent token should remain
        Boolean oldTokenExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM password_reset_tokens WHERE id = ?)",
                Boolean.class, oldTokenId);
        Boolean recentTokenExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM password_reset_tokens WHERE id = ?)",
                Boolean.class, recentTokenId);

        assertThat(oldTokenExists).isFalse();
        assertThat(recentTokenExists).isTrue();

        // Verify audit record
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_execution_audit WHERE job_name = 'cleanup_password_reset_tokens' AND success = true",
                Integer.class);
        assertThat(auditCount).isGreaterThan(0);
    }

    @Test
    void cleanupPasswordResetTokens_WithNoOldTokens_ShouldLogZeroDeleted() throws SQLException {
        // Arrange - Create only recent tokens
        Long emailId = createCustomerEmail("recent@gmail.com", true);
        Long customerId = createCustomer("Recent", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "recent@gmail.com");

        OffsetDateTime recentDate = OffsetDateTime.now().minusDays(2);
        createPasswordResetToken(accountId, "recent-token", recentDate);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_password_reset_tokens()")) {
            stmt.execute();
        }

        // Assert - Should have processed 0 records
        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT records_processed FROM job_execution_audit WHERE job_name = 'cleanup_password_reset_tokens' " +
                        "AND execution_date = CURRENT_DATE ORDER BY created_date DESC LIMIT 1",
                Integer.class);
        assertThat(processedCount).isEqualTo(0);
    }

    // ===== CLEANUP_ACCOUNT_STATUS_AUDIT FUNCTION TESTS =====

    @Test
    void cleanupAccountStatusAudit_WithOldRecords_ShouldDeleteExpiredRecords() throws SQLException {
        // Arrange - Create old and recent audit records
        Long emailId = createCustomerEmail("audit@gmail.com", true);
        Long customerId = createCustomer("Audit", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "audit@gmail.com");

        // Create old audit record (400 days old - beyond default 365 day threshold)
        createAccountStatusAuditRecord(accountId, "ACTIVE", "INACTIVE", OffsetDateTime.now().minusDays(400));

        // Create recent audit record (200 days old - within threshold)
        createAccountStatusAuditRecord(accountId, "INACTIVE", "ACTIVE", OffsetDateTime.now().minusDays(200));

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_account_status_audit()")) {
            stmt.execute();
        }

        // Assert - Should have deleted 1 record (the old one)
        Integer remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_status_audit WHERE account_id = ?",
                Integer.class, accountId);
        assertThat(remainingCount).isEqualTo(1);

        // Verify audit record
        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT records_processed FROM job_execution_audit WHERE job_name = 'cleanup_account_status_audit' " +
                        "AND execution_date = CURRENT_DATE ORDER BY created_date DESC LIMIT 1",
                Integer.class);
        assertThat(processedCount).isEqualTo(1);
    }

    // ===== CLEANUP_UNVERIFIED_ACCOUNTS FUNCTION TESTS =====

    @Test
    void cleanupUnverifiedAccounts_WithOldUnverifiedAccounts_ShouldDeleteAccounts() throws SQLException {
        // Arrange - Create old unverified accounts that never logged in
        Long emailId1 = createCustomerEmail("old1@gmail.com", false);
        Long emailId2 = createCustomerEmail("old2@gmail.com", false);
        Long customerId1 = createCustomer("Old", "Unverified1", emailId1, null);
        Long customerId2 = createCustomer("Old", "Unverified2", emailId2, null);

        // Create accounts with old creation dates (40 days old - beyond default 30 day threshold)
        Long accountId1 = createCustomerAccountWithDate(customerId1, "old1@gmail.com", OffsetDateTime.now().minusDays(40));
        Long accountId2 = createCustomerAccountWithDate(customerId2, "old2@gmail.com", OffsetDateTime.now().minusDays(40));

        // Ensure accounts are unverified and never logged in
        jdbcTemplate.update("UPDATE customer_accounts SET verification_status = 'UNVERIFIED', last_login_at = NULL WHERE id IN (?, ?)",
                accountId1, accountId2);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_unverified_accounts()")) {
            stmt.execute();
        }

        // Assert - Accounts should be deleted (cascading to customers and emails)
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_accounts WHERE id IN (?, ?)",
                Integer.class, accountId1, accountId2);
        assertThat(accountCount).isEqualTo(0);

        // Verify audit record
        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT records_processed FROM job_execution_audit WHERE job_name = 'cleanup_unverified_accounts' " +
                        "AND execution_date = CURRENT_DATE ORDER BY created_date DESC LIMIT 1",
                Integer.class);
        assertThat(processedCount).isEqualTo(2);
    }

    @Test
    void cleanupUnverifiedAccounts_WithVerifiedAccounts_ShouldNotDelete() throws SQLException {
        // Arrange - Create old but verified account
        Long emailId = createCustomerEmail("verified@gmail.com", true);
        Long customerId = createCustomer("Verified", "Old", emailId, null);
        Long accountId = createCustomerAccountWithDate(customerId, "verified@gmail.com", OffsetDateTime.now().minusDays(40));

        // Set as email verified
        jdbcTemplate.update("UPDATE customer_accounts SET verification_status = 'EMAIL_VERIFIED' WHERE id = ?", accountId);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_unverified_accounts()")) {
            stmt.execute();
        }

        // Assert - Account should still exist
        Boolean accountExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM customer_accounts WHERE id = ?)",
                Boolean.class, accountId);
        assertThat(accountExists).isTrue();
    }

    @Test
    void cleanupUnverifiedAccounts_WithAccountsThatLoggedIn_ShouldNotDelete() throws SQLException {
        // Arrange - Create old unverified account that logged in
        Long emailId = createCustomerEmail("logged@gmail.com", false);
        Long customerId = createCustomer("Logged", "In", emailId, null);
        Long accountId = createCustomerAccountWithDate(customerId, "logged@gmail.com", OffsetDateTime.now().minusDays(40));

        // Set last login (even though unverified)
        updateAccountLastLogin(accountId, OffsetDateTime.now().minusDays(20));

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_unverified_accounts()")) {
            stmt.execute();
        }

        // Assert - Account should still exist (logged in users are kept even if unverified)
        Boolean accountExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM customer_accounts WHERE id = ?)",
                Boolean.class, accountId);
        assertThat(accountExists).isTrue();
    }

    // ===== CLEANUP_JOB_EXECUTION_AUDIT FUNCTION TESTS =====

    @Test
    void cleanupJobExecutionAudit_WithOldRecords_ShouldDeleteExpiredRecords() throws SQLException {
        // Arrange - Create old and recent job audit records
        createJobExecutionAuditRecord("recent_job", LocalDate.now().minusDays(30), true, 5, null); // Recent (within 90 days)
        createJobExecutionAuditRecord("old_job", LocalDate.now().minusDays(120), true, 10, null); // Old (beyond 90 day default)

        // Get initial count
        Integer initialCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_execution_audit",
                Integer.class);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT cleanup_job_execution_audit()")) {
            stmt.execute();
        }

        // Assert - Should have deleted old record, kept recent one
        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_execution_audit",
                Integer.class);

        // Should have deleted at least 1 record (the old one)
        assertThat(finalCount).isLessThan(initialCount);

        // Recent records should still exist
        Integer recentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_execution_audit WHERE execution_date >= CURRENT_DATE - INTERVAL '90 days'",
                Integer.class);
        assertThat(recentCount).isGreaterThan(0);
    }

    // ===== RUN_ALL_CLEANUP_JOBS FUNCTION TESTS =====

    @Test
    void runAllCleanupJobs_ShouldExecuteAllCleanupFunctions() throws SQLException {
        // Arrange - Create data for each cleanup function
        Long emailId = createCustomerEmail("master@gmail.com", true);
        Long customerId = createCustomer("Master", "Cleanup", emailId, null);
        Long accountId = createCustomerAccount(customerId, "master@gmail.com");

        // Create old data that should be cleaned up
        createPasswordResetToken(accountId, "old-token", OffsetDateTime.now().minusDays(10));
        createAccountStatusAuditRecord(accountId, "ACTIVE", "INACTIVE", OffsetDateTime.now().minusDays(400));

        // Create separate customer and email for the old unverified account
        Long oldEmailId = createCustomerEmail("old@gmail.com", false);
        Long oldCustomerId = createCustomer("Old", "Unverified", oldEmailId, null);
        Long oldAccountId = createCustomerAccountWithDate(oldCustomerId, "old@gmail.com", OffsetDateTime.now().minusDays(40));
        jdbcTemplate.update("UPDATE customer_accounts SET verification_status = 'UNVERIFIED', last_login_at = NULL WHERE id = ?", oldAccountId);

        // Act
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT run_all_cleanup_jobs()")) {
            stmt.execute();
        }

        // Assert - Master job should be logged
        Boolean masterJobExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM job_execution_audit WHERE job_name = 'run_all_cleanup_jobs' AND execution_date = CURRENT_DATE)",
                Boolean.class);
        assertThat(masterJobExists).isTrue();

        // Individual cleanup jobs should also be logged
        Integer cleanupJobCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT job_name) FROM job_execution_audit WHERE execution_date = CURRENT_DATE " +
                        "AND job_name IN ('cleanup_password_reset_tokens', 'cleanup_account_status_audit', 'cleanup_unverified_accounts')",
                Integer.class);
        assertThat(cleanupJobCount).isEqualTo(3);
    }

    @Test
    void runAllCleanupJobs_WithMissingConfiguration_ShouldLogPartialFailure() throws SQLException {
        // Arrange - Remove one configuration to cause partial failure
        jdbcTemplate.update("DELETE FROM business_config WHERE key = 'password_reset_token_cleanup_days'");

        try {
            // Act
            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("SELECT run_all_cleanup_jobs()")) {
                stmt.execute();
            }

            // Assert - Master job should log partial failure
            String errorMessage = jdbcTemplate.queryForObject(
                    "SELECT error_message FROM job_execution_audit WHERE job_name = 'run_all_cleanup_jobs' " +
                            "AND execution_date = CURRENT_DATE ORDER BY created_date DESC LIMIT 1",
                    String.class);
            assertThat(errorMessage).contains("cleanup job(s) failed");

        } finally {
            // Restore configuration
            jdbcTemplate.update(
                    "INSERT INTO business_config (key, value, description) VALUES (?, ?, ?)",
                    "password_reset_token_cleanup_days", "7", "Days to keep password reset tokens after creation");
        }
    }

    // ===== HELPER METHODS =====

    private Long createCustomerEmail(String email, boolean verified) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customer_emails (email, is_verified) VALUES (?, ?) RETURNING id",
                Long.class, email, verified);
    }

    private Long createCustomerPhone(String phone, boolean verified) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customer_phones (phone, is_verified) VALUES (?, ?) RETURNING id",
                Long.class, phone, verified);
    }

    private Long createCustomer(String firstName, String lastName, Long emailId, Long phoneId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customers (first_name, last_name, email_id, phone_id) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, firstName, lastName, emailId, phoneId);
    }

    private Long createCustomerAccount(Long customerId, String username) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customer_accounts (customer_id, username, password, activity_status, verification_status) " +
                        "VALUES (?, ?, ?, ?::customer_account_activity_status_enum, ?::customer_account_verification_status_enum) RETURNING id",
                Long.class, customerId, username, "hashedPassword123", "ACTIVE", "UNVERIFIED");
    }

    private Long createCustomerAccountWithDate(Long customerId, String username, OffsetDateTime createdDate) {
        Long accountId = createCustomerAccount(customerId, username);
        // Update created date manually
        jdbcTemplate.update("UPDATE customer_accounts SET created_date = ? WHERE id = ?", createdDate, accountId);
        return accountId;
    }

    private Long createPasswordResetToken(Long accountId, String token, OffsetDateTime createdDate) {
        Long tokenId = jdbcTemplate.queryForObject(
                "INSERT INTO password_reset_tokens (customer_account_id, token, expires_at, used) " +
                        "VALUES (?, ?, ?, false) RETURNING id",
                Long.class, accountId, token, OffsetDateTime.now().plusHours(24));
        // Update created date manually
        jdbcTemplate.update("UPDATE password_reset_tokens SET created_date = ? WHERE id = ?", createdDate, tokenId);
        return tokenId;
    }

    private void createAccountStatusAuditRecord(Long accountId, String oldStatus, String newStatus, OffsetDateTime createdDate) {
        Long auditId = jdbcTemplate.queryForObject(
                "INSERT INTO account_status_audit (account_id, old_status, new_status) " +
                        "VALUES (?, ?::customer_account_activity_status_enum, ?::customer_account_activity_status_enum) RETURNING id",
                Long.class, accountId, oldStatus, newStatus);
        // Update created date manually
        jdbcTemplate.update("UPDATE account_status_audit SET created_date = ? WHERE id = ?", createdDate, auditId);
    }

    private void createJobExecutionAuditRecord(String jobName, LocalDate executionDate, boolean success, int recordsProcessed, String errorMessage) {
        Long auditId = jdbcTemplate.queryForObject(
                "INSERT INTO job_execution_audit (job_name, execution_date, success, records_processed, error_message) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, jobName, executionDate, success, recordsProcessed, errorMessage);
        // Update created date to match execution date
        jdbcTemplate.update("UPDATE job_execution_audit SET created_date = ? WHERE id = ?",
                executionDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()), auditId);
    }

    private void updateAccountLastLogin(Long accountId, OffsetDateTime lastLogin) {
        jdbcTemplate.update(
                "UPDATE customer_accounts SET last_login_at = ? WHERE id = ?",
                lastLogin, accountId);
    }

    private void updateAccountActivityStatus(Long accountId, String status) {
        jdbcTemplate.update(
                "UPDATE customer_accounts SET activity_status = ?::customer_account_activity_status_enum WHERE id = ?",
                status, accountId);
    }
}