// src/it/java/com/bizwaresol/loyalty_service_club_api/database/DatabaseFunctionsTest.java
package com.bizwaresol.loyalty_service_club_api.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JDBC tests for database functions and jobs - no Spring bullshit.
 * Tests: mark_inactive_accounts_batched(), job execution audit, business config
 */
class DatabaseFunctionsTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/loyalty_service_club_test";
        String username = "loyalty_service_club_admin";
        String password = "Loyalty16";

        conn = DriverManager.getConnection(url, username, password);
        conn.setAutoCommit(false);
        cleanDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.rollback();
            conn.close();
        }
    }

    // ===== MARK INACTIVE ACCOUNTS FUNCTION TESTS =====

    @Test
    @DisplayName("mark_inactive_accounts_batched should mark old accounts as inactive")
    void testMarkInactiveAccountsFunction() throws SQLException {
        // Setup accounts with different last login dates
        setupTestAccountsForInactivity();

        // Verify initial state - all accounts should be ACTIVE
        assertEquals(3, getActiveAccountCount(), "All test accounts should initially be ACTIVE");
        assertEquals(0, getInactiveAccountCount(), "No accounts should be inactive initially");

        // Call the function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // Verify results - old accounts should be marked INACTIVE
        assertEquals(1, getActiveAccountCount(), "Only recent accounts should remain ACTIVE");
        assertEquals(2, getInactiveAccountCount(), "Old accounts should be marked INACTIVE");

        // Verify job execution audit record was created
        assertTrue(checkJobExecutionAudit("mark_inactive_accounts", true),
                "Job execution should be logged as successful");
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should handle accounts with NULL last_login")
    void testMarkInactiveAccountsWithNullLastLogin() throws SQLException {
        // Create account with NULL last_login_at (never logged in)
        long customerId = insertCustomer("Never", "LoggedIn", null, null);
        long accountId = insertAccount(customerId, "never_logged_in");

        // Ensure last_login_at is NULL
        PreparedStatement nullifyStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = NULL WHERE id = ?"
        );
        nullifyStmt.setLong(1, accountId);
        nullifyStmt.executeUpdate();

        // Call the function - should not crash
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        assertDoesNotThrow(() -> stmt.executeQuery(),
                "Function should handle NULL last_login_at gracefully");

        // Account with NULL last_login should remain ACTIVE (business rule)
        assertEquals("ACTIVE", getAccountStatus(accountId),
                "Accounts with NULL last_login should remain ACTIVE");
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should respect configuration values")
    void testMarkInactiveAccountsRespectsConfiguration() throws SQLException {
        // Change configuration to 30 days instead of default 60
        updateBusinessConfig("account_inactivity_days", "30");

        // Create account that's 35 days old (should be marked inactive with 30-day config)
        long customerId = insertCustomer("Config", "Test", null, null);
        long accountId = insertAccount(customerId, "config_test");

        // Set last login to 35 days ago
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '35 days' WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Call function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // Account should be marked inactive due to 30-day config
        assertEquals("INACTIVE", getAccountStatus(accountId),
                "Account should be marked inactive based on configuration");

        // Reset configuration for other tests
        updateBusinessConfig("account_inactivity_days", "60");
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should handle missing configuration gracefully")
    void testMarkInactiveAccountsWithMissingConfig() throws SQLException {
        // Remove configuration values
        PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM business_config WHERE key IN ('account_inactivity_days', 'inactivity_batch_size')"
        );
        deleteStmt.executeUpdate();

        // Function should throw exception about missing configuration
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        SQLException exception = assertThrows(SQLException.class, () -> stmt.executeQuery(),
                "Function should fail when required configuration is missing");

        assertTrue(exception.getMessage().contains("Configuration missing"),
                "Exception should mention missing configuration");

        // Verify error was logged in job execution audit
        assertTrue(checkJobExecutionAudit("mark_inactive_accounts", false),
                "Failed execution should be logged in audit table");
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should create proper audit trail")
    void testMarkInactiveAccountsAuditTrail() throws SQLException {
        // Setup test account
        long customerId = insertCustomer("Audit", "Trail", null, null);
        long accountId = insertAccount(customerId, "audit_trail");

        // Set last login to 70 days ago (should trigger inactivity)
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '70 days' WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Run function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // Check account status audit was created
        PreparedStatement auditQuery = conn.prepareStatement(
                "SELECT old_status, new_status FROM account_status_audit WHERE account_id = ?"
        );
        auditQuery.setLong(1, accountId);
        ResultSet auditResult = auditQuery.executeQuery();

        assertTrue(auditResult.next(), "Account status change should be audited");
        assertEquals("ACTIVE", auditResult.getString("old_status"));
        assertEquals("INACTIVE", auditResult.getString("new_status"));
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should only affect ACTIVE accounts")
    void testMarkInactiveAccountsOnlyAffectsActiveAccounts() throws SQLException {
        // Create account that's already INACTIVE
        long customerId = insertCustomer("Already", "Inactive", null, null);
        long accountId = insertAccount(customerId, "already_inactive");

        // Set account to INACTIVE and old last login
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET activity_status = 'INACTIVE'::customer_account_activity_status_enum, last_login_at = CURRENT_TIMESTAMP - INTERVAL '90 days' WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Get initial audit count
        int initialAuditCount = getAuditCountForAccount(accountId);

        // Run function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // Account should still be INACTIVE (no change)
        assertEquals("INACTIVE", getAccountStatus(accountId),
                "Already inactive account should remain unchanged");

        // No new audit record should be created by the function
        assertEquals(initialAuditCount, getAuditCountForAccount(accountId),
                "Function should not create audit records for already inactive accounts");
    }

    @Test
    @DisplayName("mark_inactive_accounts_batched should process accounts in batches")
    void testMarkInactiveAccountsBatchProcessing() throws SQLException {
        // Set small batch size for testing
        updateBusinessConfig("inactivity_batch_size", "2");

        // Create 5 old accounts
        for (int i = 1; i <= 5; i++) {
            long customerId = insertCustomer("Batch" + i, "Test", null, null);
            long accountId = insertAccount(customerId, "batch_test_" + i);

            // Set last login to 70 days ago
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '70 days' WHERE id = ?"
            );
            updateStmt.setLong(1, accountId);
            updateStmt.executeUpdate();
        }

        // All should be ACTIVE initially
        assertEquals(5, getActiveAccountCount());

        // Run function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // All should be processed despite batch size of 2
        assertEquals(0, getActiveAccountCount());
        assertEquals(5, getInactiveAccountCount());

        // Reset batch size
        updateBusinessConfig("inactivity_batch_size", "1000");
    }

    // ===== JOB EXECUTION AUDIT TESTS =====

    @Test
    @DisplayName("Job execution audit should record successful function execution")
    void testJobExecutionAuditSuccess() throws SQLException {
        // Create account for function to process
        long customerId = insertCustomer("Job", "Success", null, null);
        long accountId = insertAccount(customerId, "job_success");

        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '70 days' WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Run function
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        stmt.executeQuery();

        // Check job execution audit
        PreparedStatement auditQuery = conn.prepareStatement("""
            SELECT job_name, execution_date, success, records_processed, execution_time_ms
            FROM job_execution_audit 
            WHERE job_name = 'mark_inactive_accounts' AND execution_date = CURRENT_DATE
        """);
        ResultSet rs = auditQuery.executeQuery();

        assertTrue(rs.next(), "Job execution audit record should exist");
        assertEquals("mark_inactive_accounts", rs.getString("job_name"));
        assertEquals(LocalDate.now(), rs.getDate("execution_date").toLocalDate());
        assertTrue(rs.getBoolean("success"));
        assertEquals(1, rs.getInt("records_processed"));
        assertTrue(rs.getInt("execution_time_ms") >= 0, "Execution time should be recorded");
    }

    @Test
    @DisplayName("Job execution audit should record failed function execution")
    void testJobExecutionAuditFailure() throws SQLException {
        // Remove required configuration to cause failure
        PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM business_config WHERE key = 'account_inactivity_days'"
        );
        deleteStmt.executeUpdate();

        // Try to run function (should fail)
        PreparedStatement stmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        assertThrows(SQLException.class, () -> stmt.executeQuery());

        // Check job execution audit recorded the failure
        PreparedStatement auditQuery = conn.prepareStatement("""
            SELECT job_name, success, error_message 
            FROM job_execution_audit 
            WHERE job_name = 'mark_inactive_accounts' AND execution_date = CURRENT_DATE
        """);
        ResultSet rs = auditQuery.executeQuery();

        assertTrue(rs.next(), "Job execution audit record should exist for failure");
        assertEquals("mark_inactive_accounts", rs.getString("job_name"));
        assertFalse(rs.getBoolean("success"));
        assertNotNull(rs.getString("error_message"), "Error message should be recorded");
        assertTrue(rs.getString("error_message").contains("Configuration missing"));
    }

    // ===== BUSINESS CONFIGURATION TESTS =====

    @Test
    @DisplayName("Business configuration should be properly seeded")
    void testBusinessConfigurationSeeded() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT key, value FROM business_config ORDER BY key"
        );
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "First config should exist");
        assertEquals("account_inactivity_days", rs.getString("key"));
        assertEquals("60", rs.getString("value"));

        assertTrue(rs.next(), "Second config should exist");
        assertEquals("inactivity_batch_size", rs.getString("key"));
        assertEquals("1000", rs.getString("value"));
    }

    @Test
    @DisplayName("Business configuration should be updateable")
    void testBusinessConfigurationUpdate() throws SQLException {
        // Update configuration
        updateBusinessConfig("account_inactivity_days", "90");

        // Verify update
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT value FROM business_config WHERE key = 'account_inactivity_days'"
        );
        ResultSet rs = stmt.executeQuery();
        rs.next();
        assertEquals("90", rs.getString("value"));
    }

    // ===== HELPER METHODS =====

    private void setupTestAccountsForInactivity() throws SQLException {
        // Account 1: Recent login (should stay ACTIVE)
        long customer1 = insertCustomer("Recent", "User", null, null);
        long account1 = insertAccount(customer1, "recent_user");
        PreparedStatement stmt1 = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '10 days' WHERE id = ?"
        );
        stmt1.setLong(1, account1);
        stmt1.executeUpdate();

        // Account 2: Old login (should become INACTIVE)
        long customer2 = insertCustomer("Old", "User1", null, null);
        long account2 = insertAccount(customer2, "old_user1");
        PreparedStatement stmt2 = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '70 days' WHERE id = ?"
        );
        stmt2.setLong(1, account2);
        stmt2.executeUpdate();

        // Account 3: Very old login (should become INACTIVE)
        long customer3 = insertCustomer("Old", "User2", null, null);
        long account3 = insertAccount(customer3, "old_user2");
        PreparedStatement stmt3 = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '90 days' WHERE id = ?"
        );
        stmt3.setLong(1, account3);
        stmt3.executeUpdate();
    }

    private int getActiveAccountCount() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM customer_accounts WHERE activity_status = 'ACTIVE'"
        );
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private int getInactiveAccountCount() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM customer_accounts WHERE activity_status = 'INACTIVE'"
        );
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private String getAccountStatus(long accountId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT activity_status FROM customer_accounts WHERE id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getString(1);
    }

    private boolean checkJobExecutionAudit(String jobName, boolean expectedSuccess) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT success FROM job_execution_audit WHERE job_name = ? AND execution_date = CURRENT_DATE"
        );
        stmt.setString(1, jobName);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            return false;
        }

        return rs.getBoolean("success") == expectedSuccess;
    }

    private int getAuditCountForAccount(long accountId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM account_status_audit WHERE account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private void updateBusinessConfig(String key, String value) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE business_config SET value = ? WHERE key = ?"
        );
        stmt.setString(1, value);
        stmt.setString(2, key);
        stmt.executeUpdate();
    }

    private void cleanDatabase() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("SET session_replication_role = replica;");
        stmt.execute("DELETE FROM account_status_audit;");
        stmt.execute("DELETE FROM job_execution_audit;");
        stmt.execute("DELETE FROM password_reset_tokens;");
        stmt.execute("DELETE FROM customer_accounts;");
        stmt.execute("DELETE FROM customers;");
        stmt.execute("DELETE FROM customer_emails;");
        stmt.execute("DELETE FROM customer_phones;");
        stmt.execute("DELETE FROM business_config;");
        stmt.execute("""
            INSERT INTO business_config (key, value, description) VALUES 
            ('account_inactivity_days', '60', 'Number of days after last login before marking account as inactive'),
            ('inactivity_batch_size', '1000', 'Batch size for processing inactive accounts')
        """);
        stmt.execute("SET session_replication_role = DEFAULT;");
        stmt.execute("ALTER SEQUENCE customer_emails_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customer_phones_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customers_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customer_accounts_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE account_status_audit_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE job_execution_audit_id_seq RESTART WITH 1;");
    }

    private long insertCustomer(String firstName, String lastName, Long emailId, Long phoneId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customers (first_name, last_name, email_id, phone_id) VALUES (?, ?, ?, ?) RETURNING id"
        );
        stmt.setString(1, firstName);
        stmt.setString(2, lastName);
        if (emailId != null) {
            stmt.setLong(3, emailId);
        } else {
            stmt.setNull(3, Types.BIGINT);
        }
        if (phoneId != null) {
            stmt.setLong(4, phoneId);
        } else {
            stmt.setNull(4, Types.BIGINT);
        }
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getLong(1);
    }

    private long insertAccount(long customerId, String username) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, username, password) VALUES (?, ?, ?) RETURNING id"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, username);
        stmt.setString(3, "hashedpassword");
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getLong(1);
    }
}