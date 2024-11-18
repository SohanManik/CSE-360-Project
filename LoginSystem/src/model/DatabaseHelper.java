package model;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class DatabaseHelper {
    private static DatabaseHelper instance;
    private Connection connection;

    private DatabaseHelper() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        setupDatabase();
    }
    
    public Connection getConnection() {
        return connection;
    }

    public static DatabaseHelper getInstance() throws SQLException {
        if (instance == null) instance = new DatabaseHelper();
        return instance;
    }

    private void setupDatabase() throws SQLException {
        String createSpecialAccessGroupsTable = """
            CREATE TABLE IF NOT EXISTS SpecialAccessGroups (
                groupId VARCHAR(255) PRIMARY KEY,
                groupName VARCHAR(255)
            );
        """;

        String createGroupUsersTable = """
            CREATE TABLE IF NOT EXISTS GroupUsers (
                groupId VARCHAR(255),
                username VARCHAR(255),
                role VARCHAR(50), -- admin, instructor, student
                canView BOOLEAN DEFAULT FALSE,
                canAdmin BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (groupId, username)
            );
        """;

        String createGroupArticlesTable = """
            CREATE TABLE IF NOT EXISTS GroupArticles (
                groupId VARCHAR(255),
                articleId INT,
                PRIMARY KEY (groupId, articleId),
                FOREIGN KEY (articleId) REFERENCES Articles(id)
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSpecialAccessGroupsTable);
            stmt.execute(createGroupUsersTable);
            stmt.execute(createGroupArticlesTable);
        }
    }

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

    public void addGroupViewRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canView", true);
    }

    public void removeGroupViewRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canView", false);
    }

    public void addGroupAdminRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canAdmin", true);
    }

    public void removeGroupAdminRights(String groupId) throws SQLException {
        modifyGroupRights(groupId, "canAdmin", false);
    }

    private boolean checkGroupRights(String groupId, String column) throws SQLException {
        String sql = "SELECT %s FROM AccessRights WHERE groupId = ?;".formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(column);
            }
        }
    }

    public boolean hasGroupViewRights(String groupId) throws SQLException {
        return checkGroupRights(groupId, "canView");
    }

    public boolean hasGroupAdminRights(String groupId) throws SQLException {
        return checkGroupRights(groupId, "canAdmin");
    }

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

            // Get the generated article ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int articleId = generatedKeys.getInt(1);
                String groupId = "article-" + articleId;

                // Set default access rights
                if (isEncrypted) {
                    // For encrypted articles, viewRights off, adminRights on
                    addGroupAdminRights(groupId);
                    removeGroupViewRights(groupId);
                } else {
                    // For decrypted articles, viewRights on, adminRights off
                    addGroupViewRights(groupId);
                    removeGroupAdminRights(groupId);
                }
            } else {
                throw new SQLException("Failed to obtain article ID.");
            }
        }
    }


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


    public void deleteArticle(int displayId) throws SQLException {
        int articleId = getDatabaseIdForDisplayId(displayId);
        String sql = "DELETE FROM Articles WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, articleId);
            pstmt.executeUpdate();
        }
    }

    public void backupArticles(String backupFileName) throws SQLException {
        String backupSQL = String.format("SCRIPT TO '%s'", backupFileName);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(backupSQL);
        }
    }

    public void restoreArticles(String backupFileName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS Articles");
            stmt.execute(String.format("RUNSCRIPT FROM '%s'", backupFileName));
        }
    }

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

    public void addViewRights(String username) throws SQLException {
        modifyUserRights(username, "canView", true);
    }

    public void removeViewRights(String username) throws SQLException {
        modifyUserRights(username, "canView", false);
    }

    public void addAdminRights(String username) throws SQLException {
        modifyUserRights(username, "canAdmin", true);
    }

    public void removeAdminRights(String username) throws SQLException {
        modifyUserRights(username, "canAdmin", false);
    }

    private boolean checkUserRights(String username, String column) throws SQLException {
        String sql = "SELECT %s FROM AccessRights WHERE username = ?;".formatted(column);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getBoolean(column);
            }
        }
    }

    public boolean hasViewRights(String username) throws SQLException {
        return checkUserRights(username, "canView");
    }

    public boolean hasAdminRights(String username) throws SQLException {
        return checkUserRights(username, "canAdmin");
    }
    
    // Utility methods for encryption and decryption
    public static String encryptContent(String content) {
        // Simple example using Base64 (replace with a stronger algorithm for real-world use)
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptContent(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return "";
        }
        // Use Base64 decoding (adjust if your encryption is different)
        return new String(Base64.getDecoder().decode(encryptedContent), StandardCharsets.UTF_8);
    }
    
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
        return false;    }
    
    
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

 // Special Group Management
    public void createSpecialAccessGroup(String groupName) throws SQLException {
        String groupId = UUID.randomUUID().toString();
        String sql = "INSERT INTO SpecialAccessGroups (groupId, groupName) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, groupName);
            pstmt.executeUpdate();
        }
    }

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

    public void addUserToGroup(String groupId, String username, String role) throws SQLException {
        String sqlInsertOrUpdate = """
            MERGE INTO GroupUsers (groupId, username, role, canView, canAdmin)
            VALUES (?, ?, ?, ?, ?)
        """;

        boolean isAdmin = false;
        if (role.equalsIgnoreCase("Administrator")) {
            isAdmin = true; // Admins get full access by default
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sqlInsertOrUpdate)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, username);
            pstmt.setString(3, role);
            pstmt.setBoolean(4, true); // Can view
            pstmt.setBoolean(5, isAdmin); // Admin rights only if role is Admin
            pstmt.executeUpdate();
        }
    }



    public void grantAdminRights(String groupId, String username) throws SQLException {
        updateUserPermissions(groupId, username, true, true);
    }

    public void grantViewRights(String groupId, String username) throws SQLException {
        updateUserPermissions(groupId, username, true, false);
    }

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
    
    public void addArticleToGroup(String groupId, int displayId, boolean encrypt) throws SQLException {
        // Get the actual database ID for the given display ID
        int articleId = getDatabaseIdForDisplayId(displayId);

        // Check if the article exists in the database
        String sqlCheckArticleExists = "SELECT COUNT(*) FROM Articles WHERE id = ?";
        String sqlInsert = "INSERT INTO GroupArticles (groupId, articleId) VALUES (?, ?)";
        String updateArticleSql = "UPDATE Articles SET isEncrypted = ? WHERE id = ?";

        try (PreparedStatement pstmtCheckArticle = connection.prepareStatement(sqlCheckArticleExists)) {
            pstmtCheckArticle.setInt(1, articleId);
            try (ResultSet rs = pstmtCheckArticle.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Article exists, proceed to insert and encrypt
                    try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsert);
                         PreparedStatement pstmtUpdate = connection.prepareStatement(updateArticleSql)) {

                        // Add the article to the group
                        pstmtInsert.setString(1, groupId);
                        pstmtInsert.setInt(2, articleId);
                        pstmtInsert.executeUpdate();

                        // Update encryption status for the article
                        pstmtUpdate.setBoolean(1, encrypt);
                        pstmtUpdate.setInt(2, articleId);
                        pstmtUpdate.executeUpdate();
                    }
                } else {
                    throw new SQLException("Article with ID " + displayId + " does not exist.");
                }
            }
        }
    }


    
    public List<Map<String, String>> getArticlesInGroup(String groupId, String username) throws SQLException {
        String sql = """
            SELECT a.id, a.title, a.body, a.isEncrypted, gu.canView
            FROM Articles a
            JOIN GroupArticles ga ON a.id = ga.articleId
            JOIN GroupUsers gu ON ga.groupId = gu.groupId
            WHERE ga.groupId = ? AND gu.username = ?
        """;

        List<Map<String, String>> articles = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance();

        try (PreparedStatement pstmt = dbHelper.getConnection().prepareStatement(sql)) {
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


}
