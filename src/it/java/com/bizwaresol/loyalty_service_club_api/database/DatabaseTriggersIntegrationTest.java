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
import java.sql.Timestamp;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test-postgres")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false"
})
@Component
public class DatabaseTriggersIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        // Clean all tables except business_config
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
    }

    // ===== AUDIT TRIGGERS TESTS =====

    @Test
    void auditTriggers_OnInsert_ShouldSetCreatedAndModifiedDates() throws SQLException {
        // Act - Insert new email (triggers audit_customer_emails)
        Long emailId = jdbcTemplate.queryForObject(
                "INSERT INTO customer_emails (email, is_verified) VALUES (?, ?) RETURNING id",
                Long.class, "audit@gmail.com", false);

        // Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT created_date, last_modified_date FROM customer_emails WHERE id = ?")) {

            stmt.setLong(1, emailId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            Timestamp createdDate = rs.getTimestamp("created_date");
            Timestamp modifiedDate = rs.getTimestamp("last_modified_date");

            assertThat(createdDate).isNotNull();
            assertThat(modifiedDate).isNotNull();
            assertThat(createdDate).isEqualTo(modifiedDate); // Should be same on insert
        }
    }

    @Test
    void auditTriggers_OnUpdate_ShouldUpdateModifiedDateOnly() throws SQLException {
        // Arrange - Insert email first
        Long emailId = createCustomerEmail("update@gmail.com", false);

        // Get initial timestamps
        Timestamp initialCreated = jdbcTemplate.queryForObject(
                "SELECT created_date FROM customer_emails WHERE id = ?",
                Timestamp.class, emailId);
        Timestamp initialModified = jdbcTemplate.queryForObject(
                "SELECT last_modified_date FROM customer_emails WHERE id = ?",
                Timestamp.class, emailId);

        // Wait a moment to ensure timestamp difference
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        // Act - Update the email
        jdbcTemplate.update(
                "UPDATE customer_emails SET is_verified = true WHERE id = ?", emailId);

        // Assert
        Timestamp finalCreated = jdbcTemplate.queryForObject(
                "SELECT created_date FROM customer_emails WHERE id = ?",
                Timestamp.class, emailId);
        Timestamp finalModified = jdbcTemplate.queryForObject(
                "SELECT last_modified_date FROM customer_emails WHERE id = ?",
                Timestamp.class, emailId);

        assertThat(finalCreated).isEqualTo(initialCreated); // Created date unchanged
        assertThat(finalModified).isAfter(initialModified); // Modified date updated
    }

    @Test
    void auditTriggers_OnAllTables_ShouldWorkCorrectly() throws SQLException {
        // Test audit triggers on all tables with audit fields

        // Customer emails
        Long emailId = createCustomerEmail("test@gmail.com", false);
        assertAuditFieldsExist("customer_emails", emailId);

        // Customer phones
        Long phoneId = createCustomerPhone("+381611111111", false);
        assertAuditFieldsExist("customer_phones", phoneId);

        // Customers
        Long customerId = createCustomer("Test", "User", emailId, phoneId);
        assertAuditFieldsExist("customers", customerId);

        // Customer accounts
        Long accountId = createCustomerAccount(customerId, "test@gmail.com");
        assertAuditFieldsExist("customer_accounts", accountId);

        // OTP tokens
        Long otpTokenId = createOTPToken(emailId, null, "123456", "PASSWORD_RESET", "EMAIL");
        assertAuditFieldsExist("otp_tokens", otpTokenId);

        // Password reset tokens (NEWLY ADDED)
        Long passwordResetTokenId = createPasswordResetToken(accountId, "a-valid-uuid");
        assertAuditFieldsExist("password_reset_tokens", passwordResetTokenId);
    }

    // ===== USERNAME SETTING TRIGGER TESTS =====

    @Test
    void usernameSettingTrigger_WithEmailContact_ShouldSetEmailAsUsername() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("username@gmail.com", true);
        Long customerId = createCustomer("Username", "Test", emailId, null);

        // Act - Create account (triggers set_username_on_account_creation)
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO customer_accounts (customer_id, password, activity_status, verification_status) " +
                        "VALUES (?, 'hashedPassword123', 'ACTIVE', 'UNVERIFIED') RETURNING id",
                Long.class, customerId);

        // Assert
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("username@gmail.com");
    }

    @Test
    void usernameSettingTrigger_WithPhoneContact_ShouldSetPhoneAsUsername() throws SQLException {
        // Arrange
        Long phoneId = createCustomerPhone("+381622222222", true);
        Long customerId = createCustomer("Phone", "Test", null, phoneId);

        // Act - Create account
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO customer_accounts (customer_id, password, activity_status, verification_status) " +
                        "VALUES (?, 'hashedPassword123', 'ACTIVE', 'UNVERIFIED') RETURNING id",
                Long.class, customerId);

        // Assert
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("+381622222222");
    }

    @Test
    void usernameSettingTrigger_WithBothContacts_ShouldPreferEmail() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("prefer@gmail.com", true);
        Long phoneId = createCustomerPhone("+381633333333", true);
        Long customerId = createCustomer("Prefer", "Email", emailId, phoneId);

        // Act - Create account
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO customer_accounts (customer_id, password, activity_status, verification_status) " +
                        "VALUES (?, 'hashedPassword123', 'ACTIVE', 'UNVERIFIED') RETURNING id",
                Long.class, customerId);

        // Assert
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("prefer@gmail.com"); // Email preferred over phone
    }

    @Test
    void usernameSettingTrigger_WithNoContacts_ShouldThrowException() {
        // Arrange
        Long customerId = createCustomer("No", "Contact", null, null);

        // Act & Assert
        assertThatThrownBy(() -> {
            jdbcTemplate.queryForObject(
                    "INSERT INTO customer_accounts (customer_id, password, activity_status, verification_status) " +
                            "VALUES (?, 'hashedPassword123', 'ACTIVE', 'UNVERIFIED') RETURNING id",
                    Long.class, customerId);
        }).hasMessageContaining("Cannot create account: customer has no email or phone contact information");
    }

    // ===== CONTACT CHANGE TRIGGER TESTS =====

    @Test
    void contactChangeTrigger_WhenEmailChanges_ShouldUpdateUsername() throws SQLException {
        // Arrange
        Long emailId1 = createCustomerEmail("original@gmail.com", true);
        Long emailId2 = createCustomerEmail("updated@gmail.com", true);
        Long customerId = createCustomer("Change", "Email", emailId1, null);
        Long accountId = createCustomerAccount(customerId, "original@gmail.com");

        // Act - Change customer's email (triggers update_username_on_contact_change)
        jdbcTemplate.update(
                "UPDATE customers SET email_id = ? WHERE id = ?", emailId2, customerId);

        // Assert
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("updated@gmail.com");
    }

    @Test
    void contactChangeTrigger_WhenPhoneChanges_ShouldUpdateUsername() throws SQLException {
        // Arrange
        Long phoneId1 = createCustomerPhone("+381611111111", true);
        Long phoneId2 = createCustomerPhone("+381622222222", true);
        Long customerId = createCustomer("Change", "Phone", null, phoneId1);
        Long accountId = createCustomerAccount(customerId, "+381611111111");

        // Act - Change customer's phone
        jdbcTemplate.update(
                "UPDATE customers SET phone_id = ? WHERE id = ?", phoneId2, customerId);

        // Assert
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("+381622222222");
    }

    @Test
    void contactChangeTrigger_WhenAddingEmailToPhoneOnlyCustomer_ShouldSwitchToEmail() throws SQLException {
        // Arrange - Customer with phone only
        Long phoneId = createCustomerPhone("+381644444444", true);
        Long customerId = createCustomer("Add", "Email", null, phoneId);
        Long accountId = createCustomerAccount(customerId, "+381644444444");

        // Act - Add email to customer
        Long emailId = createCustomerEmail("newemail@gmail.com", true);
        jdbcTemplate.update(
                "UPDATE customers SET email_id = ? WHERE id = ?", emailId, customerId);

        // Assert - Username should switch to email (higher priority)
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("newemail@gmail.com");
    }

    @Test
    void contactChangeTrigger_WhenRemovingContact_ShouldUpdateToRemaining() throws SQLException {
        // Arrange - Customer with both email and phone
        Long emailId = createCustomerEmail("remove@gmail.com", true);
        Long phoneId = createCustomerPhone("+381655555555", true);
        Long customerId = createCustomer("Remove", "Email", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "remove@gmail.com");

        // Act - Remove email, leaving only phone
        jdbcTemplate.update(
                "UPDATE customers SET email_id = NULL WHERE id = ?", customerId);

        // Assert - Username should switch to phone
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM customer_accounts WHERE id = ?",
                String.class, accountId);

        assertThat(username).isEqualTo("+381655555555");
    }

    // ===== VERIFICATION STATUS TRIGGER TESTS =====

    @Test
    void verificationStatusTrigger_WhenEmailVerified_ShouldUpdateAccountStatus() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("verify@gmail.com", false);
        Long customerId = createCustomer("Verify", "Email", emailId, null);
        Long accountId = createCustomerAccount(customerId, "verify@gmail.com");

        // Verify initial status is UNVERIFIED
        String initialStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);
        assertThat(initialStatus).isEqualTo("UNVERIFIED");

        // Act - Verify email (triggers update_account_status_on_email_verification)
        jdbcTemplate.update(
                "UPDATE customer_emails SET is_verified = true WHERE id = ?", emailId);

        // Assert
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);
        assertThat(finalStatus).isEqualTo("EMAIL_VERIFIED");
    }

    @Test
    void verificationStatusTrigger_WhenPhoneVerified_ShouldUpdateAccountStatus() throws SQLException {
        // Arrange
        Long phoneId = createCustomerPhone("+381666666666", false);
        Long customerId = createCustomer("Verify", "Phone", null, phoneId);
        Long accountId = createCustomerAccount(customerId, "+381666666666");

        // Act - Verify phone (triggers update_account_status_on_phone_verification)
        jdbcTemplate.update(
                "UPDATE customer_phones SET is_verified = true WHERE id = ?", phoneId);

        // Assert
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);
        assertThat(finalStatus).isEqualTo("PHONE_VERIFIED");
    }

    @Test
    void verificationStatusTrigger_WhenBothVerified_ShouldUpdateToFullyVerified() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("both@gmail.com", false);
        Long phoneId = createCustomerPhone("+381677777777", false);
        Long customerId = createCustomer("Both", "Verified", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "both@gmail.com");

        // Act - Verify both email and phone
        jdbcTemplate.update(
                "UPDATE customer_emails SET is_verified = true WHERE id = ?", emailId);
        jdbcTemplate.update(
                "UPDATE customer_phones SET is_verified = true WHERE id = ?", phoneId);

        // Assert
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);
        assertThat(finalStatus).isEqualTo("FULLY_VERIFIED");
    }

    @Test
    void verificationStatusTrigger_WhenUnverifyingEmail_ShouldDowngradeStatus() throws SQLException {
        // Arrange - Start with verified email
        Long emailId = createCustomerEmail("downgrade@gmail.com", true);
        Long phoneId = createCustomerPhone("+381688888888", false);
        Long customerId = createCustomer("Down", "Grade", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "downgrade@gmail.com");

        // Manually set account to EMAIL_VERIFIED
        jdbcTemplate.update(
                "UPDATE customer_accounts SET verification_status = 'EMAIL_VERIFIED' WHERE id = ?", accountId);

        // Act - Unverify email
        jdbcTemplate.update(
                "UPDATE customer_emails SET is_verified = false WHERE id = ?", emailId);

        // Assert - Should downgrade to UNVERIFIED
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?",
                String.class, accountId);
        assertThat(finalStatus).isEqualTo("UNVERIFIED");
    }

    // ===== ACCOUNT STATUS AUDIT TRIGGER TESTS =====

    @Test
    void accountStatusAuditTrigger_WhenActivityStatusChanges_ShouldCreateAuditRecord() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("audit@gmail.com", true);
        Long customerId = createCustomer("Audit", "Test", emailId, null);
        Long accountId = createCustomerAccount(customerId, "audit@gmail.com");

        // Act - Change activity status (triggers audit_account_status_changes)
        jdbcTemplate.update(
                "UPDATE customer_accounts SET activity_status = 'INACTIVE'::customer_account_activity_status_enum WHERE id = ?",
                accountId);

        // Assert - Audit record should be created
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM account_status_audit WHERE account_id = ?")) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("account_id")).isEqualTo(accountId);
            assertThat(rs.getString("old_status")).isEqualTo("ACTIVE");
            assertThat(rs.getString("new_status")).isEqualTo("INACTIVE");
            assertThat(rs.getTimestamp("created_date")).isNotNull();
        }
    }

    @Test
    void accountStatusAuditTrigger_WhenOtherFieldsChange_ShouldNotCreateAuditRecord() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("noaudit@gmail.com", true);
        Long customerId = createCustomer("No", "Audit", emailId, null);
        Long accountId = createCustomerAccount(customerId, "noaudit@gmail.com");

        // Act - Change verification status (should not trigger audit)
        jdbcTemplate.update(
                "UPDATE customer_accounts SET verification_status = 'EMAIL_VERIFIED'::customer_account_verification_status_enum WHERE id = ?",
                accountId);

        // Assert - No audit record should be created
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_status_audit WHERE account_id = ?",
                Integer.class, accountId);

        assertThat(auditCount).isEqualTo(0);
    }

    @Test
    void accountStatusAuditTrigger_WithMultipleChanges_ShouldCreateMultipleRecords() throws SQLException {
        // Arrange
        Long emailId = createCustomerEmail("multiple@gmail.com", true);
        Long customerId = createCustomer("Multiple", "Changes", emailId, null);
        Long accountId = createCustomerAccount(customerId, "multiple@gmail.com");

        // Act - Multiple status changes
        jdbcTemplate.update(
                "UPDATE customer_accounts SET activity_status = 'INACTIVE'::customer_account_activity_status_enum WHERE id = ?",
                accountId);
        jdbcTemplate.update(
                "UPDATE customer_accounts SET activity_status = 'SUSPENDED'::customer_account_activity_status_enum WHERE id = ?",
                accountId);
        jdbcTemplate.update(
                "UPDATE customer_accounts SET activity_status = 'ACTIVE'::customer_account_activity_status_enum WHERE id = ?",
                accountId);

        // Assert - Should have 3 audit records
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_status_audit WHERE account_id = ?",
                Integer.class, accountId);

        assertThat(auditCount).isEqualTo(3);

        // Verify the sequence of changes
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT old_status, new_status FROM account_status_audit WHERE account_id = ? ORDER BY created_date")) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            // First change: ACTIVE -> INACTIVE
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("old_status")).isEqualTo("ACTIVE");
            assertThat(rs.getString("new_status")).isEqualTo("INACTIVE");

            // Second change: INACTIVE -> SUSPENDED
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("old_status")).isEqualTo("INACTIVE");
            assertThat(rs.getString("new_status")).isEqualTo("SUSPENDED");

            // Third change: SUSPENDED -> ACTIVE
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("old_status")).isEqualTo("SUSPENDED");
            assertThat(rs.getString("new_status")).isEqualTo("ACTIVE");
        }
    }

    // ===== OTP VERIFICATION TRIGGER TESTS (NEWLY ADDED) =====

    @Test
    void otpVerificationTrigger_WhenEmailOtpIsUsed_ShouldVerifyEmailAndAccount() {
        // Arrange
        Long emailId = createCustomerEmail("otp-verify-email@gmail.com", false);
        Long customerId = createCustomer("Otp", "Verify", emailId, null);
        Long accountId = createCustomerAccount(customerId, "otp-verify-email@gmail.com");
        Long otpTokenId = createOTPToken(emailId, null, "111222", "EMAIL_VERIFICATION", "EMAIL");

        // Act: Mark the OTP as used, which should fire the new trigger
        markOTPTokenAsUsed(otpTokenId);

        // Assert: Check that the email is now verified
        Boolean emailVerified = jdbcTemplate.queryForObject(
                "SELECT is_verified FROM customer_emails WHERE id = ?", Boolean.class, emailId);
        assertThat(emailVerified).isTrue();

        // Assert: Check that the account status is now EMAIL_VERIFIED due to the trigger chain
        String accountStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?", String.class, accountId);
        assertThat(accountStatus).isEqualTo("EMAIL_VERIFIED");
    }

    @Test
    void otpVerificationTrigger_WhenPhoneOtpIsUsed_ShouldVerifyPhoneAndAccount() {
        // Arrange
        Long phoneId = createCustomerPhone("+381699999999", false);
        Long customerId = createCustomer("Otp", "Verify", null, phoneId);
        Long accountId = createCustomerAccount(customerId, "+381699999999");
        Long otpTokenId = createOTPToken(null, phoneId, "333444", "PHONE_VERIFICATION", "SMS");

        // Act: Mark the OTP as used
        markOTPTokenAsUsed(otpTokenId);

        // Assert: Check that the phone is now verified
        Boolean phoneVerified = jdbcTemplate.queryForObject(
                "SELECT is_verified FROM customer_phones WHERE id = ?", Boolean.class, phoneId);
        assertThat(phoneVerified).isTrue();

        // Assert: Check that the account status is now PHONE_VERIFIED
        String accountStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?", String.class, accountId);
        assertThat(accountStatus).isEqualTo("PHONE_VERIFIED");
    }

    @Test
    void otpVerificationTrigger_WhenPasswordResetOtpIsUsed_ShouldNotChangeVerificationStatus() {
        // Arrange
        Long emailId = createCustomerEmail("otp-password@gmail.com", false);
        Long customerId = createCustomer("Otp", "Password", emailId, null);
        Long accountId = createCustomerAccount(customerId, "otp-password@gmail.com");
        Long otpTokenId = createOTPToken(emailId, null, "555666", "PASSWORD_RESET", "EMAIL");

        // Act: Mark the password reset OTP as used
        markOTPTokenAsUsed(otpTokenId);

        // Assert: The email should NOT be verified
        Boolean emailVerified = jdbcTemplate.queryForObject(
                "SELECT is_verified FROM customer_emails WHERE id = ?", Boolean.class, emailId);
        assertThat(emailVerified).isFalse();

        // Assert: The account status should remain UNVERIFIED
        String accountStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?", String.class, accountId);
        assertThat(accountStatus).isEqualTo("UNVERIFIED");
    }

    @Test
    void otpVerificationTrigger_WhenContactIsAlreadyVerified_ShouldNotChangeStatus() {
        // Arrange: Email is already verified, and account status reflects this
        Long emailId = createCustomerEmail("otp-already-verified@gmail.com", true);
        Long customerId = createCustomer("Otp", "Verified", emailId, null);
        Long accountId = createCustomerAccount(customerId, "otp-already-verified@gmail.com");
        jdbcTemplate.update("UPDATE customer_accounts SET verification_status = 'EMAIL_VERIFIED' WHERE id = ?", accountId);
        Long otpTokenId = createOTPToken(emailId, null, "777888", "EMAIL_VERIFICATION", "EMAIL");

        // Act: Use another verification token
        markOTPTokenAsUsed(otpTokenId);

        // Assert: The email is still verified
        Boolean emailVerified = jdbcTemplate.queryForObject(
                "SELECT is_verified FROM customer_emails WHERE id = ?", Boolean.class, emailId);
        assertThat(emailVerified).isTrue();

        // Assert: The account status remains EMAIL_VERIFIED without error
        String accountStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM customer_accounts WHERE id = ?", String.class, accountId);
        assertThat(accountStatus).isEqualTo("EMAIL_VERIFIED");
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
                        "VALUES (?, ?, 'hashedPassword123', 'ACTIVE', 'UNVERIFIED') RETURNING id",
                Long.class, customerId, username);
    }

    private Long createOTPToken(Long customerEmailId, Long customerPhoneId, String otpCode, String purpose, String deliveryMethod) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO otp_tokens (customer_email_id, customer_phone_id, otp_code, purpose, delivery_method, expires_at) " +
                        "VALUES (?, ?, ?, ?::otp_purpose_enum, ?::otp_delivery_method_enum, ?) RETURNING id",
                Long.class, customerEmailId, customerPhoneId, otpCode, purpose, deliveryMethod,
                OffsetDateTime.now().plusMinutes(10)); // 10 minute expiry
    }

    private void markOTPTokenAsUsed(Long tokenId) {
        jdbcTemplate.update(
                "UPDATE otp_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?",
                tokenId);
    }

    private void assertAuditFieldsExist(String tableName, Long recordId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT created_date, last_modified_date FROM " + tableName + " WHERE id = ?")) {

            stmt.setLong(1, recordId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getTimestamp("created_date")).isNotNull();
            assertThat(rs.getTimestamp("last_modified_date")).isNotNull();
        }
    }

    private Long createPasswordResetToken(Long accountId, String token) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO password_reset_tokens (customer_account_id, token, expires_at) " +
                        "VALUES (?, ?, ?) RETURNING id",
                Long.class, accountId, token, OffsetDateTime.now().plusMinutes(10));
    }
}
