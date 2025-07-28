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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test-postgres")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false" // Prevent conflicts during tests
})
@Component
public class DatabaseViewsIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        // Clean all tables except business_config (preserve seeded values)
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
    }

    // ===== CUSTOMER_CONTACT_LOOKUP VIEW TESTS =====

    @Test
    void customerContactLookupView_WithEmailAndPhone_ShouldReturnBothContacts() {
        // Arrange
        Long emailId = createCustomerEmail("test@gmail.com", true);
        Long phoneId = createCustomerPhone("+381612345678", false);
        Long customerId = createCustomer("John", "Doe", emailId, phoneId);

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_contact_lookup WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("customer_id")).isEqualTo(customerId);
            assertThat(rs.getLong("email_id")).isEqualTo(emailId);
            assertThat(rs.getLong("phone_id")).isEqualTo(phoneId);
            assertThat(rs.getString("email")).isEqualTo("test@gmail.com");
            assertThat(rs.getString("phone")).isEqualTo("+381612345678");
            assertThat(rs.getBoolean("email_verified")).isTrue();
            assertThat(rs.getBoolean("phone_verified")).isFalse();
            assertThat(rs.getString("preferred_username")).isEqualTo("test@gmail.com"); // Email takes precedence
            assertThat(rs.getBoolean("has_email")).isTrue();
            assertThat(rs.getBoolean("has_phone")).isTrue();
            assertThat(rs.getString("contact_type")).isEqualTo("BOTH");
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void customerContactLookupView_WithEmailOnly_ShouldReturnEmailAsPreferred() {
        // Arrange
        Long emailId = createCustomerEmail("john.doe@outlook.com", false);
        Long customerId = createCustomer("John", "Doe", emailId, null);

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_contact_lookup WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("preferred_username")).isEqualTo("john.doe@outlook.com");
            assertThat(rs.getBoolean("has_email")).isTrue();
            assertThat(rs.getBoolean("has_phone")).isFalse();
            assertThat(rs.getString("contact_type")).isEqualTo("EMAIL_ONLY");
            assertThat(rs.getObject("phone_id")).isNull();
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void customerContactLookupView_WithPhoneOnly_ShouldReturnPhoneAsPreferred() {
        // Arrange
        Long phoneId = createCustomerPhone("+381611234567", true);
        Long customerId = createCustomer("Jane", "Smith", null, phoneId);

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_contact_lookup WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("preferred_username")).isEqualTo("+381611234567");
            assertThat(rs.getBoolean("has_email")).isFalse();
            assertThat(rs.getBoolean("has_phone")).isTrue();
            assertThat(rs.getString("contact_type")).isEqualTo("PHONE_ONLY");
            assertThat(rs.getObject("email_id")).isNull();
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void customerContactLookupView_WithNoContacts_ShouldReturnNoneType() {
        // Arrange
        Long customerId = createCustomer("Empty", "Contact", null, null);

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_contact_lookup WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject("preferred_username")).isNull();
            assertThat(rs.getBoolean("has_email")).isFalse();
            assertThat(rs.getBoolean("has_phone")).isFalse();
            assertThat(rs.getString("contact_type")).isEqualTo("NONE");
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    // ===== CUSTOMER_VERIFICATION_DATA VIEW TESTS =====

    @Test
    void customerVerificationDataView_WithBothVerified_ShouldShowFullyVerified() {
        // Arrange
        Long emailId = createCustomerEmail("verified@gmail.com", true);
        Long phoneId = createCustomerPhone("+381611111111", true);
        Long customerId = createCustomer("Verified", "User", emailId, phoneId);
        Long accountId = createCustomerAccount(customerId, "verified@gmail.com", "hashedPassword123");

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_verification_data WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("account_id")).isEqualTo(accountId);
            assertThat(rs.getString("current_status")).isEqualTo("UNVERIFIED"); // Default from account creation
            assertThat(rs.getBoolean("email_verified")).isTrue();
            assertThat(rs.getBoolean("phone_verified")).isTrue();
            assertThat(rs.getBoolean("has_email")).isTrue();
            assertThat(rs.getBoolean("has_phone")).isTrue();
            assertThat(rs.getString("calculated_status")).isEqualTo("FULLY_VERIFIED");
            assertThat(rs.getBoolean("status_needs_update")).isTrue(); // Needs update from UNVERIFIED to FULLY_VERIFIED
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void customerVerificationDataView_WithEmailVerifiedOnly_ShouldShowEmailVerified() {
        // Arrange
        Long emailId = createCustomerEmail("partial@gmail.com", true);
        Long phoneId = createCustomerPhone("+381622222222", false);
        Long customerId = createCustomer("Partial", "User", emailId, phoneId);
        createCustomerAccount(customerId, "partial@gmail.com", "hashedPassword123");

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_verification_data WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("email_verified")).isTrue();
            assertThat(rs.getBoolean("phone_verified")).isFalse();
            assertThat(rs.getString("calculated_status")).isEqualTo("EMAIL_VERIFIED");
            assertThat(rs.getBoolean("status_needs_update")).isTrue();
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void customerVerificationDataView_WithNoVerification_ShouldShowUnverified() {
        // Arrange
        Long emailId = createCustomerEmail("unverified@gmail.com", false);
        Long phoneId = createCustomerPhone("+381633333333", false);
        Long customerId = createCustomer("Unverified", "User", emailId, phoneId);
        createCustomerAccount(customerId, "unverified@gmail.com", "hashedPassword123");

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM customer_verification_data WHERE customer_id = ?")) {

            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("email_verified")).isFalse();
            assertThat(rs.getBoolean("phone_verified")).isFalse();
            assertThat(rs.getString("calculated_status")).isEqualTo("UNVERIFIED");
            assertThat(rs.getBoolean("status_needs_update")).isFalse(); // Already UNVERIFIED
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    // ===== ACCOUNT_ACTIVITY_DATA VIEW TESTS =====

    @Test
    void accountActivityDataView_WithRecentLogin_ShouldCalculateCorrectDays() {
        // Arrange
        Long emailId = createCustomerEmail("active@gmail.com", true);
        Long customerId = createCustomer("Active", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "active@gmail.com", "hashedPassword123");

        // Set last login to 5 days ago
        OffsetDateTime fiveDaysAgo = OffsetDateTime.now().minusDays(5);
        updateAccountLastLogin(accountId, fiveDaysAgo);

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM account_activity_data WHERE account_id = ?")) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("activity_status")).isEqualTo("ACTIVE");
            assertThat(rs.getBoolean("has_logged_in")).isTrue();
            assertThat(rs.getInt("days_since_login")).isEqualTo(5);
            assertThat(rs.getObject("last_login_at")).isNotNull();
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void accountActivityDataView_WithNeverLoggedIn_ShouldShowNullDays() {
        // Arrange
        Long emailId = createCustomerEmail("newuser@gmail.com", false);
        Long customerId = createCustomer("New", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "newuser@gmail.com", "hashedPassword123");

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM account_activity_data WHERE account_id = ?")) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("has_logged_in")).isFalse();
            assertThat(rs.getObject("days_since_login")).isNull();
            assertThat(rs.getObject("last_login_at")).isNull();
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }

    @Test
    void accountActivityDataView_WithInactiveStatus_ShouldRetainStatusInfo() {
        // Arrange
        Long emailId = createCustomerEmail("inactive@gmail.com", true);
        Long customerId = createCustomer("Inactive", "User", emailId, null);
        Long accountId = createCustomerAccount(customerId, "inactive@gmail.com", "hashedPassword123");

        // Mark account as inactive
        updateAccountActivityStatus(accountId, "INACTIVE");

        // Act & Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM account_activity_data WHERE account_id = ?")) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("activity_status")).isEqualTo("INACTIVE");
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
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

    private Long createCustomerAccount(Long customerId, String username, String password) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customer_accounts (customer_id, username, password, activity_status, verification_status) " +
                        "VALUES (?, ?, ?, ?::customer_account_activity_status_enum, ?::customer_account_verification_status_enum) RETURNING id",
                Long.class, customerId, username, password, "ACTIVE", "UNVERIFIED");
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