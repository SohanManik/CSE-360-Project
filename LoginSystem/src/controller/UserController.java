package controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;

public class UserController {

    // References to the primary stage and the user whose role is being selected
    private Stage primaryStage;
    private User user;

    // Constructor initializing the stage and user
    public UserController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
    }

    // Method to display the role selection page, allowing the user to choose a role to proceed with
    public void showRoleSelectionPage() {
        VBox vbox = new VBox(10);
        ToggleGroup roleGroup = new ToggleGroup();
        
        // Create a radio button for each role assigned to the user
        user.getRoles().forEach(role -> {
            RadioButton roleButton = new RadioButton(role);
            roleButton.setToggleGroup(roleGroup);
            vbox.getChildren().add(roleButton);
        });
        Button proceedButton = new Button("Proceed");
        proceedButton.setOnAction(e -> {
            RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
            if (selectedRole != null) {
                HomeController homeController = new HomeController(primaryStage, user, selectedRole.getText());
                homeController.showHomePage();
            }
        });
        vbox.getChildren().add(proceedButton);
        primaryStage.setScene(new Scene(vbox, 300, 200));
    }
    
    // Method to display the password reset page layout
    public void showPasswordResetPage() {
        VBox vbox = new VBox(10);
        PasswordField newPasswordField = new PasswordField(), confirmNewPasswordField = new PasswordField();
        Label messageLabel = new Label();
        Button resetButton = new Button("Reset Password");
        resetButton.setOnAction(e -> {
            String newPassword = newPasswordField.getText(), confirmNewPassword = confirmNewPasswordField.getText();
            if (newPassword.equals(confirmNewPassword) && !newPassword.isEmpty()) {
                user.setPassword(newPassword);
                user.clearPasswordReset();
                LoginController loginController = new LoginController(primaryStage);
                loginController.showLoginPage();
            } else messageLabel.setText("Passwords do not match or are empty.");
        });
        vbox.getChildren().addAll(new Label("Enter New Password:"), newPasswordField,
                new Label("Confirm New Password:"), confirmNewPasswordField, resetButton, messageLabel);
        primaryStage.setScene(new Scene(vbox, 300, 200));
    }    
}
