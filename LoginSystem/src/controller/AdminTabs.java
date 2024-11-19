package controller;

import model.DatabaseHelper;
import model.DataStore;
import model.User;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.sql.*; 
import javafx.beans.property.SimpleStringProperty;

public class AdminTabs {
    private static DatabaseHelper databaseHelper;

    static {
        try { databaseHelper = DatabaseHelper.getInstance();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static VBox createVBoxWithPadding() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private static void setupLabelAndField(VBox vbox, String labelText, Control field) {
        vbox.getChildren().addAll(new Label(labelText), field);
    }

    private static void addLabelAndFields(VBox vbox, Label label, Control... fields) {
        vbox.getChildren().add(label);
        vbox.getChildren().addAll(fields);
    }

    private static void showMessage(Label messageLabel, String message) {
        messageLabel.setText(message);
    }

    public static VBox createAddArticleTab() {
        VBox vbox = createVBoxWithPadding();

        // Fields for article details
        TextField titleField = new TextField(), authorsField = new TextField(), keywordsField = new TextField();
        TextArea abstractField = new TextArea(), bodyField = new TextArea(), referencesField = new TextArea();
        CheckBox encryptCheckBox = new CheckBox("Encrypt Article Content");
        Label messageLabel = new Label();
        Button addArticleButton = new Button("Add Article");

        // Button Logic
        addArticleButton.setOnAction(e -> {
            try {
                String title = titleField.getText().trim();
                String authors = authorsField.getText().trim();
                String abstractText = abstractField.getText().trim();
                String keywords = keywordsField.getText().trim();
                String body = bodyField.getText().trim();
                String references = referencesField.getText().trim();

                // Encrypt content if checkbox is selected
                boolean encrypt = encryptCheckBox.isSelected();
                if (encrypt) {
                    body = DatabaseHelper.encryptContent(body);
                }

                databaseHelper.addArticle(title, authors, abstractText, keywords, body, references, encrypt);
                messageLabel.setText("Article added successfully!");
                
                // Clear fields after successful addition
                List.of(titleField, authorsField, abstractField, keywordsField, bodyField, referencesField)
                        .forEach(field -> ((TextInputControl) field).clear());
                encryptCheckBox.setSelected(false); // Reset encryption checkbox
            } catch (Exception ex) {
                messageLabel.setText("Error adding article: " + ex.getMessage());
            }
        });

        // Setup UI components
        setupLabelAndField(vbox, "Title:", titleField);
        setupLabelAndField(vbox, "Authors (comma-separated):", authorsField);
        setupLabelAndField(vbox, "Abstract:", abstractField);
        setupLabelAndField(vbox, "Keywords (comma-separated):", keywordsField);
        setupLabelAndField(vbox, "Body:", bodyField);
        setupLabelAndField(vbox, "References (comma-separated):", referencesField);
        vbox.getChildren().addAll(encryptCheckBox, addArticleButton, messageLabel);

        return vbox;
    }


    public static VBox createListArticlesTab() {
        VBox vbox = createVBoxWithPadding();
        ListView<String> articlesListView = new ListView<>();
        Label messageLabel = new Label();
        Button listArticlesButton = new Button("Refresh List");
        listArticlesButton.setOnAction(e -> {
            try {
                articlesListView.getItems().setAll(databaseHelper.listArticles());
            } catch (Exception ex) {
                messageLabel.setText("Error listing articles: " + ex.getMessage());
            }
        });
        vbox.getChildren().addAll(listArticlesButton, articlesListView, messageLabel);
        return vbox;
    }

    public static VBox createViewArticleTab() {
        VBox vbox = createVBoxWithPadding();
        TextField articleIdField = new TextField();
        TextArea articleDetailsArea = new TextArea();
        articleDetailsArea.setEditable(false);
        Label messageLabel = new Label();
        Button viewArticleButton = new Button("View Article");
        viewArticleButton.setOnAction(e -> {
            try {
                int articleId = Integer.parseInt(articleIdField.getText().trim());
                articleDetailsArea.setText(databaseHelper.viewArticle(articleId));
            } catch (Exception ex) {
                messageLabel.setText("Error viewing article: " + ex.getMessage());
            }
        });
        setupLabelAndField(vbox, "Article ID:", articleIdField);
        vbox.getChildren().addAll(viewArticleButton, articleDetailsArea, messageLabel);
        return vbox;
    }

    
    public static VBox createDeleteArticleTab() {
        VBox vbox = createVBoxWithPadding();
        TextField articleIdField = new TextField();
        Label messageLabel = new Label();
        Button deleteArticleButton = new Button("Delete Article");
        deleteArticleButton.setOnAction(e -> {
            try {
                databaseHelper.deleteArticle(Integer.parseInt(articleIdField.getText().trim()));
                messageLabel.setText("Article deleted successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error deleting article: " + ex.getMessage());
            }
        });
        setupLabelAndField(vbox, "Article ID:", articleIdField);
        vbox.getChildren().addAll(deleteArticleButton, messageLabel);
        return vbox;
    }

    public static VBox createBackupArticlesTab() {
        VBox vbox = createVBoxWithPadding();
        TextField backupFileField = new TextField();
        Label messageLabel = new Label();
        Button backupButton = new Button("Backup Articles");
        backupButton.setOnAction(e -> {
            try {
                databaseHelper.backupArticles(backupFileField.getText().trim());
                messageLabel.setText("Backup completed successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error backing up articles: " + ex.getMessage());
            }
        });
        setupLabelAndField(vbox, "Backup File Name (e.g., backup.txt):", backupFileField);
        vbox.getChildren().addAll(backupButton, messageLabel);
        return vbox;
    }

    public static VBox createRestoreArticlesTab() {
        VBox vbox = createVBoxWithPadding();
        TextField restoreFileField = new TextField();
        Label messageLabel = new Label();
        Button restoreButton = new Button("Restore Articles");
        restoreButton.setOnAction(e -> {
            try {
                databaseHelper.restoreArticles(restoreFileField.getText().trim());
                messageLabel.setText("Restore completed successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error restoring articles: " + ex.getMessage());
            }
        });
        setupLabelAndField(vbox, "Restore File Name (e.g., backup.txt):", restoreFileField);
        vbox.getChildren().addAll(restoreButton, messageLabel);
        return vbox;
    }

    public static VBox createInviteUserTab() {
        VBox vbox = createVBoxWithPadding();
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor");
        Label codeLabel = new Label(), messageLabel = new Label();
        Button generateCodeButton = new Button("Generate Invitation Code");
        generateCodeButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");
            if (!roles.isEmpty()) {
                String code = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
                DataStore.getInstance().getInvitations().put(code, new User.Invitation(roles));
                codeLabel.setText("Invitation Code: " + code);
            } else showMessage(messageLabel, "Select at least one role.");
        });
        addLabelAndFields(vbox, new Label("  Select Roles for Invitation:"), studentCheckBox, instructorCheckBox, generateCodeButton, codeLabel, messageLabel);
        return vbox;
    }

