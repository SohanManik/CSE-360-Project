package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.*;

public class LoginSystemJavaFX extends Application {

    private static List<User> userList = new ArrayList<>();
    private static Map<String, Invitation> invitations = new HashMap<>();
    private Stage primaryStage;
    private TextField usernameField;
    private PasswordField passwordField, confirmPasswordField;
    private TextField invitationCodeField;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Login System");
        showLoginPage();
    }

    private void showLoginPage() {
        GridPane grid = createGridPane();

        usernameField = new TextField();
        passwordField = new PasswordField();
        confirmPasswordField = new PasswordField();
        invitationCodeField = new TextField();
        Label messageLabel = new Label();
        
        TextField invitationCodeField = new TextField();

        addGridComponents(grid, "Username:", usernameField, 0);
        addGridComponents(grid, "Password:", passwordField, 1);
        addGridComponents(grid, "Confirm Password:", confirmPasswordField, 2);
        addGridComponents(grid, "Invitation Code:", invitationCodeField, 3);

        Button loginButton = new Button("Login / Register");
        loginButton.setOnAction(e -> handleLoginOrRegister(messageLabel));
        grid.add(loginButton, 1, 4);
        grid.add(messageLabel, 1, 5);

        setScene(grid, 400, 300);
    }

    private void handleLoginOrRegister(Label messageLabel) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String invitationCode = invitationCodeField.getText().trim();

        if (!invitationCode.isEmpty()) {
            handleInvitationCode(invitationCode, messageLabel);
        } else if (userList.isEmpty() && validatePassword(password, confirmPassword, messageLabel)) {
            registerAdmin(username, password, messageLabel);
        } else {
            User user = findUserByUsername(username);
            if (user != null) {
                if (user.isPasswordResetRequired()) {
                    handlePasswordReset(user, password, messageLabel);
                } else if (user.getPassword().equals(password)) {
                    if (!user.isAccountSetupComplete()) {
                        showAccountSetupPage(user);
                    } else {
                        proceedAfterLogin(user);
                    }
                } else {
                    messageLabel.setText("Invalid username or password.");
                }
            } else if (validatePassword(password, confirmPassword, messageLabel)) {
                showRoleSelectionForRegistration(username, password);
            } else {
                messageLabel.setText("Invalid login or registration details.");
            }
        }
        clearFields();
    }

    private void handleInvitationCode(String code, Label messageLabel) {
        Invitation invitation = invitations.get(code);
        if (invitation != null) {
            showRegistrationPageWithRoles(invitation.getRoles(), code);
        } else {
            messageLabel.setText("Invalid invitation code.");
        }
    }

    private void handlePasswordReset(User user, String oneTimePassword, Label messageLabel) {
        if (oneTimePassword.equals(user.getOneTimePassword()) && LocalDateTime.now().isBefore(user.getPasswordExpiry())) {
            showPasswordResetPage(user);
        } else {
            messageLabel.setText("Invalid or expired one-time password.");
        }
    }

    private void showPasswordResetPage(User user) {
        VBox vbox = createVBox();
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmNewPasswordField = new PasswordField();
        Label messageLabel = new Label();

        vbox.getChildren().addAll(
                new Label("Enter New Password:"), newPasswordField,
                new Label("Confirm New Password:"), confirmNewPasswordField
        );

        Button resetButton = new Button("Reset Password");
        resetButton.setOnAction(e -> {
            String newPassword = newPasswordField.getText();
            String confirmNewPassword = confirmNewPasswordField.getText();
            if (newPassword.equals(confirmNewPassword) && !newPassword.isEmpty()) {
                user.setPassword(newPassword);
                user.clearPasswordReset();
                messageLabel.setText("Password reset successful. Please log in.");
                showLoginPage();
            } else {
                messageLabel.setText("Passwords do not match or are empty.");
            }
        });

        vbox.getChildren().addAll(resetButton, messageLabel);
        setScene(vbox, 300, 200);
    }

    private boolean validatePassword(String password, String confirmPassword, Label messageLabel) {
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Password fields cannot be empty.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return false;
        }
        return true;
    }

    private void registerAdmin(String username, String password, Label messageLabel) {
        User admin = new User(username, password);
        admin.addRole("Administrator");
        userList.add(admin);
        messageLabel.setText("Admin account created. Please log in again.");
    }

    private User findUserByUsername(String username) {
        return userList.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        invitationCodeField.clear();
    }

    private void showAccountSetupPage(User user) {
        GridPane grid = createGridPane();

        TextField emailField = new TextField();
        TextField firstNameField = new TextField();
        TextField middleNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField preferredFirstNameField = new TextField();
        Label setupMessageLabel = new Label();

        addGridComponents(grid, "Email:", emailField, 0);
        addGridComponents(grid, "First Name:", firstNameField, 1);
        addGridComponents(grid, "Middle Name:", middleNameField, 2);
        addGridComponents(grid, "Last Name:", lastNameField, 3);
        addGridComponents(grid, "Preferred First Name:", preferredFirstNameField, 4);

        Button finishSetupButton = new Button("Finish Setup");
        finishSetupButton.setOnAction(e -> {
            if (fieldsAreEmpty(emailField, firstNameField, lastNameField)) {
                setupMessageLabel.setText("Please fill in all required fields.");
            } else {
                user.setDetails(emailField.getText(), firstNameField.getText(), middleNameField.getText(),
                        lastNameField.getText(), preferredFirstNameField.getText());
                user.setAccountSetupComplete(true);
                proceedAfterLogin(user);
            }
        });
        grid.add(finishSetupButton, 1, 5);
        grid.add(setupMessageLabel, 1, 6);

        setScene(grid, 400, 350);
    }

    private void proceedAfterLogin(User user) {
        if (user.getRoles().size() > 1) {
            showRoleSelectionPage(user);
        } else {
            showHomePage(user, user.getRoles().get(0));
        }
    }

    private void showRoleSelectionPage(User user) {
        VBox vbox = createVBox();
        ToggleGroup roleGroup = new ToggleGroup();

        user.getRoles().forEach(role -> {
            RadioButton roleButton = new RadioButton(role);
            roleButton.setToggleGroup(roleGroup);
            vbox.getChildren().add(roleButton);
        });

        Button proceedButton = new Button("Proceed");
        proceedButton.setOnAction(e -> {
            RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
            if (selectedRole != null) showHomePage(user, selectedRole.getText());
        });

        vbox.getChildren().add(proceedButton);
        setScene(vbox, 300, 200);
    }

    private void showHomePage(User user, String role) {
        VBox vbox = createVBox();
        String displayName = user.getPreferredFirstNameOrDefault();
        vbox.getChildren().add(new Label("Welcome, " + role + " " + displayName + "!"));

        if ("Administrator".equals(role)) {
            // Administrator functionalities
            TabPane adminTabs = new TabPane();

            Tab inviteTab = new Tab("Invite User", createInviteUserTab());
            Tab resetTab = new Tab("Reset Account", createResetUserTab());
            Tab deleteTab = new Tab("Delete User", createDeleteUserTab());
            Tab listTab = new Tab("List Users", createListUsersTab());
            Tab roleTab = new Tab("Manage Roles", createManageRolesTab());

            adminTabs.getTabs().addAll(inviteTab, resetTab, deleteTab, listTab, roleTab);
            vbox.getChildren().add(adminTabs);
        }

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> showLoginPage());
        vbox.getChildren().add(logoutButton);

        setScene(vbox, 600, 500);
    }

    private Pane createInviteUserTab() {
        VBox vbox = createVBox();
        TextField emailField = new TextField();
        CheckBox studentCheckBox = new CheckBox("Student");
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        Label codeLabel = new Label();
        Label messageLabel = new Label();

        Button generateCodeButton = new Button("Generate Invitation Code");
        generateCodeButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");
            if (!roles.isEmpty()) {
                String code = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
                invitations.put(code, new Invitation(roles));
                codeLabel.setText("Invitation Code: " + code);
                messageLabel.setText("Invitation code generated.");
            } else {
                messageLabel.setText("Select at least one role.");
            }
        });

        vbox.getChildren().addAll(
                new Label("Select Roles for Invitation:"),
                studentCheckBox, instructorCheckBox,
                generateCodeButton, codeLabel, messageLabel
        );

        return vbox;
    }

    private Pane createResetUserTab() {
        VBox vbox = createVBox();
        TextField usernameField = new TextField();
        DatePicker expiryDatePicker = new DatePicker();
        TextField expiryTimeField = new TextField();
        Label messageLabel = new Label();

        Button resetButton = new Button("Reset User Account");
        resetButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            User user = findUserByUsername(username);
            if (user != null) {
                String oneTimePassword = UUID.randomUUID().toString().substring(0, 4);
                LocalDateTime expiryDateTime = expiryDatePicker.getValue().atTime(
                        Integer.parseInt(expiryTimeField.getText().split(":")[0]),
                        Integer.parseInt(expiryTimeField.getText().split(":")[1])
                );
                user.setOneTimePassword(oneTimePassword, expiryDateTime);
                messageLabel.setText("One-time password set: " + oneTimePassword);
            } else {
                messageLabel.setText("User not found.");
            }
        });

        vbox.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Expiry Date:"), expiryDatePicker,
                new Label("Expiry Time (HH:MM):"), expiryTimeField,
                resetButton, messageLabel
        );

        return vbox;
    }

    private Pane createDeleteUserTab() {
        VBox vbox = createVBox();
        TextField usernameField = new TextField();
        Label messageLabel = new Label();

        Button deleteButton = new Button("Delete User Account");
        deleteButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            User user = findUserByUsername(username);
            if (user != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        userList.remove(user);
                        messageLabel.setText("User account deleted.");
                    }
                });
            } else {
                messageLabel.setText("User not found.");
            }
        });

        vbox.getChildren().addAll(
                new Label("Username:"), usernameField,
                deleteButton, messageLabel
        );

        return vbox;
    }

    private Pane createListUsersTab() {
        VBox vbox = createVBox();
        ListView<String> userListView = new ListView<>();
        Button refreshButton = new Button("Refresh List");

        refreshButton.setOnAction(e -> {
            userListView.getItems().clear();
            for (User user : userList) {
                String roles = String.join(", ", user.getRoles());
                String displayName = user.getFullName();
                userListView.getItems().add("Username: " + user.getUsername() +
                        ", Name: " + displayName +
                        ", Roles: " + roles);
            }
        });

        vbox.getChildren().addAll(refreshButton, userListView);
        return vbox;
    }

    private Pane createManageRolesTab() {
        VBox vbox = createVBox();
        TextField usernameField = new TextField();
        CheckBox studentCheckBox = new CheckBox("Student");
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        CheckBox adminCheckBox = new CheckBox("Administrator");
        Label messageLabel = new Label();

        Button updateRolesButton = new Button("Update Roles");
        updateRolesButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            User user = findUserByUsername(username);
            if (user != null) {
                List<String> roles = new ArrayList<>();
                if (studentCheckBox.isSelected()) roles.add("Student");
                if (instructorCheckBox.isSelected()) roles.add("Instructor");
                if (adminCheckBox.isSelected()) roles.add("Administrator");

                if (!roles.isEmpty()) {
                    user.setRoles(roles);
                    messageLabel.setText("Roles updated.");
                } else {
                    messageLabel.setText("Select at least one role.");
                }
            } else {
                messageLabel.setText("User not found.");
            }
        });

        vbox.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Assign Roles:"),
                studentCheckBox, instructorCheckBox, adminCheckBox,
                updateRolesButton, messageLabel
        );

        return vbox;
    }

    private void showRegistrationPageWithRoles(List<String> roles, String invitationCode) {
        VBox vbox = createVBox();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        Label messageLabel = new Label();

        vbox.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmPasswordField
        );

        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (validatePassword(password, confirmPassword, messageLabel)) {
                User newUser = new User(username, password, roles);
                userList.add(newUser);
                invitations.remove(invitationCode);
                messageLabel.setText("Registration successful. Please log in.");
                showLoginPage();
            }
        });

        vbox.getChildren().addAll(registerButton, messageLabel);
        setScene(vbox, 300, 250);
    }

    private void showRoleSelectionForRegistration(String username, String password) {
        VBox vbox = createVBox();
        CheckBox studentCheckBox = new CheckBox("Student");
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        Label messageLabel = new Label();

        vbox.getChildren().addAll(new Label("Select your roles:"), studentCheckBox, instructorCheckBox);

        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");

            if (!roles.isEmpty()) {
                User newUser = new User(username, password, roles);
                userList.add(newUser);
                messageLabel.setText("Registration successful. Please log in.");
                showLoginPage();
            } else {
                messageLabel.setText("Please select at least one role.");
            }
        });

        vbox.getChildren().addAll(registerButton, messageLabel);
        setScene(vbox, 300, 200);
    }

    private void setScene(javafx.scene.Parent root, int width, int height) {
        primaryStage.setScene(new Scene(root, width, height));
        primaryStage.show();
    }

    private GridPane createGridPane() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);
        return grid;
    }

    private VBox createVBox() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private void addGridComponents(GridPane grid, String labelText, TextField field, int row) {
        grid.add(new Label(labelText), 0, row);
        grid.add(field, 1, row);
    }

    private boolean fieldsAreEmpty(TextField... fields) {
        for (TextField field : fields) {
            if (field.getText().trim().isEmpty()) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }

    // User class with added functionalities
    public static class User {
        private String username, password, email, firstName, middleName, lastName, preferredFirstName;
        private List<String> roles = new ArrayList<>();
        private boolean accountSetupComplete = false;
        private String oneTimePassword;
        private LocalDateTime passwordExpiry;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public User(String username, String password, List<String> roles) {
            this(username, password);
            this.roles.addAll(roles);
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public List<String> getRoles() { return roles; }
        public boolean isAccountSetupComplete() { return accountSetupComplete; }
        public void setPassword(String password) { this.password = password; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public void setDetails(String email, String firstName, String middleName, String lastName, String preferredFirstName) {
            this.email = email;
            this.firstName = firstName;
            this.middleName = middleName;
            this.lastName = lastName;
            this.preferredFirstName = preferredFirstName;
        }

        public void setAccountSetupComplete(boolean complete) { this.accountSetupComplete = complete; }

        public String getPreferredFirstNameOrDefault() {
            return (preferredFirstName == null || preferredFirstName.isEmpty()) ? firstName : preferredFirstName;
        }

        public void addRole(String role) {
            if (!roles.contains(role)) roles.add(role);
        }

        public String getOneTimePassword() { return oneTimePassword; }
        public LocalDateTime getPasswordExpiry() { return passwordExpiry; }

        public void setOneTimePassword(String oneTimePassword, LocalDateTime expiry) {
            this.oneTimePassword = oneTimePassword;
            this.passwordExpiry = expiry;
        }

        public boolean isPasswordResetRequired() {
            return oneTimePassword != null && LocalDateTime.now().isBefore(passwordExpiry);
        }

        public void clearPasswordReset() {
            this.oneTimePassword = null;
            this.passwordExpiry = null;
        }

        public String getFullName() {
            return String.format("%s %s %s", firstName, middleName != null ? middleName : "", lastName).trim();
        }
    }

    // Invitation class to store invitation details
    public static class Invitation {
        private List<String> roles;

        public Invitation(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
