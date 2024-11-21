package model;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * Singleton helper class to manage database connections and operations
 * for managing special access groups, users, and related articles.
 */
public class DatabaseHelper {
    private static DatabaseHelper instance; // Singleton instance
    private Connection connection; // Database connection instance

    /**
     * Private constructor to initialize the database connection and setup the schema.
     *
     * @throws SQLException if a database access error occurs or the URL is invalid.
     */
    private DatabaseHelper() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        setupDatabase();
    }

    /**
     * Provides the active database connection.
     *
     * @return the database connection object.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Provides a singleton instance of the DatabaseHelper.
     *
     * @return the singleton instance of DatabaseHelper.
     * @throws SQLException if there is an issue creating the instance.
     */
    public static DatabaseHelper getInstance() throws SQLException {
        if (instance == null) instance = new DatabaseHelper();
        return instance;
    }

    /**
     * Initializes and sets up the database schema, creating or altering
     * necessary tables for managing groups, users, and articles.
     *
     * @throws SQLException if a database error occurs during table creation or modification.
     */
    private void setupDatabase() throws SQLException {
        // SQL for creating the table to store group information.
        String createGroupsTable = """
            CREATE TABLE IF NOT EXISTS SpecialAccessGroups (
                groupId VARCHAR(255) PRIMARY KEY,
                groupName VARCHAR(255) UNIQUE NOT NULL,
                groupType VARCHAR(50) NOT NULL
            );
        """;

        // SQL for altering the groups table to ensure the presence of the groupType column.
        String alterGroupsTable = """
            ALTER TABLE SpecialAccessGroups ADD COLUMN IF NOT EXISTS groupType VARCHAR(50) NOT NULL DEFAULT 'General';
        """;

        // SQL for creating a table to store group-user relationships.
        String createGroupUsersTable = """
            CREATE TABLE IF NOT EXISTS GroupUsers (
                groupId VARCHAR(255),
                username VARCHAR(255),
                role VARCHAR(50), -- admin, instructor, student
                canView BOOLEAN DEFAULT FALSE,
                canAdmin BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (groupId, username),
                FOREIGN KEY (groupId) REFERENCES SpecialAccessGroups(groupId) ON DELETE CASCADE
            );
        """;

        // SQL for creating a table to store group-article relationships.
        String createGroupArticlesTable = """
            CREATE TABLE IF NOT EXISTS GroupArticles (
                groupId VARCHAR(255),
                articleId INT,
                PRIMARY KEY (groupId, articleId),
                FOREIGN KEY (groupId) REFERENCES SpecialAccessGroups(groupId) ON DELETE CASCADE,
                FOREIGN KEY (articleId) REFERENCES Articles(id)
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createGroupsTable); // Create groups table if it doesn't exist
            stmt.execute(alterGroupsTable); // Add missing groupType column if needed
            stmt.execute(createGroupUsersTable); // Create group-users relationship table
            stmt.execute(createGroupArticlesTable); // Create group-articles relationship table
        }
    }

    /**
     * Updates or modifies specific rights for a given group.
     *
     * @param groupId the ID of the group to modify.
     * @param column  the column representing the right to be updated (e.g., canView, canAdmin).
     * @param value   the new value for the right (true or false).
     * @throws SQLException if a database error occurs during the update.
     */
    private void modifyGroupRights(String groupId, String column, boolean value) throws SQLException {
        String sql = """
            MERGE INTO AccessRights (groupId, %s) KEY(groupId) VALUES (?, ?);
        """.formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setBoolean(2, value);
            pstmt.executeUpdate();
        }
    }

    /**
     * Grants view rights to a group.
     *
     * @param groupId the ID of the group to grant view rights to.
     * @throws SQLException if a database error occurs.
     */
    public void addGroupViewRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canView", true);
    }

    /**
     * Revokes view rights from a group.
     *
     * @param groupId the ID of the group to revoke view rights from.
     * @throws SQLException if a database error occurs.
     */
    public void removeGroupViewRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canView", false);
    }

    /**
     * Grants admin rights to a group.
     *
     * @param groupId the ID of the group to grant admin rights to.
     * @throws SQLException if a database error occurs.
     */
    public void addGroupAdminRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canAdmin", true);
    }

    /**
     * Revokes admin rights from a group.
     *
     * @param groupId the ID of the group to revoke admin rights from.
     * @throws SQLException if a database error occurs.
     */
    public void removeGroupAdminRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canAdmin", false);
    }


    /**
     * Checks a specific access right for a group.
     *
     * @param groupId the ID of the group to check.
     * @param column  the column representing the right to check (e.g., canView, canAdmin).
     * @return true if the group has the specified right, false otherwise.
     * @throws SQLException if a database error occurs during the check.
     */
    private boolean checkGroupRights(String groupId, String column) throws SQLException {
        String sql = "SELECT %s FROM AccessRights WHERE groupId = ?;".formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(column);
            }
        }
    }

    /**
     * Determines if a group has view rights.
     *
     * @param groupId the ID of the group to check.
     * @return true if the group has view rights, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean hasGroupViewRights(String groupId) throws SQLException {
        return checkGroupRights(groupId, "canView");
    }

    /**
     * Determines if a group has admin rights.
     *
     * @param groupId the ID of the group to check.
     * @return true if the group has admin rights, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean hasGroupAdminRights(String groupId) throws SQLException {
        return checkGroupRights(groupId, "canAdmin");
    }

    /**
     * Adds a new article to the database and assigns appropriate group access rights based on encryption status.
     *
     * @param title        the title of the article.
     * @param authors      the authors of the article.
     * @param abstractText the abstract of the article.
     * @param keywords     keywords associated with the article.
     * @param body         the body content of the article.
     * @param references   the references cited in the article.
     * @param isEncrypted  whether the article is encrypted.
     * @throws SQLException if a database error occurs during the insertion.
     */
    public void addArticle(String title, String authors, String abstractText, String keywords, String body, String references, boolean isEncrypted) throws SQLException {
        String sql = "INSERT INTO Articles (title, authors, abstractText, keywords, body, references, isEncrypted) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, authors);
            pstmt.setString(3, abstractText);
            pstmt.setString(4, keywords);
            pstmt.setString(5, body);
            pstmt.setString(6, references);
            pstmt.setBoolean(7, isEncrypted);
            pstmt.executeUpdate();

            // Retrieve the generated article ID.
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int articleId = generatedKeys.getInt(1);
                String groupId = "article-" + articleId;

                // Set default access rights based on encryption status.
                if (isEncrypted) {
                    // For encrypted articles, grant admin rights and revoke view rights.
                    addGroupAdminRights(groupId);
                    removeGroupViewRights(groupId);
                } else {
                    // For unencrypted articles, grant view rights and revoke admin rights.
                    addGroupViewRights(groupId);
                    removeGroupAdminRights(groupId);
                }
            } else {
                throw new SQLException("Failed to obtain article ID.");
            }
        }
    }

    /**
     * Lists all articles in the database with their display ID, title, and authors.
     *
     * @return a list of formatted strings representing the articles.
     * @throws SQLException if a database error occurs during the retrieval.
     */
    public List<String> listArticles() throws SQLException {
        String sql = "SELECT id, title, authors FROM Articles ORDER BY id";
        List<String> articles = new ArrayList<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int displayId = 1;
            while (rs.next()) {
                articles.add("ID: " + displayId++ + ", Title: " + rs.getString("title") + ", Authors: " + rs.getString("authors"));
            }
        }
        return articles;
    }

    /**
     * Retrieves the database ID corresponding to a given display ID for articles.
     *
     * @param displayId the display ID to look up.
     * @return the database ID corresponding to the given display ID.
     * @throws SQLException if the display ID is invalid or a database error occurs.
     */
    public int getDatabaseIdForDisplayId(int displayId) throws SQLException {
        String sql = "SELECT id FROM Articles ORDER BY id";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int currentDisplayId = 1;
            while (rs.next()) {
                if (currentDisplayId++ == displayId) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Invalid display ID: " + displayId);
    }

    /**
     * Retrieves and displays the details of an article, decrypting the body if necessary.
     *
     * @param displayId the display ID of the article.
     * @return a formatted string containing the article's details, or an error message if not found.
     * @throws SQLException if a database error occurs.
     */
    public String viewArticle(int displayId) throws SQLException {
        int articleId = getDatabaseIdForDisplayId(displayId); // Get actual DB ID from display ID
        String sql = "SELECT * FROM Articles WHERE id = ?";
        StringBuilder articleDetails = new StringBuilder();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, articleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean isEncrypted = rs.getBoolean("isEncrypted");
                    String body = rs.getString("body");

                    // Decrypt body if encrypted
                    if (isEncrypted) {
                        body = decryptContent(body);
                    }

                    articleDetails.append("ID: ").append(displayId) // Show display ID, not DB ID
                            .append("\nTitle: ").append(rs.getString("title"))
                            .append("\nAuthors: ").append(rs.getString("authors"))
                            .append("\nAbstract: ").append(rs.getString("abstractText"))
                            .append("\nKeywords: ").append(rs.getString("keywords"))
                            .append("\nBody: ").append(body)
                            .append("\nReferences: ").append(rs.getString("references"));
                } else {
                    return "Article not found.";
                }
            }
        }
        return articleDetails.toString();
    }

    /**
     * Deletes an article from the database, removing associated entries in GroupArticles.
     *
     * @param displayId the display ID of the article to delete.
     * @throws SQLException if a database error occurs during deletion.
     */
    public void deleteArticle(int displayId) throws SQLException {
        int articleId = getDatabaseIdForDisplayId(displayId);

        // Step 1: Delete references to the article in GroupArticles
        String deleteFromGroupArticlesSQL = "DELETE FROM GroupArticles WHERE articleId = ?";
        try (PreparedStatement pstmtGroupArticles = connection.prepareStatement(deleteFromGroupArticlesSQL)) {
            pstmtGroupArticles.setInt(1, articleId);
            pstmtGroupArticles.executeUpdate();
        }

        // Step 2: Delete the article from the Articles table
        String deleteFromArticlesSQL = "DELETE FROM Articles WHERE id = ?";
        try (PreparedStatement pstmtArticles = connection.prepareStatement(deleteFromArticlesSQL)) {
            pstmtArticles.setInt(1, articleId);
            pstmtArticles.executeUpdate();
        }
    }

    /**
     * Backs up all articles to a specified file.
     *
     * @param backupFileName the file path to store the backup.
     * @throws SQLException if a database error occurs during the backup operation.
     */
    public void backupArticles(String backupFileName) throws SQLException {
        String backupSQL = String.format("SCRIPT TO '%s'", backupFileName);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(backupSQL);
        }
    }

    /**
     * Restores articles from a backup file, replacing the current Articles table.
     *
     * @param backupFileName the file path of the backup to restore.
     * @throws SQLException if a database error occurs during the restore operation.
     */
    public void restoreArticles(String backupFileName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS Articles");
            stmt.execute(String.format("RUNSCRIPT FROM '%s'", backupFileName));
        }
    }

    /**
     * Modifies specific user rights in the AccessRights table.
     *
     * @param username the username whose rights are being modified.
     * @param column   the column representing the right to modify (e.g., canView, canAdmin).
     * @param value    the new value for the right (true or false).
     * @throws SQLException if a database error occurs.
     */
    private void modifyUserRights(String username, String column, boolean value) throws SQLException {
        String sql = """
            MERGE INTO AccessRights (username, %s) KEY(username) VALUES (?, ?);
        """.formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setBoolean(2, value);
            pstmt.executeUpdate();
        }
    }

    /**
     * Grants view rights to a user.
     *
     * @param username the username to grant view rights to.
     * @throws SQLException if a database error occurs.
     */
    public void addViewRights(String username) throws SQLException {
        modifyUserRights(username, "canView", true);
    }

    /**
     * Revokes view rights from a user.
     *
     * @param username the username to revoke view rights from.
     * @throws SQLException if a database error occurs.
     */
    public void removeViewRights(String username) throws SQLException {
        modifyUserRights(username, "canView", false);
    }

    /**
     * Grants admin rights to a user.
     *
     * @param username the username to grant admin rights to.
     * @throws SQLException if a database error occurs.
     */
    public void addAdminRights(String username) throws SQLException {
        modifyUserRights(username, "canAdmin", true);
    }

    /**
     * Revokes admin rights from a user.
     *
     * @param username the username to revoke admin rights from.
     * @throws SQLException if a database error occurs.
     */
    public void removeAdminRights(String username) throws SQLException {
        modifyUserRights(username, "canAdmin", false);
    }

    /**
     * Checks a specific access right for a user.
     *
     * @param username the username to check.
     * @param column   the column representing the right to check (e.g., canView, canAdmin).
     * @return true if the user has the specified right, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    private boolean checkUserRights(String username, String column) throws SQLException {
        String sql = "SELECT %s FROM AccessRights WHERE username = ?;".formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(column);
            }
        }
    }

    /**
     * Determines if a user has view rights.
     *
     * @param username the username to check.
     * @return true if the user has view rights, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean hasViewRights(String username) throws SQLException {
        return checkUserRights(username, "canView");
    }

    /**
     * Determines if a user has admin rights.
     *
     * @param username the username to check.
     * @return true if the user has admin rights, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean hasAdminRights(String username) throws SQLException {
        return checkUserRights(username, "canAdmin");
    }

    /**
     * Encrypts content using Base64 encoding. Replace with a stronger algorithm for production.
     *
     * @param content the content to encrypt.
     * @return the encrypted content as a Base64-encoded string.
     */
    public static String encryptContent(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts content encoded in Base64.
     *
     * @param encryptedContent the Base64-encoded encrypted content.
     * @return the decrypted content as a string.
     */
    public static String decryptContent(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(encryptedContent), StandardCharsets.UTF_8);
    }

    /**
     * Checks whether an article is encrypted.
     *
     * @param articleId the ID of the article to check.
     * @return true if the article is encrypted, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean isArticleEncrypted(int articleId) throws SQLException {
        String sql = "SELECT isEncrypted FROM Articles WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, articleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("isEncrypted");
                }
            }
        }
        return false;
    }

    /**
     * Searches articles based on a query, filtering by level and group if specified.
     *
     * @param query the search query to filter articles by title, authors, or abstract.
     * @param level the level filter (e.g., keyword matching); "All" to ignore this filter.
     * @param group the group filter (e.g., group ID with view access); "All" to ignore this filter.
     * @return a list of formatted strings representing the matching articles.
     * @throws SQLException if a database error occurs during the search.
     */
    public List<String> searchArticles(String query, String level, String group) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, title, authors, abstractText FROM Articles WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        // Add search query to the SQL
        if (!query.isEmpty()) {
            sql.append(" AND (title LIKE ? OR authors LIKE ? OR abstractText LIKE ?)");
            String likeQuery = "%" + query + "%";
            parameters.add(likeQuery);
            parameters.add(likeQuery);
            parameters.add(likeQuery);
        }

        // Add level filtering if needed
        if (!"All".equalsIgnoreCase(level)) {
            sql.append(" AND keywords LIKE ?");
            parameters.add("%" + level + "%");
        }

        // Add group filtering if needed
        if (!"All".equalsIgnoreCase(group)) {
            sql.append(" AND id IN (SELECT id FROM AccessRights WHERE groupId = ? AND canView = TRUE)");
            parameters.add(group);
        }

        sql.append(" ORDER BY id");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            // Execute query and collect results
            List<String> results = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                int sequence = 1;
                while (rs.next()) {
                    results.add(String.format("Seq: %d, Title: %s, Authors: %s, Abstract: %s",
                            sequence++, rs.getString("title"), rs.getString("authors"), rs.getString("abstractText")));
                }
            }
            return results;
        }
    }
    
    /**
     * Retrieves detailed information about an article based on its sequence in the list.
     *
     * @param sequence the sequence number of the article.
     * @return a formatted string with the article's details.
     * @throws SQLException if the article is not found or a database error occurs.
     */
    public String getArticleDetails(int sequence) throws SQLException {
        String sql = "SELECT * FROM Articles ORDER BY id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                int currentSequence = 1;
                while (rs.next()) {
                    if (currentSequence++ == sequence) {
                        boolean isEncrypted = rs.getBoolean("isEncrypted");
                        String body = rs.getString("body");

                        // Decrypt body if encrypted
                        if (isEncrypted) {
                            body = decryptContent(body);
                        }

                        return String.format("ID: %d\nTitle: %s\nAuthors: %s\nAbstract: %s\nKeywords: %s\nBody: %s\nReferences: %s",
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("authors"),
                                rs.getString("abstractText"),
                                rs.getString("keywords"),
                                body,
                                rs.getString("references"));
                    }
                }
            }
        }
        throw new SQLException("Article not found.");
    }

    /**
     * Generates statistics about article levels based on their keywords.
     *
     * @param articles a list of article IDs to consider for statistics.
     * @return a formatted string with counts of articles at different levels (Beginner, Intermediate, Advanced, Expert).
     * @throws SQLException if a database error occurs.
     */
    public String getLevelStatistics(List<String> articles) throws SQLException {
        String sql = "SELECT keywords FROM Articles WHERE id IN (SELECT id FROM Articles)";
        int beginnerCount = 0, intermediateCount = 0, advancedCount = 0, expertCount = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String keywords = rs.getString("keywords").toLowerCase();
                    if (keywords.contains("beginner")) beginnerCount++;
                    if (keywords.contains("intermediate")) intermediateCount++;
                    if (keywords.contains("advanced")) advancedCount++;
                    if (keywords.contains("expert")) expertCount++;
                }
            }
        }

        return String.format("Beginner: %d, Intermediate: %d, Advanced: %d, Expert: %d",
                beginnerCount, intermediateCount, advancedCount, expertCount);
    }

    /**
     * Creates a new group with the specified name and type.
     *
     * @param groupName      the name of the group to create.
     * @param isSpecialGroup whether the group is special or general.
     * @throws SQLException if a database error occurs during group creation.
     */
    public void createGroup(String groupName, boolean isSpecialGroup) throws SQLException {
        String groupId = UUID.randomUUID().toString();
        String groupType = isSpecialGroup ? "Special" : "General";
        String sql = "INSERT INTO SpecialAccessGroups (groupId, groupName, groupType) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, groupName);
            pstmt.setString(3, groupType);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves the group ID associated with the given group name.
     *
     * @param groupName the name of the group.
     * @return the group ID corresponding to the provided group name.
     * @throws SQLException if the group is not found or a database error occurs.
     */
    public String getGroupIdByName(String groupName) throws SQLException {
        String sql = "SELECT groupId FROM SpecialAccessGroups WHERE groupName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("groupId");
                }
            }
        }
        throw new SQLException("Group not found: " + groupName);
    }

    /**
     * Adds a user to a group, assigning role-specific permissions. The first instructor in a group is granted admin rights.
     *
     * @param groupId  the ID of the group to add the user to.
     * @param username the username of the user to add.
     * @param role     the role of the user in the group (e.g., "Instructor").
     * @throws SQLException if a database error occurs.
     */
    public void addUserToGroup(String groupId, String username, String role) throws SQLException {
        String sqlCheckAdmins = "SELECT COUNT(*) FROM GroupUsers WHERE groupId = ? AND canAdmin = TRUE";
        String sqlInsertOrUpdate = """
            MERGE INTO GroupUsers (groupId, username, role, canView, canAdmin)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmtCheckAdmins = connection.prepareStatement(sqlCheckAdmins)) {
            pstmtCheckAdmins.setString(1, groupId);
            try (ResultSet rs = pstmtCheckAdmins.executeQuery()) {
                boolean isFirstInstructor = false;

                if (rs.next() && rs.getInt(1) == 0 && role.equalsIgnoreCase("Instructor")) {
                    isFirstInstructor = true; // First instructor gets admin rights
                }

                try (PreparedStatement pstmtInsertOrUpdate = connection.prepareStatement(sqlInsertOrUpdate)) {
                    pstmtInsertOrUpdate.setString(1, groupId);
                    pstmtInsertOrUpdate.setString(2, username);
                    pstmtInsertOrUpdate.setString(3, role); // Insert role
                    pstmtInsertOrUpdate.setBoolean(4, false); // Can view
                    pstmtInsertOrUpdate.setBoolean(5, isFirstInstructor); // Admin rights if first instructor
                    pstmtInsertOrUpdate.executeUpdate();
                }
            }
        }
    }

    /**
     * Retrieves a list of users in a group with their roles and permissions.
     *
     * @param groupId the ID of the group.
     * @return a list of maps where each map represents a user with attributes (username, role, canView, canAdmin).
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, String>> getUsersInGroup(String groupId) throws SQLException {
        String sql = """
            SELECT gu.username, gu.role, gu.canView, gu.canAdmin
            FROM GroupUsers gu
            WHERE gu.groupId = ?
        """;

        List<Map<String, String>> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> user = new HashMap<>();
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role")); // Get role from the database
                    user.put("canView", rs.getBoolean("canView") ? "Yes" : "No");
                    user.put("canAdmin", rs.getBoolean("canAdmin") ? "Yes" : "No");
                    users.add(user);
                }
            }
        }

        return users;
    }

    /**
     * Updates the view rights of a user in a group.
     *
     * @param groupId the ID of the group.
     * @param username the username of the user to update.
     * @param canView whether the user should have view rights.
     * @throws SQLException if a database error occurs.
     */
    public void updateUserViewRights(String groupId, String username, boolean canView) throws SQLException {
        String sql = "UPDATE GroupUsers SET canView = ? WHERE groupId = ? AND username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, canView);
            pstmt.setString(2, groupId);
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Updates the admin rights of a user in a group, ensuring there is always at least one admin.
     *
     * @param groupId the ID of the group.
     * @param username the username of the user to update.
     * @param canAdmin whether the user should have admin rights.
     * @throws SQLException if the update would result in no admins in the group or a database error occurs.
     */
    public void updateUserAdminRights(String groupId, String username, boolean canAdmin) throws SQLException {
        // Count the number of current admins in the group
        int adminCount = countAdminsInGroup(groupId);

        // Prevent removing admin rights if it results in no admins in the group
        if (!canAdmin && adminCount == 1) {
            throw new SQLException("There must be at least one admin in the group.");
        }

        // Update the admin rights in the database
        String sql = "UPDATE GroupUsers SET canAdmin = ? WHERE groupId = ? AND username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, canAdmin);
            pstmt.setString(2, groupId);
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Counts the number of admin users in a specified group.
     *
     * @param groupId the ID of the group to count admins for.
     * @return the number of admins in the group.
     * @throws SQLException if a database error occurs.
     */
    public int countAdminsInGroup(String groupId) throws SQLException {
        String sql = "SELECT COUNT(*) AS adminCount FROM GroupUsers WHERE groupId = ? AND canAdmin = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("adminCount");
                }
            }
        }
        return 0; // Return 0 if no admins are found
    }

    /**
     * Grants admin rights to a user within a specific group.
     *
     * @param groupId  the ID of the group.
     * @param username the username of the user to grant admin rights to.
     * @throws SQLException if a database error occurs.
     */
    public void grantAdminRights(String groupId, String username) throws SQLException {
        updateUserPermissions(groupId, username, true, true);
    }

    /**
     * Grants view rights to a user within a specific group.
     *
     * @param groupId  the ID of the group.
     * @param username the username of the user to grant view rights to.
     * @throws SQLException if a database error occurs.
     */
    public void grantViewRights(String groupId, String username) throws SQLException {
        updateUserPermissions(groupId, username, true, false);
    }

    /**
     * Updates the permissions (view and admin rights) of a user in a group.
     *
     * @param groupId  the ID of the group.
     * @param username the username of the user to update.
     * @param canView  whether the user should have view rights.
     * @param canAdmin whether the user should have admin rights.
     * @throws SQLException if a database error occurs.
     */
    private void updateUserPermissions(String groupId, String username, boolean canView, boolean canAdmin) throws SQLException {
        String sql = "UPDATE GroupUsers SET canView = ?, canAdmin = ? WHERE groupId = ? AND username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, canView);
            pstmt.setBoolean(2, canAdmin);
            pstmt.setString(3, groupId);
            pstmt.setString(4, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves all users in a group with their roles and permissions.
     *
     * @param groupId the ID of the group.
     * @return a list of maps where each map represents a user's attributes (username, role, canView, canAdmin).
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, String>> getGroupUsers(String groupId) throws SQLException {
        String sql = "SELECT username, role, canView, canAdmin FROM GroupUsers WHERE groupId = ?";
        List<Map<String, String>> users = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> user = new HashMap<>();
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("canView", rs.getBoolean("canView") ? "Yes" : "No");
                    user.put("canAdmin", rs.getBoolean("canAdmin") ? "Yes" : "No");
                    users.add(user);
                }
            }
        }
        return users;
    }

    /**
     * Adds an article to a group, ensuring the article exists in the Articles table.
     *
     * @param groupId    the ID of the group to add the article to.
     * @param articleId  the ID of the article to add.
     * @param isEncrypted whether the article is encrypted (currently unused in this method).
     * @throws SQLException if the article does not exist or a database error occurs.
     */
    public void addArticleToGroup(String groupId, int articleId, boolean isEncrypted) throws SQLException {
        // Query to validate the article ID
        String checkArticleSql = "SELECT id FROM Articles WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkArticleSql)) {
            checkStmt.setInt(1, articleId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Article with ID " + articleId + " does not exist in Articles table.");
                } else {
                    System.out.println("Article with ID " + articleId + " exists in Articles table.");
                }
            }
        }

        // Add the article to the group
        String sql = "INSERT INTO GroupArticles (groupId, articleId) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setInt(2, articleId);
            pstmt.executeUpdate();
            System.out.println("Article with ID " + articleId + " successfully added to groupId: " + groupId);
        }
    }

    /**
     * Retrieves a list of articles in a group visible to a specific user.
     *
     * @param groupId  the ID of the group.
     * @param username the username of the user to check permissions for.
     * @return a list of maps where each map represents an article's details (id, title, and body).
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, String>> getArticlesInGroup(String groupId, String username) throws SQLException {
        String sql = """
            SELECT a.id, a.title, a.body, a.isEncrypted, gu.canView
            FROM Articles a
            JOIN GroupArticles ga ON a.id = ga.articleId
            JOIN GroupUsers gu ON ga.groupId = gu.groupId
            WHERE ga.groupId = ? AND gu.username = ?
        """;

        List<Map<String, String>> articles = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> article = new HashMap<>();
                    article.put("id", String.valueOf(rs.getInt("id")));
                    article.put("title", rs.getString("title"));

                    boolean canView = rs.getBoolean("canView");
                    boolean isEncrypted = rs.getBoolean("isEncrypted");
                    String body = rs.getString("body");

                    if (canView) {
                        body = isEncrypted ? DatabaseHelper.decryptContent(body) : body;
                    } else {
                        body = "No Permission";
                    }

                    article.put("body", body);
                    articles.add(article);
                }
            }
        }
        return articles;
    }

    /**
     * Deletes a group from the database by its ID.
     *
     * @param groupId the ID of the group to delete.
     * @throws SQLException if no group is found with the given ID or a database error occurs.
     */
    public void deleteGroup(String groupId) throws SQLException {
        String deleteGroupSQL = "DELETE FROM SpecialAccessGroups WHERE groupId = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteGroupSQL)) {
            pstmt.setString(1, groupId);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted == 0) {
                throw new SQLException("No group found with ID: " + groupId);
            }
        }
    }

    /**
     * Retrieves a list of all groups in the database.
     *
     * @return a list of maps where each map represents a group's attributes (groupId and groupName).
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, String>> getAllGroups() throws SQLException {
        String sql = "SELECT id AS groupId, name AS groupName FROM Groups";
        List<Map<String, String>> groups = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, String> group = new HashMap<>();
                group.put("groupId", rs.getString("groupId"));
                group.put("groupName", rs.getString("groupName"));
                groups.add(group);
            }
        }

        return groups;
    }

    /**
     * Deletes a user from a group based on group ID and username.
     *
     * @param groupId  the ID of the group.
     * @param username the username of the user to remove.
     * @return true if the user was successfully removed, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean deleteUserFromGroup(String groupId, String username) throws SQLException {
        String deleteSQL = "DELETE FROM ACCESSRIGHTS WHERE GROUPID = ? AND USERNAME = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if a user was successfully deleted
        }
    }

    /**
     * Backs up the groups table to a specified file.
     *
     * @param backupFileName the file path to store the backup.
     * @throws SQLException if a database error occurs during the backup operation.
     */
    public void backupGroups(String backupFileName) throws SQLException {
        String backupSQL = String.format("SCRIPT TO '%s'", backupFileName);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(backupSQL);
        }
    }

    /**
     * Restores the groups table from a specified backup file.
     *
     * @param backupFileName the file path of the backup to restore.
     * @throws SQLException if a database error occurs during the restore operation.
     */
    public void restoreGroups(String backupFileName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Drop the existing groups table to avoid conflicts during restore
            stmt.execute("DROP TABLE IF EXISTS Groups");
            // Run the SQL script to restore the groups table
            stmt.execute(String.format("RUNSCRIPT FROM '%s'", backupFileName));
        }
    }

    /**
     * Retrieves a list of all admin accounts from the database.
     *
     * @return a list of usernames with admin rights.
     * @throws SQLException if a database error occurs.
     */
    public List<String> getAdminAccounts() throws SQLException {
        String sql = "SELECT username FROM AccessRights WHERE canAdmin = TRUE";
        List<String> admins = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                admins.add(rs.getString("username"));
            }
        }
        return admins;
    }
}
