// src/it/java/com/bizwaresol/loyalty_service_club_api/database/DatabaseTriggersTest.java
package com.bizwaresol.loyalty_service_club_api.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JDBC tests for database triggers - no Spring bullshit.
 * Tests: auto username, verification status updates, audit trail, username updates
 */
class DatabaseTriggersTest {

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

    // ===== AUTO USERNAME TRIGGER TESTS =====

    @Test
    @DisplayName("Auto username trigger should set email as username")
    void testAutoUsernameWithEmail() throws SQLException {
        // Setup customer with email
        long emailId = insertEmail("autouser@gmail.com", false);
        long customerId = insertCustomer("Auto", "User", emailId, null);

        // Insert account WITHOUT username - trigger should set it
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, password) VALUES (?, ?) RETURNING username"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, "password");
        ResultSet rs = stmt.executeQuery();
        rs.next();

        assertEquals("autouser@gmail.com", rs.getString("username"),
                "Trigger should set username to email");
    }

    @Test
    @DisplayName("Auto username trigger should set phone as username when no email")
    void testAutoUsernameWithPhone() throws SQLException {
        // Setup customer with phone only
        long phoneId = insertPhone("+381612345678", false);
        long customerId = insertCustomer("Phone", "User", null, phoneId);

        // Insert account WITHOUT username
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, password) VALUES (?, ?) RETURNING username"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, "password");
        ResultSet rs = stmt.executeQuery();
        rs.next();

        assertEquals("+381612345678", rs.getString("username"),
                "Trigger should set username to phone when no email");
    }

    @Test
    @DisplayName("Auto username trigger should prefer email over phone")
    void testAutoUsernameEmailOverPhone() throws SQLException {
        // Setup customer with both email and phone
        long emailId = insertEmail("prefer@gmail.com", false);
        long phoneId = insertPhone("+381611111111", false);
        long customerId = insertCustomer("Prefer", "Email", emailId, phoneId);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, password) VALUES (?, ?) RETURNING username"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, "password");
        ResultSet rs = stmt.executeQuery();
        rs.next();

        assertEquals("prefer@gmail.com", rs.getString("username"),
                "Trigger should prefer email over phone");
    }

    @Test
    @DisplayName("Auto username trigger should fail when no contact info")
    void testAutoUsernameFailsWithoutContact() throws SQLException {
        // Setup customer without email or phone
        long customerId = insertCustomer("No", "Contact", null, null);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO customer_accounts (customer_id, password) VALUES (?, ?)"
        );
        stmt.setLong(1, customerId);
        stmt.setString(2, "password");

        assertThrows(SQLException.class, () -> stmt.executeUpdate(),
                "Account creation should fail when customer has no contact info");
    }

    // ===== VERIFICATION STATUS TRIGGER TESTS =====

    @Test
    @DisplayName("Email verification trigger should update account status")
    void testEmailVerificationTrigger() throws SQLException {
        // Setup customer with email and account
        long emailId = insertEmail("verify@gmail.com", false);
        long customerId = insertCustomer("Verify", "Test", emailId, null);
        long accountId = insertAccount(customerId, "verify@gmail.com");

        // Verify initial status
        assertEquals("UNVERIFIED", getVerificationStatus(accountId));

        // Update email to verified - trigger should fire
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_emails SET is_verified = ? WHERE id = ?"
        );
        updateStmt.setBoolean(1, true);
        updateStmt.setLong(2, emailId);
        updateStmt.executeUpdate();

        // Check account status changed
        assertEquals("EMAIL_VERIFIED", getVerificationStatus(accountId),
                "Account should be EMAIL_VERIFIED after email verification");
    }

    @Test
    @DisplayName("Phone verification trigger should update account status")
    void testPhoneVerificationTrigger() throws SQLException {
        // Setup customer with phone and account
        long phoneId = insertPhone("+381612345678", false);
        long customerId = insertCustomer("Phone", "Verify", null, phoneId);
        long accountId = insertAccount(customerId, "+381612345678");

        assertEquals("UNVERIFIED", getVerificationStatus(accountId));

        // Update phone to verified
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_phones SET is_verified = ? WHERE id = ?"
        );
        updateStmt.setBoolean(1, true);
        updateStmt.setLong(2, phoneId);
        updateStmt.executeUpdate();

        assertEquals("PHONE_VERIFIED", getVerificationStatus(accountId),
                "Account should be PHONE_VERIFIED after phone verification");
    }

    @Test
    @DisplayName("Both email and phone verified should result in FULLY_VERIFIED")
    void testFullVerificationStatus() throws SQLException {
        // Setup customer with both email and phone
        long emailId = insertEmail("full@gmail.com", false);
        long phoneId = insertPhone("+381611111111", false);
        long customerId = insertCustomer("Full", "Verify", emailId, phoneId);
        long accountId = insertAccount(customerId, "full@gmail.com");

        // Verify email first
        PreparedStatement emailUpdate = conn.prepareStatement(
                "UPDATE customer_emails SET is_verified = ? WHERE id = ?"
        );
        emailUpdate.setBoolean(1, true);
        emailUpdate.setLong(2, emailId);
        emailUpdate.executeUpdate();

        assertEquals("EMAIL_VERIFIED", getVerificationStatus(accountId));

        // Now verify phone - should become FULLY_VERIFIED
        PreparedStatement phoneUpdate = conn.prepareStatement(
                "UPDATE customer_phones SET is_verified = ? WHERE id = ?"
        );
        phoneUpdate.setBoolean(1, true);
        phoneUpdate.setLong(2, phoneId);
        phoneUpdate.executeUpdate();

        assertEquals("FULLY_VERIFIED", getVerificationStatus(accountId),
                "Account should be FULLY_VERIFIED when both email and phone are verified");
    }

    @Test
    @DisplayName("Unverifying email should downgrade status correctly")
    void testEmailUnverificationTrigger() throws SQLException {
        // Setup fully verified account
        long emailId = insertEmail("downgrade@gmail.com", true);
        long phoneId = insertPhone("+381622222222", true);
        long customerId = insertCustomer("Down", "Grade", emailId, phoneId);
        long accountId = insertAccount(customerId, "downgrade@gmail.com");

        // Should start as FULLY_VERIFIED (both verified)
        assertEquals("FULLY_VERIFIED", getVerificationStatus(accountId));

        // Unverify email
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_emails SET is_verified = ? WHERE id = ?"
        );
        updateStmt.setBoolean(1, false);
        updateStmt.setLong(2, emailId);
        updateStmt.executeUpdate();

        assertEquals("PHONE_VERIFIED", getVerificationStatus(accountId),
                "Account should downgrade to PHONE_VERIFIED when email is unverified");
    }

    // ===== ACCOUNT STATUS AUDIT TRIGGER TESTS =====

    @Test
    @DisplayName("Account status change should create audit record")
    void testAccountStatusAuditTrigger() throws SQLException {
        // Setup account
        long customerId = insertCustomer("Audit", "Test", null, null);
        long accountId = insertAccount(customerId, "audit_test");

        // Change activity status
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET activity_status = 'INACTIVE'::customer_account_activity_status_enum WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Check audit record was created
        PreparedStatement auditQuery = conn.prepareStatement(
                "SELECT old_status, new_status FROM account_status_audit WHERE account_id = ?"
        );
        auditQuery.setLong(1, accountId);
        ResultSet rs = auditQuery.executeQuery();

        assertTrue(rs.next(), "Audit record should be created");
        assertEquals("ACTIVE", rs.getString("old_status"));
        assertEquals("INACTIVE", rs.getString("new_status"));
    }

    @Test
    @DisplayName("Multiple status changes should create multiple audit records")
    void testMultipleStatusChangeAudits() throws SQLException {
        long customerId = insertCustomer("Multi", "Audit", null, null);
        long accountId = insertAccount(customerId, "multi_audit");

        // Change to INACTIVE
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET activity_status = ?::customer_account_activity_status_enum WHERE id = ?"
        );
        updateStmt.setString(1, "INACTIVE");
        updateStmt.setLong(2, accountId);
        updateStmt.executeUpdate();

        // Change to SUSPENDED
        updateStmt.setString(1, "SUSPENDED");
        updateStmt.setLong(2, accountId);
        updateStmt.executeUpdate();

        // Check both audit records exist
        PreparedStatement auditQuery = conn.prepareStatement(
                "SELECT old_status, new_status FROM account_status_audit WHERE account_id = ? ORDER BY created_date"
        );
        auditQuery.setLong(1, accountId);
        ResultSet rs = auditQuery.executeQuery();

        assertTrue(rs.next(), "First audit record should exist");
        assertEquals("ACTIVE", rs.getString("old_status"));
        assertEquals("INACTIVE", rs.getString("new_status"));

        assertTrue(rs.next(), "Second audit record should exist");
        assertEquals("INACTIVE", rs.getString("old_status"));
        assertEquals("SUSPENDED", rs.getString("new_status"));
    }

    @Test
    @DisplayName("Audit trigger should not fire when status unchanged")
    void testAuditTriggerNotFiredWhenUnchanged() throws SQLException {
        long customerId = insertCustomer("Unchanged", "Test", null, null);
        long accountId = insertAccount(customerId, "unchanged");

        // Update something other than activity_status
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Check no audit record created
        PreparedStatement auditQuery = conn.prepareStatement(
                "SELECT COUNT(*) FROM account_status_audit WHERE account_id = ?"
        );
        auditQuery.setLong(1, accountId);
        ResultSet rs = auditQuery.executeQuery();
        rs.next();

        assertEquals(0, rs.getInt(1), "No audit record should be created when status unchanged");
    }

    // ===== USERNAME UPDATE TRIGGER TESTS =====

    @Test
    @DisplayName("Username should update when customer email is added")
    void testUsernameUpdateOnEmailAdd() throws SQLException {
        // Setup customer with phone initially
        long phoneId = insertPhone("+381612345678", false);
        long customerId = insertCustomer("Update", "Test", null, phoneId);
        long accountId = insertAccount(customerId, "+381612345678");

        assertEquals("+381612345678", getUsername(accountId));

        // Add email to customer
        long emailId = insertEmail("newemail@gmail.com", false);
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customers SET email_id = ? WHERE id = ?"
        );
        updateStmt.setLong(1, emailId);
        updateStmt.setLong(2, customerId);
        updateStmt.executeUpdate();

        assertEquals("newemail@gmail.com", getUsername(accountId),
                "Username should update to email when email is added");
    }

    @Test
    @DisplayName("Username should fallback to phone when email is removed")
    void testUsernameUpdateOnEmailRemoval() throws SQLException {
        // Setup customer with both email and phone
        long emailId = insertEmail("remove@gmail.com", false);
        long phoneId = insertPhone("+381622222222", false);
        long customerId = insertCustomer("Remove", "Email", emailId, phoneId);
        long accountId = insertAccount(customerId, "remove@gmail.com");

        assertEquals("remove@gmail.com", getUsername(accountId));

        // Remove email from customer
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customers SET email_id = NULL WHERE id = ?"
        );
        updateStmt.setLong(1, customerId);
        updateStmt.executeUpdate();

        assertEquals("+381622222222", getUsername(accountId),
                "Username should fallback to phone when email is removed");
    }

    @Test
    @DisplayName("Username should update when customer phone is changed")
    void testUsernameUpdateOnPhoneChange() throws SQLException {
        // Setup customer with phone only
        long phoneId = insertPhone("+381611111111", false);
        long customerId = insertCustomer("Phone", "Change", null, phoneId);
        long accountId = insertAccount(customerId, "+381611111111");

        assertEquals("+381611111111", getUsername(accountId));

        // Change to different phone
        long newPhoneId = insertPhone("+381622222222", false);
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customers SET phone_id = ? WHERE id = ?"
        );
        updateStmt.setLong(1, newPhoneId);
        updateSt