    public static VBox createResetUserTab() {
        VBox vbox = createVBoxWithPadding();
        TextField usernameField = new TextField(), expiryTimeField = new TextField();
        DatePicker expiryDatePicker = new DatePicker();
        Label messageLabel = new Label();
        Button resetButton = new Button("Reset User Account");
        resetButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                String oneTimePassword = UUID.randomUUID().toString().substring(0, 4);
                LocalDateTime expiryDateTime = expiryDatePicker.getValue().atTime(
                        Integer.parseInt(expiryTimeField.getText().split(":")[0]),
                        Integer.parseInt(expiryTimeField.getText().split(":")[1]));
                user.setOneTimePassword(oneTimePassword, expiryDateTime);
                showMessage(messageLabel, "One-time password set: " + oneTimePassword);
            } else showMessage(messageLabel, "User not found.");
        });
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, new Label("  Expiry Date:"), expiryDatePicker, new Label("  Expiry Time (HH:MM):"), expiryTimeField, resetButton, messageLabel);
        return vbox;
    }

    public static VBox createDeleteUserTab() {
        VBox vbox = createVBoxWithPadding();
        TextField usernameField = new TextField();
        Label messageLabel = new Label();
        Button deleteButton = new Button("Delete User Account");
        deleteButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        DataStore.getInstance().getUserList().remove(user);
                        showMessage(messageLabel, "User account deleted.");
                    }
                });
            } else showMessage(messageLabel, "User not found.");
        });
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, deleteButton, messageLabel);
        return vbox;
    }

    public static VBox createListUsersTab() {
        VBox vbox = createVBoxWithPadding();
        ListView<String> userListView = new ListView<>();
        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> {
            userListView.getItems().clear();
            DataStore.getInstance().getUserList().forEach(user -> userListView.getItems().add(
                    "Username: " + user.getUsername() + ", Name: " + user.getFullName() + ", Roles: " + String.join(", ", user.getRoles())));
        });
        vbox.getChildren().addAll(refreshButton, userListView);
        return vbox;
    }

    public static VBox createManageRolesTab() {
        VBox vbox = createVBoxWithPadding();
        TextField usernameField = new TextField();
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor"), adminCheckBox = new CheckBox("Administrator");
        Label messageLabel = new Label();
        Button updateRolesButton = new Button("Update Roles");
        updateRolesButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                List<String> roles = new ArrayList<>();
                if (studentCheckBox.isSelected()) roles.add("Student");
                if (instructorCheckBox.isSelected()) roles.add("Instructor");
                if (adminCheckBox.isSelected()) roles.add("Administrator");
                if (!roles.isEmpty()) {
                    user.setRoles(roles);
                    showMessage(messageLabel, "Roles updated.");
                } else showMessage(messageLabel, "Select at least one role.");
            } else showMessage(messageLabel, "User not found.");
        });
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, new Label("  Assign Roles:"), studentCheckBox, instructorCheckBox, adminCheckBox, updateRolesButton, messageLabel);
        return vbox;
    }
    
    public static VBox createManageGroupsTab() {
        VBox vbox = createVBoxWithPadding();
        TextField groupNameField = new TextField();
        RadioButton specialGroupRadio = new RadioButton("Special Group");
        RadioButton generalGroupRadio = new RadioButton("General Group");
        ToggleGroup groupTypeToggle = new ToggleGroup();
        specialGroupRadio.setToggleGroup(groupTypeToggle);
        generalGroupRadio.setToggleGroup(groupTypeToggle);
        TextField usernameField = new TextField();
        TextField articleIdField = new TextField();
        CheckBox adminRightsBox = new CheckBox("Grant Admin Rights");
        CheckBox viewRightsBox = new CheckBox("Grant View Rights");
        Label messageLabel = new Label();

        DatabaseHelper dbHelper;
        try {
            dbHelper = DatabaseHelper.getInstance();
        } catch (SQLException e) {
            messageLabel.setText("Database initialization error: " + e.getMessage());
            vbox.getChildren().add(messageLabel);
            return vbox;
        }

        // Button to create a new group
        Button createGroupButton = new Button("Create Group");
        createGroupButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    return;
                }

                // Validate group type selection
                if (groupTypeToggle.getSelectedToggle() == null) {
                    messageLabel.setText("Please select the type of group (Special or General).");
                    return;
                }

                boolean isSpecialGroup = specialGroupRadio.isSelected();
                dbHelper.createGroup(groupName, isSpecialGroup); // Create the group
                String groupType = isSpecialGroup ? "Special" : "General";
                messageLabel.setText("Group '" + groupName + "' created successfully as a " + groupType + " group.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to add a user to the group
        Button addUserButton = new Button("Add User to Group");
        addUserButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                String username = usernameField.getText().trim();
                if (groupName.isEmpty() || username.isEmpty()) {
                    messageLabel.setText("Group name and username cannot be empty.");
                    return;
                }

                // Determine the role based on the state of adminRightsBox
                String role = adminRightsBox.isSelected() ? "Instructor" : "Viewer";

                String groupId = dbHelper.getGroupIdByName(groupName);
                dbHelper.addUserToGroup(groupId, username, role); // Pass the role as a String
                messageLabel.setText("User '" + username + "' added to group '" + groupName + "' as " + role + ".");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to add an article to the group
        Button addArticleButton = new Button("Add Article to Group");
        addArticleButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                int articleId = Integer.parseInt(articleIdField.getText().trim());
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    return;
                }

                String groupId = dbHelper.getGroupIdByName(groupName);
                dbHelper.addArticleToGroup(groupId, articleId, viewRightsBox.isSelected());
                messageLabel.setText("Article '" + articleId + "' added to group '" + groupName + "'.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to delete a group
        Button deleteGroupButton = new Button("Delete Group");
        deleteGroupButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    return;
                }

                String groupId = dbHelper.getGroupIdByName(groupName);
                dbHelper.deleteGroup(groupId);
                messageLabel.setText("Group '" + groupName + "' deleted successfully.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        vbox.getChildren().addAll(
            new Label("Group Name:"), groupNameField,
            specialGroupRadio, generalGroupRadio,
            createGroupButton,
            new Label("Username:"), usernameField,
            adminRightsBox, viewRightsBox,
            addUserButton,
            new Label("Article ID:"), articleIdField,
            addArticleButton,
            deleteGroupButton,
            messageLabel
        );
        return vbox;
    }



    
    public static VBox createViewGroupUsersTab() {
        VBox vbox = createVBoxWithPadding();
        TextField groupNameField = new TextField();
        Label messageLabel = new Label();
        TableView<Map<String, String>> userTable = new TableView<>();

        // Define table columns
        TableColumn<Map<String, String>, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("username")));

        TableColumn<Map<String, String>, String> roleColumn = new TableColumn<>("Role");
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("role")));

        TableColumn<Map<String, String>, String> canViewColumn = new TableColumn<>("Can View");
        canViewColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("canView")));

        TableColumn<Map<String, String>, String> canAdminColumn = new TableColumn<>("Can Admin");
        canAdminColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("canAdmin")));

        userTable.getColumns().addAll(usernameColumn, roleColumn, canViewColumn, canAdminColumn);

        // Button to fetch users in the group
        Button viewUsersButton = new Button("View Group Users");
        viewUsersButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    userTable.getItems().clear(); // Clear table if input is invalid
                    return;
                }

                // Fetch group ID and users
                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                if (groupId == null) {
                    messageLabel.setText("Group not found: " + groupName);
                    userTable.getItems().clear(); // Clear table for non-existent group
                    return;
                }

                List<Map<String, String>> users = dbHelper.getUsersInGroup(groupId);
                userTable.getItems().clear();
                userTable.getItems().addAll(users);

                messageLabel.setText("Users in group '" + groupName + "' loaded successfully.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
                userTable.getItems().clear(); // Clear table on error
            }
        });


        // Button to update viewing rights
        Button updateViewRightsButton = new Button("Update Viewing Rights");
        updateViewRightsButton.setOnAction(e -> {
            try {
                Map<String, String> selectedUser = userTable.getSelectionModel().getSelectedItem();
                if (selectedUser == null) {
                    messageLabel.setText("Select a user to update viewing rights.");
                    return;
                }

                String username = selectedUser.get("username");
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    return;
                }

                // Fetch group ID
                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                // Toggle viewing rights
                boolean canView = selectedUser.get("canView").equals("Yes");
                dbHelper.updateUserViewRights(groupId, username, !canView);

                messageLabel.setText("Viewing rights for user '" + username + "' updated to " + (!canView ? "Yes" : "No") + ".");
                viewUsersButton.fire(); // Refresh user list
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to update admin rights
        Button updateAdminRightsButton = new Button("Update Admin Rights");
        updateAdminRightsButton.setOnAction(e -> {
            try {
                Map<String, String> selectedUser = userTable.getSelectionModel().getSelectedItem();
                if (selectedUser == null) {
                    messageLabel.setText("Select a user to update admin rights.");
                    return;
                }

                String username = selectedUser.get("username");
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    return;
                }

                // Fetch group ID
                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                // Toggle admin rights
                boolean canAdmin = selectedUser.get("canAdmin").equals("Yes");
                dbHelper.updateUserAdminRights(groupId, username, !canAdmin);

                messageLabel.setText("Admin rights for user '" + username + "' updated to " + (!canAdmin ? "Yes" : "No") + ".");
                viewUsersButton.fire(); // Refresh user list
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Add components to VBox
        vbox.getChildren().addAll(
            new Label("Group Name:"), groupNameField,
            viewUsersButton, userTable,
            updateViewRightsButton, updateAdminRightsButton,
            messageLabel
        );
        return vbox;
    }


    
    public static VBox createViewArticlesInGroupTab() {
        VBox vbox = createVBoxWithPadding();
        TextField groupNameField = new TextField(); // Input for group name
        Label messageLabel = new Label();
        TableView<Map<String, String>> articlesTable = new TableView<>();

        // Define table columns
        TableColumn<Map<String, String>, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("id")));

        TableColumn<Map<String, String>, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("title")));

        TableColumn<Map<String, String>, String> bodyColumn = new TableColumn<>("Body");
        bodyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("body")));

        articlesTable.getColumns().addAll(idColumn, titleColumn, bodyColumn);

        // Button to fetch articles
        Button viewArticlesButton = new Button("View Articles");
        viewArticlesButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                if (groupName.isEmpty()) {
                    messageLabel.setText("Group name cannot be empty.");
                    articlesTable.getItems().clear(); // Clear table for invalid input
                    return;
                }

                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                if (groupId == null) {
                    messageLabel.setText("Group not found: " + groupName);
                    articlesTable.getItems().clear(); // Clear table for non-existent group
                    return;
                }

                String username = "yourLoggedInUsername"; // Replace with actual logged-in username
                List<Map<String, String>> articles = dbHelper.getArticlesInGroup(groupId, username);

                if (articles.isEmpty()) {
                    messageLabel.setText("No articles found in group '" + groupName + "'.");
                } else {
                    messageLabel.setText("Articles in group '" + groupName + "' loaded successfully.");
                }

                articlesTable.getItems().clear();
                articlesTable.getItems().addAll(articles);
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
                articlesTable.getItems().clear(); // Clear table on error
            }
        });



        // Add UI components to the layout
        vbox.getChildren().addAll(
            new Label("Group Name:"), groupNameField,
            viewArticlesButton, articlesTable, messageLabel
        );
        return vbox;
    }

}
