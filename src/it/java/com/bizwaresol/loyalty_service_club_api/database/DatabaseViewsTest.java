// src/it/java/com/bizwaresol/loyalty_service_club_api/database/DatabaseViewsTest.java
package com.bizwaresol.loyalty_service_club_api.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JDBC tests for database views - no Spring bullshit.
 * Tests: customer_verification_lookup, customer_account_info_lookup
 */
class DatabaseViewsTest {

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

    // ===== CUSTOMER VERIFICATION LOOKUP VIEW TESTS =====

    @Test
    @DisplayName("customer_verification_lookup should show correct verification status")
    void testCustomerVerificationLookupView() throws SQLException {
        // Setup customer with email and phone
        long emailId = insertEmail("lookup@gmail.com", false);
        long phoneId = insertPhone("+381612345678", true);
        long customerId = insertCustomer("Lookup", "Test", emailId, phoneId);
        long accountId = insertAccount(customerId, "lookup@gmail.com");

        // Query the view
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT email_id, phone_id, verification_status, customer_id, customer_account_id FROM customer_verification_lookup WHERE customer_account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data for account");
        assertEquals(emailId, rs.getLong("email_id"));
        assertEquals(phoneId, rs.getLong("phone_id"));
        assertEquals("UNVERIFIED", rs.getString("verification_status"));
        assertEquals(customerId, rs.getLong("customer_id"));
        assertEquals(accountId, rs.getLong("customer_account_id"));
    }

    @Test
    @DisplayName("customer_verification_lookup should handle NULL email")
    void testCustomerVerificationLookupWithNullEmail() throws SQLException {
        // Setup customer with only phone
        long phoneId = insertPhone("+381611111111", false);
        long customerId = insertCustomer("Phone", "Only", null, phoneId);
        long accountId = insertAccount(customerId, "+381611111111");

        PreparedStatement stmt = conn.prepareStatement(
                "SELECT email_id, phone_id FROM customer_verification_lookup WHERE customer_account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data even with NULL email");
        assertEquals(0, rs.getLong("email_id"));
        assertTrue(rs.wasNull(), "email_id should be NULL");
        assertEquals(phoneId, rs.getLong("phone_id"));
    }

    @Test
    @DisplayName("customer_verification_lookup should handle NULL phone")
    void testCustomerVerificationLookupWithNullPhone() throws SQLException {
        // Setup customer with only email
        long emailId = insertEmail("emailonly@gmail.com", true);
        long customerId = insertCustomer("Email", "Only", emailId, null);
        long accountId = insertAccount(customerId, "emailonly@gmail.com");

        PreparedStatement stmt = conn.prepareStatement(
                "SELECT email_id, phone_id FROM customer_verification_lookup WHERE customer_account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data even with NULL phone");
        assertEquals(emailId, rs.getLong("email_id"));
        assertEquals(0, rs.getLong("phone_id"));
        assertTrue(rs.wasNull(), "phone_id should be NULL");
    }

    // ===== CUSTOMER ACCOUNT INFO LOOKUP VIEW TESTS =====

    @Test
    @DisplayName("customer_account_info_lookup should show complete customer info")
    void testCustomerAccountInfoLookupView() throws SQLException {
        // Setup complete customer with email and phone
        long emailId = insertEmail("complete@gmail.com", true);
        long phoneId = insertPhone("+381612345678", false);
        long customerId = insertCustomer("Complete", "User", emailId, phoneId);
        long accountId = insertAccount(customerId, "complete@gmail.com");

        // Query the view
        PreparedStatement stmt = conn.prepareStatement("""
            SELECT account_id, username, activity_status, verification_status, last_login_at,
                   customer_id, first_name, last_name, email, phone, contact_type
            FROM customer_account_info_lookup WHERE account_id = ?
        """);
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return complete customer info");
        assertEquals(accountId, rs.getLong("account_id"));
        assertEquals("complete@gmail.com", rs.getString("username"));
        assertEquals("ACTIVE", rs.getString("activity_status"));
        assertEquals("UNVERIFIED", rs.getString("verification_status"));
        assertEquals(customerId, rs.getLong("customer_id"));
        assertEquals("Complete", rs.getString("first_name"));
        assertEquals("User", rs.getString("last_name"));
        assertEquals("complete@gmail.com", rs.getString("email"));
        assertEquals("+381612345678", rs.getString("phone"));
        assertEquals("BOTH", rs.getString("contact_type"));
    }

