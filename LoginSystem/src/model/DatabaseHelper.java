package model;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DatabaseHelper {
    private static DatabaseHelper instance;
    private Connection connection;

    private DatabaseHelper() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        setupDatabase();
    }

    public static DatabaseHelper getInstance() throws SQLException {
        if (instance == null) instance = new DatabaseHelper();
        return instance;
    }

    private void setupDatabase() throws SQLException {
        String createArticlesTable = """
            CREATE TABLE IF NOT EXISTS Articles (
                id INT PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(255),
                authors VARCHAR(255),
                abstractText TEXT,
                keywords VARCHAR(255),
                body TEXT,
                references TEXT
            );
        """;
        
        String addIsEncryptedColumnSQL = """
                ALTER TABLE Articles ADD COLUMN IF NOT EXISTS isEncrypted BOOLEAN DEFAULT FALSE;
            """;
        
        String createAccessRightsTable = """
            CREATE TABLE IF NOT EXISTS AccessRights (
                groupId VARCHAR(255),
                username VARCHAR(255),
                canView BOOLEAN DEFAULT FALSE,
                canAdmin BOOLEAN DEFAULT FALSE,
                PRIMARY KEY (groupId, username)
            );
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createArticlesTable);
            stmt.execute(createAccessRightsTable);
            stmt.execute(addIsEncryptedColumnSQL);
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
        // Simple example using Base64 (replace with a stronger algorithm for real-world use)
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


}
