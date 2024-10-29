package controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import model.DataStore;
import model.Invitation;
import model.User;

public class LoginController {

    private Stage primaryStage;
    private TextField usernameField = new TextField(), invitationCodeField = new TextField();
    private PasswordField passwordField = new PasswordField(), confirmPasswordField = new PasswordField();
    private Label messageLabel = new Label();

    public LoginController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void showLoginPage() {
        GridPane grid = new GridPane();
        grid.setVgap(8); grid.setHgap(10);
        grid.setPadding(new Insets(10, 0, 0, 10));
        
        messageLabel.setPrefWidth(180); messageLabel.setMaxWidth(180);
        messageLabel.setWrapText(true);
        
        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Password:"), passwordField);
        grid.addRow(2, new Label("Confirm Password:"), confirmPasswordField);
        grid.addRow(3, new Label("Invitation Code:"), invitationCodeField);
        
        Button loginButton = new Button("Login / Register");
        loginButton.setOnAction(e -> handleLoginOrRegister());
        grid.addRow(4, loginButton, messageLabel);
        primaryStage.setScene(new Scene(grid, 400, 400));
        primaryStage.show();
    }

    private void handleLoginOrRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String invitationCode = invitationCodeField.getText().trim();

        DataStore dataStore = DataStore.getInstance();

        if (!invitationCode.isEmpty()) {
            handleInvitationCode(invitationCode);
        } else if (dataStore.getUserList().isEmpty() && validatePassword(password, confirmPassword)) {
            registerAdmin(username, password);
        } else {
            User user = dataStore.findUserByUsername(username);
            if (user != null) {
                if (user.isPasswordResetRequired()) {
                    handlePasswordReset(user, password);
                } else if (user.getPassword().equals(password)) {
                    if (!user.isAccountSetupComplete()) {
                        AccountSetupController accountSetupController = new AccountSetupController(user);
                        accountSetupController.showAccountSetupPage();
                    } else {
                        proceedAfterLogin(user);
                    }
                } else {
                    messageLabel.setText("Invalid username or password.");
                }
            } else if (validatePassword(password, confirmPassword)) {
                showRoleSelectionForRegistration(username, password);
            } else {
                messageLabel.setText("Invalid login or registration details.");
            }
        }
        clearFields();
    }

    private void handleInvitationCode(String code) {
        DataStore dataStore = DataStore.getInstance();
        Invitation invitation = dataStore.getInvitations().get(code);
        if (invitation != null) {
            RegistrationController registrationController = new RegistrationController(primaryStage, invitation.getRoles(), code);
            registrationController.showRegistrationPageWithRoles();
        } else {
            messageLabel.setText("Invalid invitation code.");
        }
    }

    private void handlePasswordReset(User user, String oneTimePassword) {
        if (oneTimePassword.equals(user.getOneTimePassword()) && java.time.LocalDateTime.now().isBefore(user.getPasswordExpiry())) {
            PasswordResetController passwordResetController = new PasswordResetController(primaryStage, user);
            passwordResetController.showPasswordResetPage();
        } else {
            messageLabel.setText("Invalid or expired one-time password.");
        }
    }

    private boolean validatePassword(String password, String confirmPassword) {
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

    private void registerAdmin(String username, String password) {
        DataStore dataStore = DataStore.getInstance();
        User admin = new User(username, password);
        admin.addRole("Administrator");
        dataStore.getUserList().add(admin);
        messageLabel.setText("Admin account created. Please log in again.");
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        invitationCodeField.clear();
    }

    void proceedAfterLogin(User user) {
        if (user.getRoles().size() > 1) {
            RoleSelectionController roleSelectionController = new RoleSelectionController(primaryStage, user);
            roleSelectionController.showRoleSelectionPage();
        } else {
            HomeController homeController = new HomeController(primaryStage, user, user.getRoles().get(0));
            homeController.showHomePage();
        }
    }

    private void showRoleSelectionForRegistration(String username, String password) {
        RegistrationController registrationController = new RegistrationController(primaryStage, username, password);
        registrationController.showRoleSelectionForRegistration();
    }

    // Inner class for Account Setup
    private class AccountSetupController {

        private User user;

        public AccountSetupController(User user) {
            this.user = user;
        }

        public void showAccountSetupPage() {
            GridPane grid = new GridPane();
            grid.setVgap(8); grid.setHgap(10);
            grid.setPadding(new Insets(10, 0, 0, 10));

            TextField emailField = new TextField(), firstNameField = new TextField(),
                    middleNameField = new TextField(), lastNameField = new TextField(),
                    preferredFirstNameField = new TextField();
            Label setupMessageLabel = new Label();
            Button finishSetupButton = new Button("Finish Setup");

            finishSetupButton.setOnAction(e -> {
                if (emailField.getText().trim().isEmpty() || firstNameField.getText().trim().isEmpty()
                        || lastNameField.getText().trim().isEmpty()) {
                    setupMessageLabel.setText("Please fill in all required fields.");
                } else {
                    user.setDetails(emailField.getText(), firstNameField.getText(), middleNameField.getText(),
                            lastNameField.getText(), preferredFirstNameField.getText());
                    user.setAccountSetupComplete(true);
                    proceedAfterLogin(user);
                }
            });

            grid.addRow(0, new Label("Email:"), emailField);
            grid.addRow(1, new Label("First Name:"), firstNameField);
            grid.addRow(2, new Label("Middle Name:"), middleNameField);
            grid.addRow(3, new Label("Last Name:"), lastNameField);
            grid.addRow(4, new Label("Preferred First Name:"), preferredFirstNameField);
            grid.addRow(5, finishSetupButton, setupMessageLabel);

            primaryStage.setScene(new Scene(grid, 400, 400));
        }
    }
}
