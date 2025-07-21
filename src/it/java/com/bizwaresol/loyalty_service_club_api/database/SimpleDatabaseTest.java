// src/it/java/com/bizwaresol/loyalty_service_club_api/database/SimpleDatabaseTest.java
package com.bizwaresol.loyalty_service_club_api.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JDBC database tests - no Spring, no bullshit.
 * Tests data persistence, constraints, triggers, and functions directly.
 */
class SimpleDatabaseTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        // Direct JDBC connection to test database
        String url = "jdbc:postgresql://localhost:5432/loyalty_service_club_test";
        String username = "loyalty_service_club_admin";
        String password = "Loyalty16";

        conn = DriverManager.getConnection(url, username, password);
        conn.setAutoCommit(false); // Use transactions

        // Clean database before each test
        cleanDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.rollback(); // Rollback any changes
            conn.close();
        }
    }

    // ===== DATA PERSISTENCE TESTS =====

    @Test
    @DisplayName("Insert and retrieve customer email")
    void testEmailPersistence() throws SQLException {
        // Insert email
        PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO customer_emails (email, is_verified) VALUES (?, ?) RETURNING id"
        );
        insertStmt.setString(1, "test@gmail.com");
        insertStmt.setBoolean(2, false);
        ResultSet rs = insertStmt.executeQuery();
        rs.next();
        long emailId = rs.getLong(1);

        // Retrieve and verify
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT email, is_verified FROM customer_emails WHERE id = ?"
        );
        selectStmt.setLong(1, emailId);
        rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "Email should exist");
        assertEquals("test@gmail.com", rs.getString("email"));
        assertFalse(rs.getBoolean("is_verified"));
    }

    @Test
    @DisplayName("Insert and retrieve customer phone")
    void testPhonePersistence() throws SQLException {
        // Insert phone
        PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO customer_phones (phone, is_verified) VALUES (?, ?) RETURNING id"
        );
        insertStmt.setString(1, "+381612345678");
        insertStmt.setBoolean(2, true);
        ResultSet rs = insertStmt.executeQuery();
        rs.next();
        long phoneId = rs.getLong(1);

        // Retrieve and verify
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT phone, is_verified FROM customer_phones WHERE id = ?"
        );
        selectStmt.setLong(1, phoneId);
        rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "Phone should exist");
        assertEquals("+381612345678", rs.getString("phone"));
        assertTrue(rs.getBoolean("is_verified"));
    }

    @Test
    @DisplayName("Insert and retrieve customer")
    void testCustomerPersistence() throws SQLException {
        // Insert customer
        PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO customers (first_name, last_name) VALUES (?, ?) RETURNING id"
        );
        insertStmt.setString(1, "John");
        insertStmt.setString(2, "Doe");
        ResultSet rs = insertStmt.executeQuery();
        rs.next();
        long customerId = rs.getLong(1);

        // Retrieve and verify
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT first_name, last_name FROM customers WHERE id = ?"
        );
        selectStmt.setLong(1, customerId);
        rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "Customer should exist");
        assertEquals("John", rs.getString("first_name"));
        assertEquals("Doe", rs.getString("last_name"));
    }

    @Test
    @DisplayName("Insert and retrieve customer account")
    void testCustomerAccountPersistence() throws SQLException {
        // Setup customer first
        long customerId = insertCustomer("Jane", "Smith", null, null);

        // Insert customer account
        PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, username, password) VALUES (?, ?, ?) RETURNING id"
        );
        insertStmt.setLong(1, customerId);
        insertStmt.setString(2, "janesmith");
        insertStmt.setString(3, "hashedpassword123");
        ResultSet rs = insertStmt.executeQuery();
        rs.next();
        long accountId = rs.getLong(1);

        // Retrieve and verify
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT username, activity_status, verification_status FROM customer_accounts WHERE id = ?"
        );
        selectStmt.setLong(1, accountId);
        rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "Account should exist");
        assertEquals("janesmith", rs.getString("username"));
        assertEquals("ACTIVE", rs.getString("activity_status"));
        assertEquals("UNVERIFIED", rs.getString("verification_status"));
    }

    // ===== CONSTRAINT TESTS =====

    @Test
    @DisplayName("Duplicate email should fail")
    void testEmailUniqueConstraint() throws SQLException {
        // Insert first email
        insertEmail("duplicate@gmail.com", false);

        // Try to insert duplicate - should fail
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_emails (email, is_verified) VALUES (?, ?)"
        );
        stmt.setString(1, "duplicate@gmail.com");
        stmt.setBoolean(2, true);

        assertThrows(SQLException.class, () -> stmt.executeUpdate(),
                "Duplicate email should violate unique constraint");
    }

    @Test
    @DisplayName("Invalid foreign key should fail")
    void testForeignKeyConstraint() throws SQLException {
        // Try to insert account with non-existent customer
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, username, password) VALUES (?, ?, ?)"
        );
        stmt.setLong(1, 99999L); // Non-existent customer
        stmt.setString(2, "invalid");
        stmt.setString(3, "password");

        assertThrows(SQLException.class, () -> stmt.executeUpdate(),
                "Invalid customer_id should violate foreign key constraint");
    }

    // ===== TRIGGER TESTS =====

    @Test
    @DisplayName("Auto username trigger should work")
    void testAutoUsernameCreation() throws SQLException {
        // Setup customer with email
        long emailId = insertEmail("autouser@gmail.com", false);
        long customerId = insertCustomer("Auto", "User", emailId, null);

        // Insert account without username - trigger should set it
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, password) VALUES (?, ?) RETURNING username"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, "password");
        ResultSet rs = stmt.executeQuery();
        rs.next();
        String username = rs.getString("username");

        assertEquals("autouser@gmail.com", username,
                "Trigger should set username to email");
    }

    @Test
    @DisplayName("Email verification trigger should update account status")
    void testEmailVerificationTrigger() throws SQLException {
        // Setup customer with email and account
        long emailId = insertEmail("verify@gmail.com", false);
        long customerId = insertCustomer("Verify", "User", emailId, null);
        long accountId = insertAccount(customerId, "verify@gmail.com");

        // Update email to verified
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_emails SET is_verified = ? WHERE id = ?"
        );
        updateStmt.setBoolean(1, true);
        updateStmt.setLong(2, emailId);
        updateStmt.executeUpdate();

        // Check account verification status
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT verification_status FROM customer_accounts WHERE id = ?"
        );
        selectStmt.setLong(1, accountId);
        ResultSet rs = selectStmt.executeQuery();
        rs.next();

        assertEquals("EMAIL_VERIFIED", rs.getString("verification_status"),
                "Account should be EMAIL_VERIFIED after email verification");
    }

    @Test
    @DisplayName("Account status change should create audit record")
    void testAccountStatusAuditTrigger() throws SQLException {
        // Setup account
        long customerId = insertCustomer("Audit", "Test", null, null);
        long accountId = insertAccount(customerId, "audituser");

        // Change account status
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET activity_status = 'INACTIVE'::customer_account_activity_status_enum WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Check audit record created
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT old_status, new_status FROM account_status_audit WHERE account_id = ?"
        );
        selectStmt.setLong(1, accountId);
        ResultSet rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "Audit record should be created");
        assertEquals("ACTIVE", rs.getString("old_status"));
        assertEquals("INACTIVE", rs.getString("new_status"));
    }

    // ===== FUNCTION TESTS =====

    @Test
    @DisplayName("Mark inactive accounts function should work")
    void testMarkInactiveAccountsFunction() throws SQLException {
        // Setup old account
        long customerId = insertCustomer("Old", "User", null, null);
        long accountId = insertAccount(customerId, "olduser");

        // Set last login to old date
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '70 days' WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Call function
        PreparedStatement callStmt = conn.prepareStatement("SELECT mark_inactive_accounts_batched()");
        callStmt.executeQuery();

        // Check account is now inactive
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT activity_status FROM customer_accounts WHERE id = ?"
        );
        selectStmt.setLong(1, accountId);
        ResultSet rs = selectStmt.executeQuery();
        rs.next();

        assertEquals("INACTIVE", rs.getString("activity_status"),
                "Old account should be marked INACTIVE");
    }

    // ===== HELPER METHODS =====

    private void cleanDatabase() throws SQLException {
        Statement stmt = conn.createStatement();

        // Disable constraints for cleanup
        stmt.execute("SET session_replication_role = replica;");

        // Clean tables
        stmt.execute("DELETE FROM account_status_audit;");
        stmt.execute("DELETE FROM job_execution_audit;");
        stmt.execute("DELETE FROM password_reset_tokens;");
        stmt.execute("DELETE FROM customer_accounts;");
        stmt.execute("DELETE FROM customers;");
        stmt.execute("DELETE FROM customer_emails;");
        stmt.execute("DELETE FROM customer_phones;");

        // Reset business config
        stmt.execute("DELETE FROM business_config;");
        stmt.execute("""
            INSERT INTO business_config (key, value, description) VALUES 
            ('account_inactivity_days', '60', 'Number of days after last login before marking account as inactive'),
            ('inactivity_batch_size', '1000', 'Batch size for processing inactive accounts')
        """);

        // Re-enable constraints
        stmt.execute("SET session_replication_role = DEFAULT;");

        // Reset sequences
        stmt.execute("ALTER SEQUENCE customer_emails_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customer_phones_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customers_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE customer_accounts_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE password_reset_tokens_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE account_status_audit_id_seq RESTART WITH 1;");
        stmt.execute("ALTER SEQUENCE job_execution_audit_id_seq RESTART WITH 1;");
    }

    private long insertEmail(String email, boolean verified) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_emails (email, is_verified) VALUES (?, ?) RETURNING id"
        );
        stmt.setString(1, email);
        stmt.setBoolean(2, verified);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getLong(1);
    }

    private long insertPhone(String phone, boolean verified) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_phones (phone, is_verified) VALUES (?, ?) RETURNING id"
        );
        stmt.setString(1, phone);
        stmt.setBoolean(2, verified);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getLong(1);
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