    @Test
    @DisplayName("customer_account_info_lookup should show EMAIL_ONLY contact type")
    void testCustomerAccountInfoLookupEmailOnly() throws SQLException {
        long emailId = insertEmail("emailonly@gmail.com", false);
        long customerId = insertCustomer("Email", "Only", emailId, null);
        long accountId = insertAccount(customerId, "emailonly@gmail.com");

        PreparedStatement stmt = conn.prepareStatement(
                "SELECT contact_type, email, phone FROM customer_account_info_lookup WHERE account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data");
        assertEquals("EMAIL_ONLY", rs.getString("contact_type"));
        assertEquals("emailonly@gmail.com", rs.getString("email"));
        assertNull(rs.getString("phone"));
    }

    @Test
    @DisplayName("customer_account_info_lookup should show PHONE_ONLY contact type")
    void testCustomerAccountInfoLookupPhoneOnly() throws SQLException {
        long phoneId = insertPhone("+381611111111", true);
        long customerId = insertCustomer("Phone", "Only", null, phoneId);
        long accountId = insertAccount(customerId, "+381611111111");

        PreparedStatement stmt = conn.prepareStatement(
                "SELECT contact_type, email, phone FROM customer_account_info_lookup WHERE account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data");
        assertEquals("PHONE_ONLY", rs.getString("contact_type"));
        assertNull(rs.getString("email"));
        assertEquals("+381611111111", rs.getString("phone"));
    }

    @Test
    @DisplayName("customer_account_info_lookup should show NONE contact type")
    void testCustomerAccountInfoLookupNoContact() throws SQLException {
        long customerId = insertCustomer("No", "Contact", null, null);
        long accountId = insertAccount(customerId, "nocontact");

        PreparedStatement stmt = conn.prepareStatement(
                "SELECT contact_type, email, phone FROM customer_account_info_lookup WHERE account_id = ?"
        );
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "View should return data");
        assertEquals("NONE", rs.getString("contact_type"));
        assertNull(rs.getString("email"));
        assertNull(rs.getString("phone"));
    }

    @Test
    @DisplayName("customer_account_info_lookup should handle last_login_at updates")
    void testCustomerAccountInfoLookupWithLogin() throws SQLException {
        long customerId = insertCustomer("Login", "Test", null, null);
        long accountId = insertAccount(customerId, "logintest");

        // Update last login
        PreparedStatement updateStmt = conn.prepareStatement(
                "UPDATE customer_accounts SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?"
        );
        updateStmt.setLong(1, accountId);
        updateStmt.executeUpdate();

        // Check view shows updated login time
        PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT last_login_at FROM customer_account_info_lookup WHERE account_id = ?"
        );
        selectStmt.setLong(1, accountId);
        ResultSet rs = selectStmt.executeQuery();

        assertTrue(rs.next(), "View should return data");
        assertNotNull(rs.getTimestamp("last_login_at"), "last_login_at should be set");
    }

    @Test
    @DisplayName("Both views should return same customer data consistently")
    void testViewsConsistency() throws SQLException {
        // Setup customer
        long emailId = insertEmail("consistent@gmail.com", true);
        long phoneId = insertPhone("+381612345678", false);
        long customerId = insertCustomer("Consistent", "Test", emailId, phoneId);
        long accountId = insertAccount(customerId, "consistent@gmail.com");

        // Query both views
        PreparedStatement verificationStmt = conn.prepareStatement(
                "SELECT customer_id, customer_account_id, verification_status FROM customer_verification_lookup WHERE customer_account_id = ?"
        );
        verificationStmt.setLong(1, accountId);
        ResultSet verificationRs = verificationStmt.executeQuery();

        PreparedStatement infoStmt = conn.prepareStatement(
                "SELECT customer_id, account_id, verification_status FROM customer_account_info_lookup WHERE account_id = ?"
        );
        infoStmt.setLong(1, accountId);
        ResultSet infoRs = infoStmt.executeQuery();

        assertTrue(verificationRs.next(), "Verification view should return data");
        assertTrue(infoRs.next(), "Info view should return data");

        // Both views should show same customer and verification data
        assertEquals(verificationRs.getLong("customer_id"), infoRs.getLong("customer_id"));
        assertEquals(verificationRs.getLong("customer_account_id"), infoRs.getLong("account_id"));
        assertEquals(verificationRs.getString("verification_status"), infoRs.getString("verification_status"));
    }

    // ===== HELPER METHODS =====

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