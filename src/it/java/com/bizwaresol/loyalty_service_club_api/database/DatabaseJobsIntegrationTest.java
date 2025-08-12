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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test-postgres")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false"
})
@Component
public class DatabaseJobsIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        // Clean all tables EXCEPT business_config (preserve seed data)
        jdbcTemplate.execute("TRUNCATE TABLE password_reset_tokens CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE otp_tokens CASCADE");
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
        jdbcTemplate.execute("ALTER SEQUENCE otp_tokens_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE account_status_audit_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE job_execution_audit_id_seq RESTART WITH 1");

        // Restore business configuration to known good state (don't truncate!)
        restoreBusinessConfigToDefaults();
    }

    private void restoreBusinessConfigToDefaults() {
        Map<String, String> defaultConfigs = Map.ofEntries(
                Map.entry("account_inactivity_days", "60"),
                Map.entry("inactivity_batch_size", "1000"),
                Map.entry("otp_token_cleanup_days", "7"),
                Map.entry("job_execution_audit_cleanup_days", "90"),
                Map.entry("account_status_audit_cleanup_days", "365"),
                Map.entry("unverified_account_cleanup_days", "30"),
                Map.entry("cleanup_batch_size", "500"),
                Map.entry("otp_expiry_minutes", "10"),
                Map.entry("otp_max_attempts", "3"),
                Map.entry("otp_resend_cooldown_minutes", "1"),
                Map.entry("otp_rate_limit_per_hour", "10"),
                Map.entry("password_reset_token_cleanup_days", "1")
        );

        for (Map.Entry<String, String> config : defaultConfigs.entrySet()) {
            jdbcTemplate.update(
                    "UPDATE business_config SET value = ? WHERE key = ?",
                    config.getValue(), config.getKey()
            );
        }
        ensureRequiredConfigsExist();
    }

    private void ensureRequiredConfigsExist() {
        Map<String, String> requiredConfigs = Map.ofEntries(
                Map.entry("account_inactivity_days", "60"),
                Map.entry("inactivity_batch_size", "1000"),
                Map.entry("otp_token_cleanup_days", "7"),
                Map.entry("job_execution_audit_cleanup_days", "90"),
                Map.entry("account_status_audit_cleanup_days", "365"),
                Map.entry("unverified_account_cleanup_days", "30"),
                Map.entry("cleanup_batch_size", "500"),
                Map.entry("otp_expiry_minutes", "10"),
                Map.entry("otp_max_attempts", "3"),
                Map.entry("otp_resend_cooldown_minutes", "1"),
                Map.entry("otp_rate_limit_per_hour", "10"),
                Map.entry("password_reset_token_cleanup_days", "1")
        );

        for (Map.Entry<String, String> config : requiredConfigs.entrySet()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM business_config WHERE key = ?",
                    Integer.class, config.getKey());

            if (count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO business_config (key, value, description) VALUES (?, ?, ?)",
                        config.getKey(),
                        config.getValue(),
                        "Required configuration for " + config.getKey());
            }
        }
    }

    // ===== JOB FUNCTION AVAILABILITY TESTS =====

    @Test
    void jobFunction_ShouldExistInDatabase() {
        String[] jobFunctions = {
                "mark_inactive_accounts_batched",
                "cleanup_otp_tokens",
                "cleanup_password_reset_tokens",
                "cleanup_job_execution_audit",
                "cleanup_account_status_audit",
                "cleanup_unverified_accounts",
                "run_all_cleanup_jobs"
        };

        try (Connection conn = dataSource.getConnection()) {
            for (String functionName : jobFunctions) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT proname FROM pg_proc WHERE proname = ?")) {
                    stmt.setString(1, functionName);
                    ResultSet rs = stmt.executeQuery();
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("proname")).isEqualTo(functionName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check function existence: " + e.getMessage(), e);
        }
    }

    // ===== CLEANUP JOBS TESTS =====

    @Test
    void cleanupPasswordResetTokens_WithOldTokens_ShouldProcessCorrectly() {
        // Arrange
        Long accountId1 = setupAccountForTokenTests("resettoken1@test.com");
        Long accountId2 = setupAccountForTokenTests("resettoken2@test.com");

        Long oldTokenId = createPasswordResetToken(accountId1, UUID.randomUUID().toString());
        jdbcTemplate.update("UPDATE password_reset_tokens SET created_date = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(2), oldTokenId);

        createPasswordResetToken(accountId2, UUID.randomUUID().toString());

        // Act
        callJobFunction("cleanup_password_reset_tokens");

        // Assert
        Integer processedCount = getLatestAuditRecordCount("cleanup_password_reset_tokens");
        assertThat(processedCount).isEqualTo(1);
        Integer remainingTokens = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_tokens", Integer.class);
        assertThat(remainingTokens).isEqualTo(1);
    }

    @Test
    void cleanupOtpTokens_WithOldTokens_ShouldProcessCorrectly() {
        // Arrange
        Long emailId = createCustomerEmail("cleanup@gmail.com", true);
        createOtpToken(emailId, null, "123456", "PASSWORD_RESET", "EMAIL", OffsetDateTime.now().minusDays(10));
        createOtpToken(emailId, null, "789012", "EMAIL_VERIFICATION", "EMAIL", OffsetDateTime.now().minusDays(8));
        createOtpToken(emailId, null, "345678", "EMAIL_VERIFICATION", "EMAIL", OffsetDateTime.now().minusDays(2));

        // Act
        callJobFunction("cleanup_otp_tokens");

        // Assert
        Integer processedCount = getLatestAuditRecordCount("cleanup_otp_tokens");
        assertThat(processedCount).isEqualTo(2);
        Integer remainingTokens = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM otp_tokens", Integer.class);
        assertThat(remainingTokens).isEqualTo(1);
    }

    @Test
    void runAllCleanupJobs_ShouldExecuteAllCleanupFunctions() {
        // Arrange
        setupTestDataForCleanupJobs();

        // Act
        callJobFunction("run_all_cleanup_jobs");

        // Assert
        Boolean masterJobExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM job_execution_audit WHERE job_name = 'run_all_cleanup_jobs' AND execution_date = CURRENT_DATE)",
                Boolean.class);
        assertThat(masterJobExists).isTrue();

        List<String> expectedJobs = List.of(
                "cleanup_otp_tokens",
                "cleanup_password_reset_tokens",
                "cleanup_account_status_audit",
                "cleanup_unverified_accounts"
        );

        for (String jobName : expectedJobs) {
            Boolean jobExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM job_execution_audit WHERE job_name = ? AND execution_date = CURRENT_DATE)",
                    Boolean.class, jobName);
            assertThat(jobExists).isTrue();
        }
    }

    // ===== INACTIVITY JOB TESTS =====

    @Test
    void markInactiveAccounts_WithOldAccounts_ShouldMarkThemInactive() {
        // Arrange
        setupOldAccountsForTesting(3);
        setupOldAccountsForTesting(2, false);

        // Act
        callJobFunction("mark_inactive_accounts_batched");

        // Assert
        Integer inactiveCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_accounts WHERE activity_status = 'INACTIVE'", Integer.class);
        assertThat(inactiveCount).isEqualTo(3);
        Integer processedCount = getLatestAuditRecordCount("mark_inactive_accounts");
        assertThat(processedCount).isEqualTo(3);
    }

    // ===== JOB EXECUTION AUDIT TESTS =====

    @Test
    void jobExecutionAudit_OnSuccessfulExecution_ShouldLogSuccess() {
        // Arrange
        setupOldAccountsForTesting(2);

        // Act
        callJobFunction("mark_inactive_accounts_batched");

        // Assert
        Map<String, Object> auditRecord = getLatestAuditRecord("mark_inactive_accounts");
        assertThat(auditRecord.get("success")).isEqualTo(true);
        assertThat(auditRecord.get("records_processed")).isEqualTo(2);
        assertThat(auditRecord.get("error_message")).isNull();
    }

    @Test
    void jobExecutionAudit_OnFailedExecution_ShouldLogFailure() {
        // Arrange
        jdbcTemplate.update("DELETE FROM business_config WHERE key = 'account_inactivity_days'");

        // Act
        // The function will now log the error but not throw an exception that rolls back the transaction
        callJobFunction("mark_inactive_accounts_batched");

        // Assert
        Map<String, Object> auditRecord = getLatestAuditRecord("mark_inactive_accounts");
        assertThat(auditRecord.get("success")).isEqualTo(false);
        assertThat(auditRecord.get("error_message")).asString().contains("Configuration missing: account_inactivity_days");
    }

    // ===== HELPER METHODS =====

    private void callJobFunction(String functionName) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall("SELECT " + functionName + "()")) {
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Job function '" + functionName + "' is not callable: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getLatestAuditRecord(String jobName) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM job_execution_audit WHERE job_name = ? " +
                        "AND execution_date = CURRENT_DATE ORDER BY created_date DESC LIMIT 1",
                jobName);
    }

    private Integer getLatestAuditRecordCount(String jobName) {
        return (Integer) getLatestAuditRecord(jobName).get("records_processed");
    }

    private Long setupAccountForTokenTests(String email) {
        Long emailId = createCustomerEmail(email, true);
        Long customerId = createCustomer("Token", "User", emailId, null);
        return createCustomerAccount(customerId, email);
    }

    private void setupTestDataForCleanupJobs() {
        Long emailId = createCustomerEmail("cleanup@gmail.com", true);
        Long customerId = createCustomer("Cleanup", "Test", emailId, null);
        Long accountId = createCustomerAccount(customerId, "cleanup@gmail.com");

        createOtpToken(emailId, null, "123456", "PASSWORD_RESET", "EMAIL", OffsetDateTime.now().minusDays(10));

        Long oldTokenId = createPasswordResetToken(accountId, UUID.randomUUID().toString());
        jdbcTemplate.update("UPDATE password_reset_tokens SET created_date = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(2), oldTokenId);

        createAccountStatusAuditRecord(accountId, "ACTIVE", "INACTIVE", OffsetDateTime.now().minusDays(400));

        Long unverifiedEmailId = createCustomerEmail("unverified@gmail.com", false);
        Long unverifiedCustomerId = createCustomer("Unverified", "User", unverifiedEmailId, null);
        Long unverifiedAccountId = createCustomerAccountWithDate(unverifiedCustomerId, "unverified@gmail.com",
                OffsetDateTime.now().minusDays(40));
        jdbcTemplate.update("UPDATE customer_accounts SET verification_status = 'UNVERIFIED', last_login_at = NULL WHERE id = ?",
                unverifiedAccountId);
    }

    private void setupOldAccountsForTesting(int count) {
        setupOldAccountsForTesting(count, true);
    }

    private void setupOldAccountsForTesting(int count, boolean isOld) {
        OffsetDateTime loginDate = isOld
                ? OffsetDateTime.now().minusDays(90)
                : OffsetDateTime.now().minusDays(10);

        for (int i = 0; i < count; i++) {
            String email = (isOld ? "old" : "recent") + i + "_" + System.nanoTime() + "@test.com";
            Long emailId = createCustomerEmail(email, true);
            Long customerId = createCustomer("Test", "User", emailId, null);
            Long accountId = createCustomerAccount(customerId, email);
            jdbcTemplate.update("UPDATE customer_accounts SET last_login_at = ? WHERE id = ?", loginDate, accountId);
        }
    }

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
        jdbcTemplate.update("UPDATE customer_accounts SET created_date = ? WHERE id = ?", createdDate, accountId);
        return accountId;
    }

    private void createOtpToken(Long emailId, Long phoneId, String otpCode, String purpose, String deliveryMethod, OffsetDateTime createdDate) {
        Long tokenId = jdbcTemplate.queryForObject(
                "INSERT INTO otp_tokens (customer_email_id, customer_phone_id, otp_code, purpose, delivery_method, expires_at) " +
                        "VALUES (?, ?, ?, ?::otp_purpose_enum, ?::otp_delivery_method_enum, ?) RETURNING id",
                Long.class, emailId, phoneId, otpCode, purpose, deliveryMethod,
                OffsetDateTime.now().plusMinutes(10));
        jdbcTemplate.update("UPDATE otp_tokens SET created_date = ? WHERE id = ?", createdDate, tokenId);
    }

    private Long createPasswordResetToken(Long accountId, String token) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO password_reset_tokens (customer_account_id, token, expires_at) " +
                        "VALUES (?, ?, ?) RETURNING id",
                Long.class, accountId, token, OffsetDateTime.now().plusMinutes(10));
    }

    private void createAccountStatusAuditRecord(Long accountId, String oldStatus, String newStatus, OffsetDateTime createdDate) {
        jdbcTemplate.update(
                "INSERT INTO account_status_audit (account_id, old_status, new_status, created_date) " +
                        "VALUES (?, ?::customer_account_activity_status_enum, ?::customer_account_activity_status_enum, ?)",
                accountId, oldStatus, newStatus, createdDate);
    }
}
