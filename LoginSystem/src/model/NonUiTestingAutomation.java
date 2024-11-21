package model;

import model.DatabaseHelper;
import model.HelpSystem;

import java.sql.SQLException;
import java.util.List;

/*
 * 
 * 
 */
public class NonUiTestingAutomation {

    static int numPassed = 0;
    static int numFailed = 0;

    public static void main(String[] args) throws SQLException {
        System.out.println("____________________________________________________________________________");
        System.out.println("\nTesting Automation for Non-UI Classes");

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance();

        // Test cases for HelpSystem
        performHelpSystemTestCase(1, "Generic Message:", " Confused about system usage.", true);
        performHelpSystemTestCase(2, "Specific Message: Need help with Topic1", "Topic1", true);

        // Test cases for account creation
        performAccountCreationTestCase(3, "admin1", "Admin", true);
        performAccountCreationTestCase(4, "admin2", "Admin", true);
        performAccountCreationTestCase(5, "admin3", "Admin", true);
        performAccountCreationTestCase(6, "student1", "Student", true);
        performAccountCreationTestCase(7, "guest", "Guest", false); // Invalid role

        // Test cases for article management
        performArticleTestCase(8, "Test Article 1", "Author A", "This is a test abstract.", "beginner", true);
        performArticleTestCase(9, "", "Author B", "Missing title.", "intermediate", false); // Missing title

        // Test cases for removing admin rights
        performRemoveAdminRightsTestCase(10, "admin3", true);
        performRemoveAdminRightsTestCase(11, "admin1", true); // Attempt removal when not enough admins

        // Test cases for backup and restore
        performBackupRestoreTestCase(12, "backup.sql", true);

        // Summary of results
        System.out.println("____________________________________________________________________________");
        System.out.println("\nNumber of tests passed: " + numPassed);
        System.out.println("Number of tests failed: " + numFailed);
    }

    /**
     * Test case for HelpSystem messages.
     */
    private static void performHelpSystemTestCase(int testCase, String message, String queryOrNull, boolean expectedPass) {
        System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
        System.out.println("Input: \"" + message + "\"");
        System.out.println("______________");

        try {
            if (queryOrNull == null) {
                HelpSystem.sendGenericMessage(message);
                List<String> genericMessages = HelpSystem.getGenericMessages();
                if (genericMessages.contains(message)) {
                    handleTestResult(expectedPass, true, "Generic message successfully added.");
                } else {
                    handleTestResult(expectedPass, false, "Generic message not found after addition.");
                }
            } else {
                HelpSystem.sendSpecificMessage(queryOrNull, message);
                List<String> specificMessages = HelpSystem.getSpecificMessages(queryOrNull);
                if (specificMessages.contains(message)) {
                    handleTestResult(expectedPass, true, "Specific message successfully added for query: " + queryOrNull);
                } else {
                    handleTestResult(expectedPass, false, "Specific message not found after addition.");
                }
            }
        } catch (Exception e) {
            handleTestResult(expectedPass, false, "Exception during HelpSystem operation: " + e.getMessage());
        }
    }

    /**
     * Test case for account creation.
     */
    private static void performAccountCreationTestCase(int testCase, String username, String role, boolean expectedPass) {
        System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
        System.out.println("Input: Username = \"" + username + "\", Role = \"" + role + "\"");
        System.out.println("______________");

        try {
            boolean accountCreated = createAccount(username, role);
            handleTestResult(expectedPass, accountCreated, "Account creation result: " + accountCreated);
        } catch (Exception e) {
            handleTestResult(expectedPass, false, "Exception during account creation: " + e.getMessage());
        }
    }

    /**
     * Test case for adding articles.
     */
    private static void performArticleTestCase(int testCase, String title, String authors, String abstractText, String level, boolean expectedPass) {
        System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
        System.out.println("Input: Title = \"" + title + "\", Authors = \"" + authors + "\", Level = \"" + level + "\"");
        System.out.println("______________");

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
            databaseHelper.addArticle(title, authors, abstractText, level, "This is a test body", "References", false);
            List<String> articles = databaseHelper.listArticles();
            boolean articleAdded = articles.stream().anyMatch(article -> article.contains(title));
            handleTestResult(expectedPass, articleAdded, "Article added result: " + articleAdded);
        } catch (Exception e) {
            handleTestResult(expectedPass, false, "Exception during article addition: " + e.getMessage());
        }
    }

    /**
     * Test case for removing admin rights.
     */
    private static void performRemoveAdminRightsTestCase(int testCase, String username, boolean expectedPass) {
        System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
        System.out.println("Input: Username = \"" + username + "\"");
        System.out.println("______________");

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance();

            // Check if there are enough admins
            List<String> admins = databaseHelper.getAdminAccounts();
            if (admins.size() > 2) {
                databaseHelper.removeAdminRights(username);
                admins = databaseHelper.getAdminAccounts();
                boolean adminRemoved = !admins.contains(username);
                handleTestResult(expectedPass, adminRemoved, "Admin rights removed successfully.");
            } else {
                handleTestResult(expectedPass, !expectedPass, "Insufficient admins to remove rights.");
            }
        } catch (Exception e) {
            handleTestResult(expectedPass, false, "Exception during admin rights removal: " + e.getMessage());
        }
    }

    /**
     * Test case for backup and restore operations.
     */
    private static void performBackupRestoreTestCase(int testCase, String backupFileName, boolean expectedPass) {
        System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
        System.out.println("Backup File: \"" + backupFileName + "\"");
        System.out.println("______________");

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance();

            // Backup articles
            databaseHelper.backupArticles(backupFileName);

            // Restore articles
            databaseHelper.restoreArticles(backupFileName);

            // Verify backup and restore
            List<String> articles = databaseHelper.listArticles();
            boolean backupRestored = !articles.isEmpty();
            handleTestResult(expectedPass, backupRestored, "Backup and restore operation succeeded.");
        } catch (Exception e) {
            handleTestResult(expectedPass, false, "Exception during backup/restore operation: " + e.getMessage());
        }
    }

    /**
     * Simulates account creation.
     */
    private static boolean createAccount(String username, String role) {
        if (username.isEmpty() || (!role.equals("Admin") && !role.equals("Instructor") && !role.equals("Student"))) {
            return false;
        }
        return true;
    }

    /**
     * Handles the result of a test case.
     */
    private static void handleTestResult(boolean expectedPass, boolean actualPass, String successMessage) {
        if (expectedPass == actualPass) {
            System.out.println("***Success*** " + successMessage);
            numPassed++;
        } else {
            System.out.println("***Failure*** Expected: " + expectedPass + ", but got: " + actualPass);
            numFailed++;
        }
    }
}