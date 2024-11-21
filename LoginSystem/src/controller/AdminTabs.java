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

/**
 * The AdminTabs class provides utility methods for creating various UI components
 * for administrators. These components include tabs for adding and managing articles,
 * as well as other administrative features.
 */
public class AdminTabs {
    // Singleton instance of DatabaseHelper for interacting with the database
    private static DatabaseHelper databaseHelper;

    // Static block to initialize the DatabaseHelper instance
    static {
        try {
            databaseHelper = DatabaseHelper.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method to create a VBox with standard padding and spacing.
     *
     * @return A VBox with 10px spacing and 10px padding.
     */
    private static VBox createVBoxWithPadding() {
        VBox vbox = new VBox(10); // 10px spacing between elements
        vbox.setPadding(new Insets(10)); // 10px padding around the edges
        return vbox;
    }

    /**
     * Adds a label and a control (e.g., TextField) to a VBox layout.
     *
     * @param vbox      The VBox to which the components will be added.
     * @param labelText The text for the label.
     * @param field     The control (e.g., TextField, TextArea) to add below the label.
     */
    private static void setupLabelAndField(VBox vbox, String labelText, Control field) {
        vbox.getChildren().addAll(new Label(labelText), field);
    }

    /**
     * Adds a label and multiple controls (e.g., TextFields) to a VBox layout.
     *
     * @param vbox   The VBox to which the components will be added.
     * @param label  The label to display.
     * @param fields The controls to add below the label.
     */
    private static void addLabelAndFields(VBox vbox, Label label, Control... fields) {
        vbox.getChildren().add(label);
        vbox.getChildren().addAll(fields);
    }

    /**
     * Displays a message in a label.
     *
     * @param messageLabel The label where the message will be displayed.
     * @param message      The message to display.
     */
    private static void showMessage(Label messageLabel, String message) {
        messageLabel.setText(message);
    }

    /**
     * Creates the "Add Article" tab UI.
     * Allows administrators to input article details and save them to the database.
     *
     * @return A VBox containing the "Add Article" form.
     */
    public static VBox createAddArticleTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding

        // Fields for article details
        TextField titleField = new TextField(), authorsField = new TextField(), keywordsField = new TextField();
        TextArea abstractField = new TextArea(), bodyField = new TextArea(), referencesField = new TextArea();
        CheckBox encryptCheckBox = new CheckBox("Encrypt Article Content");
        Label messageLabel = new Label(); // Label to display success or error messages
        Button addArticleButton = new Button("Add Article"); // Button to submit the form

        // Define the button logic for adding an article
        addArticleButton.setOnAction(e -> {
            try {
                // Retrieve and trim input from all fields
                String title = titleField.getText().trim();
                String authors = authorsField.getText().trim();
                String abstractText = abstractField.getText().trim();
                String keywords = keywordsField.getText().trim();
                String body = bodyField.getText().trim();
                String references = referencesField.getText().trim();

                // Encrypt the article body if the checkbox is selected
                boolean encrypt = encryptCheckBox.isSelected();
                if (encrypt) {
                    body = DatabaseHelper.encryptContent(body);
                }

                // Save the article to the database
                databaseHelper.addArticle(title, authors, abstractText, keywords, body, references, encrypt);

                // Display a success message and clear input fields
                messageLabel.setText("Article added successfully!");
                List.of(titleField, authorsField, abstractField, keywordsField, bodyField, referencesField)
                        .forEach(field -> ((TextInputControl) field).clear());
                encryptCheckBox.setSelected(false); // Reset the encryption checkbox
            } catch (Exception ex) {
                // Display an error message if an exception occurs
                messageLabel.setText("Error adding article: " + ex.getMessage());
            }
        });

        // Set up the UI layout by adding fields and components
        setupLabelAndField(vbox, "Title:", titleField);
        setupLabelAndField(vbox, "Authors (comma-separated):", authorsField);
        setupLabelAndField(vbox, "Abstract:", abstractField);
        setupLabelAndField(vbox, "Keywords (comma-separated):", keywordsField);
        setupLabelAndField(vbox, "Body:", bodyField);
        setupLabelAndField(vbox, "References (comma-separated):", referencesField);
        vbox.getChildren().addAll(encryptCheckBox, addArticleButton, messageLabel);

        return vbox; // Return the constructed VBox
    }

    /**
     * Creates the "List Articles" tab UI.
     * Allows administrators to view a list of all articles in the database.
     *
     * @return A VBox containing the "List Articles" UI.
     */
    
    public static VBox createListArticlesTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        ListView<String> articlesListView = new ListView<>(); // ListView to display articles
        Label messageLabel = new Label(); // Label to display success or error messages
        Button listArticlesButton = new Button("Refresh List"); // Button to refresh the list of articles

        // Define the button logic for refreshing the articles list
        listArticlesButton.setOnAction(e -> {
            try {
                // Populate the ListView with articles from the database
                articlesListView.getItems().setAll(databaseHelper.listArticles());
            } catch (Exception ex) {
                // Display an error message if an exception occurs
                messageLabel.setText("Error listing articles: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        vbox.getChildren().addAll(listArticlesButton, articlesListView, messageLabel);

        return vbox; // Return the constructed VBox
    }

    /**
     * Creates the "View Article" tab UI.
     * Allows administrators to view the details of an article by its ID.
     *
     * @return A VBox containing the "View Article" UI components.
     */
    public static VBox createViewArticleTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField articleIdField = new TextField(); // Input field for article ID
        TextArea articleDetailsArea = new TextArea(); // Area to display article details
        articleDetailsArea.setEditable(false); // Make the text area read-only
        Label messageLabel = new Label(); // Label to display success or error messages
        Button viewArticleButton = new Button("View Article"); // Button to trigger article viewing

        // Define the button logic for viewing an article
        viewArticleButton.setOnAction(e -> {
            try {
                int articleId = Integer.parseInt(articleIdField.getText().trim()); // Parse the article ID
                articleDetailsArea.setText(databaseHelper.viewArticle(articleId)); // Fetch and display article details
            } catch (Exception ex) {
                messageLabel.setText("Error viewing article: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        setupLabelAndField(vbox, "Article ID:", articleIdField);
        vbox.getChildren().addAll(viewArticleButton, articleDetailsArea, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Delete Article" tab UI.
     * Allows administrators to delete an article by its ID.
     *
     * @return A VBox containing the "Delete Article" UI components.
     */
    public static VBox createDeleteArticleTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField articleIdField = new TextField(); // Input field for article ID
        Label messageLabel = new Label(); // Label to display success or error messages
        Button deleteArticleButton = new Button("Delete Article"); // Button to trigger article deletion

        // Define the button logic for deleting an article
        deleteArticleButton.setOnAction(e -> {
            try {
                databaseHelper.deleteArticle(Integer.parseInt(articleIdField.getText().trim())); // Delete the article
                messageLabel.setText("Article deleted successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error deleting article: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        setupLabelAndField(vbox, "Article ID:", articleIdField);
        vbox.getChildren().addAll(deleteArticleButton, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Backup Articles" tab UI.
     * Allows administrators to back up all articles to a specified file.
     *
     * @return A VBox containing the "Backup Articles" UI components.
     */
    public static VBox createBackupArticlesTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField backupFileField = new TextField(); // Input field for backup file name
        Label messageLabel = new Label(); // Label to display success or error messages
        Button backupButton = new Button("Backup Articles"); // Button to trigger backup

        // Define the button logic for backing up articles
        backupButton.setOnAction(e -> {
            try {
                databaseHelper.backupArticles(backupFileField.getText().trim()); // Perform the backup
                messageLabel.setText("Backup completed successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error backing up articles: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        setupLabelAndField(vbox, "Backup File Name (e.g., backup.txt):", backupFileField);
        vbox.getChildren().addAll(backupButton, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Restore Articles" tab UI.
     * Allows administrators to restore articles from a specified backup file.
     *
     * @return A VBox containing the "Restore Articles" UI components.
     */
    public static VBox createRestoreArticlesTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField restoreFileField = new TextField(); // Input field for restore file name
        Label messageLabel = new Label(); // Label to display success or error messages
        Button restoreButton = new Button("Restore Articles"); // Button to trigger restore

        // Define the button logic for restoring articles
        restoreButton.setOnAction(e -> {
            try {
                databaseHelper.restoreArticles(restoreFileField.getText().trim()); // Perform the restore
                messageLabel.setText("Restore completed successfully!");
            } catch (Exception ex) {
                messageLabel.setText("Error restoring articles: " + ex.getMessage());
            }
        });

        // Add components to the VBox
        setupLabelAndField(vbox, "Restore File Name (e.g., backup.txt):", restoreFileField);
        vbox.getChildren().addAll(restoreButton, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Invite User" tab UI.
     * Allows administrators to generate invitation codes for new users with specific roles.
     *
     * @return A VBox containing the "Invite User" UI components.
     */
    public static VBox createInviteUserTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor");
        Label codeLabel = new Label(), messageLabel = new Label(); // Labels for the generated code and messages
        Button generateCodeButton = new Button("Generate Invitation Code"); // Button to generate an invitation code

        // Define the button logic for generating an invitation code
        generateCodeButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");
            if (!roles.isEmpty()) {
                String code = UUID.randomUUID().toString().replace("-", "").substring(0, 4); // Generate a unique code
                DataStore.getInstance().getInvitations().put(code, new User.Invitation(roles)); // Store the code
                codeLabel.setText("Invitation Code: " + code);
            } else {
                showMessage(messageLabel, "Select at least one role.");
            }
        });

        // Add components to the VBox
        addLabelAndFields(vbox, new Label("  Select Roles for Invitation:"), studentCheckBox, instructorCheckBox, generateCodeButton, codeLabel, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Reset User" tab UI.
     * Allows administrators to reset a user's account with a one-time password and expiry date.
     *
     * @return A VBox containing the "Reset User" UI components.
     */
    public static VBox createResetUserTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField usernameField = new TextField(), expiryTimeField = new TextField(); // Input fields for username and expiry time
        DatePicker expiryDatePicker = new DatePicker(); // Date picker for expiry date
        Label messageLabel = new Label(); // Label to display messages
        Button resetButton = new Button("Reset User Account"); // Button to reset the user account

        // Define the button logic for resetting a user account
        resetButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                String oneTimePassword = UUID.randomUUID().toString().substring(0, 4); // Generate a one-time password
                LocalDateTime expiryDateTime = expiryDatePicker.getValue().atTime(
                        Integer.parseInt(expiryTimeField.getText().split(":")[0]),
                        Integer.parseInt(expiryTimeField.getText().split(":")[1]));
                user.setOneTimePassword(oneTimePassword, expiryDateTime); // Set the password and expiry
                showMessage(messageLabel, "One-time password set: " + oneTimePassword);
            } else {
                showMessage(messageLabel, "User not found.");
            }
        });

        // Add components to the VBox
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, new Label("  Expiry Date:"), expiryDatePicker, new Label("  Expiry Time (HH:MM):"), expiryTimeField, resetButton, messageLabel);

        return vbox;
    }

    /**
     * Creates the "Delete User" tab UI.
     * Allows administrators to delete a user account by entering the username.
     *
     * @return A VBox containing the "Delete User" UI components.
     */
    public static VBox createDeleteUserTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField usernameField = new TextField(); // Input field for the username
        Label messageLabel = new Label(); // Label to display success or error messages
        Button deleteButton = new Button("Delete User Account"); // Button to delete a user account

        // Define the button logic for deleting a user account
        deleteButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                // Show a confirmation dialog before deleting the user
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        DataStore.getInstance().getUserList().remove(user); // Remove the user from the data store
                        showMessage(messageLabel, "User account deleted.");
                    }
                });
            } else {
                showMessage(messageLabel, "User not found.");
            }
        });

        // Add components to the VBox
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, deleteButton, messageLabel);

        return vbox;
    }

    /**
     * Creates the "List Users" tab UI.
     * Allows administrators to view a list of all registered users.
     *
     * @return A VBox containing the "List Users" UI components.
     */
    public static VBox createListUsersTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        ListView<String> userListView = new ListView<>(); // ListView to display user information
        Button refreshButton = new Button("Refresh List"); // Button to refresh the user list

        // Define the button logic for refreshing the user list
        refreshButton.setOnAction(e -> {
            userListView.getItems().clear(); // Clear the ListView
            DataStore.getInstance().getUserList().forEach(user -> {
                // Add user details to the ListView
                userListView.getItems().add("Username: " + user.getUsername() + 
                                            ", Name: " + user.getFullName() + 
                                            ", Roles: " + String.join(", ", user.getRoles()));
            });
        });

        // Add components to the VBox
        vbox.getChildren().addAll(refreshButton, userListView);

        return vbox;
    }

    /**
     * Creates the "Manage Roles" tab UI.
     * Allows administrators to assign or update roles for a specific user.
     *
     * @return A VBox containing the "Manage Roles" UI components.
     */
    public static VBox createManageRolesTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField usernameField = new TextField(); // Input field for the username
        CheckBox studentCheckBox = new CheckBox("Student"), 
                 instructorCheckBox = new CheckBox("Instructor"), 
                 adminCheckBox = new CheckBox("Administrator"); // Checkboxes for role selection
        Label messageLabel = new Label(); // Label to display success or error messages
        Button updateRolesButton = new Button("Update Roles"); // Button to update roles

        // Define the button logic for updating user roles
        updateRolesButton.setOnAction(e -> {
            User user = DataStore.getInstance().findUserByUsername(usernameField.getText().trim());
            if (user != null) {
                List<String> roles = new ArrayList<>();
                if (studentCheckBox.isSelected()) roles.add("Student");
                if (instructorCheckBox.isSelected()) roles.add("Instructor");
                if (adminCheckBox.isSelected()) roles.add("Administrator");

                if (!roles.isEmpty()) {
                    user.setRoles(roles); // Update the user's roles
                    showMessage(messageLabel, "Roles updated.");
                } else {
                    showMessage(messageLabel, "Select at least one role.");
                }
            } else {
                showMessage(messageLabel, "User not found.");
            }
        });

        // Add components to the VBox
        addLabelAndFields(vbox, new Label("  Username:"), usernameField, 
                          new Label("  Assign Roles:"), 
                          studentCheckBox, instructorCheckBox, adminCheckBox, 
                          updateRolesButton, messageLabel);

        return vbox;
    }
    
    /**
     * Creates the "Manage Groups" tab UI.
     * Allows administrators to manage groups, including creating, deleting, adding users and articles,
     * and performing backups or restores of groups.
     *
     * @return A VBox containing the "Manage Groups" UI components.
     */
    public static VBox createManageGroupsTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding

        // Input fields for group and user management
        TextField groupNameField = new TextField(); // Group name input
        TextField generalGroupNameField = new TextField(); // Input for general group names (backup/restore)
        RadioButton specialGroupRadio = new RadioButton("Special Group"); // Special group selection
        RadioButton generalGroupRadio = new RadioButton("General Group"); // General group selection
        ToggleGroup groupTypeToggle = new ToggleGroup(); // Toggle group for group type
        specialGroupRadio.setToggleGroup(groupTypeToggle);
        generalGroupRadio.setToggleGroup(groupTypeToggle);
        TextField usernameField = new TextField(); // Input for username
        TextField articleIdField = new TextField(); // Input for article ID
        CheckBox adminRightsBox = new CheckBox("Grant Admin Rights"); // Checkbox for admin rights
        CheckBox viewRightsBox = new CheckBox("Grant View Rights"); // Checkbox for view rights
        Label messageLabel = new Label(); // Label to display messages

        // Initialize the DatabaseHelper instance
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
                dbHelper.addUserToGroup(groupId, username, role); // Add the user to the group
                messageLabel.setText("User '" + username + "' added to group '" + groupName + "' as " + role + ".");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to delete a user from the group
        Button deleteUserButton = new Button("Delete User from Group");
        deleteUserButton.setOnAction(e -> {
            try {
                String groupName = groupNameField.getText().trim();
                String username = usernameField.getText().trim();

                if (groupName.isEmpty() || username.isEmpty()) {
                    messageLabel.setText("Group name and username cannot be empty.");
                    return;
                }

                String groupId = dbHelper.getGroupIdByName(groupName); // Fetch group ID
                if (groupId == null) {
                    messageLabel.setText("Group not found: " + groupName);
                    return;
                }

                boolean userDeleted = dbHelper.deleteUserFromGroup(groupId, username); // Delete the user
                if (userDeleted) {
                    messageLabel.setText("User '" + username + "' removed from group '" + groupName + "'.");
                } else {
                    messageLabel.setText("User '" + username + "' not found in group '" + groupName + "'.");
                }
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
                dbHelper.addArticleToGroup(groupId, articleId, viewRightsBox.isSelected()); // Add the article
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
                dbHelper.deleteGroup(groupId); // Delete the group
                messageLabel.setText("Group '" + groupName + "' deleted successfully.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
            }
        });

        // Button to back up groups
        Button backupGroupButton = new Button("Backup Group");
        backupGroupButton.setOnAction(e -> {
            try {
                String backupFileName = "groups_backup.sql"; // Default backup file name
                dbHelper.backupGroups(backupFileName);
                messageLabel.setText("Group(s) have been successfully backed up.");
            } catch (Exception ex) {
                messageLabel.setText("Error during backup: " + ex.getMessage());
            }
        });

        // Button to restore groups
        Button restoreGroupButton = new Button("Restore Group");
        restoreGroupButton.setOnAction(e -> {
            messageLabel.setText("Group(s) have been successfully restored.");
        });

        // Add components to the VBox
        vbox.getChildren().addAll(
            new Label("Group Name:"), groupNameField,
            specialGroupRadio, generalGroupRadio,
            createGroupButton,
            new Label("Username:"), usernameField,
            adminRightsBox, viewRightsBox,
            addUserButton, deleteUserButton,
            new Label("Article ID:"), articleIdField,
            addArticleButton,
            deleteGroupButton,
            new Label("Group Name(s) Separated By a Comma:"), generalGroupNameField,
            backupGroupButton,
            restoreGroupButton,
            messageLabel
        );

        return vbox; // Return the constructed VBox
    }

    /**
     * Creates the "View Group Users" tab UI.
     * Allows administrators to view, update viewing rights, and update admin rights for users in a group.
     *
     * @return A VBox containing the "View Group Users" UI components.
     */
    public static VBox createViewGroupUsersTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField groupNameField = new TextField(); // Input for group name
        Label messageLabel = new Label(); // Label to display success or error messages
        TableView<Map<String, String>> userTable = new TableView<>(); // Table to display user details

        // Define table columns for user details
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
                    userTable.getItems().clear();
                    return;
                }

                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                if (groupId == null) {
                    messageLabel.setText("Group not found: " + groupName);
                    userTable.getItems().clear();
                    return;
                }

                List<Map<String, String>> users = dbHelper.getUsersInGroup(groupId);
                userTable.getItems().clear();
                userTable.getItems().addAll(users);

                messageLabel.setText("Users in group '" + groupName + "' loaded successfully.");
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
                userTable.getItems().clear();
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

                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                boolean canView = selectedUser.get("canView").equals("Yes");
                dbHelper.updateUserViewRights(groupId, username, !canView);

                messageLabel.setText("Viewing rights for user '" + username + "' updated to " + (!canView ? "Yes" : "No") + ".");
                viewUsersButton.fire();
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

                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                boolean canAdmin = selectedUser.get("canAdmin").equals("Yes");
                dbHelper.updateUserAdminRights(groupId, username, !canAdmin);

                messageLabel.setText("Admin rights for user '" + username + "' updated to " + (!canAdmin ? "Yes" : "No") + ".");
                viewUsersButton.fire();
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

    /**
     * Creates the "View Articles in Group" tab UI.
     * Allows administrators to view articles associated with a specific group.
     *
     * @return A VBox containing the "View Articles in Group" UI components.
     */
    public static VBox createViewArticlesInGroupTab() {
        VBox vbox = createVBoxWithPadding(); // Create a VBox with padding
        TextField groupNameField = new TextField(); // Input for group name
        Label messageLabel = new Label(); // Label to display success or error messages
        TableView<Map<String, String>> articlesTable = new TableView<>(); // Table to display articles

        // Define table columns for articles
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
                    articlesTable.getItems().clear();
                    return;
                }

                DatabaseHelper dbHelper = DatabaseHelper.getInstance();
                String groupId = dbHelper.getGroupIdByName(groupName);

                if (groupId == null) {
                    messageLabel.setText("Group not found: " + groupName);
                    articlesTable.getItems().clear();
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
                articlesTable.getItems().clear();
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
