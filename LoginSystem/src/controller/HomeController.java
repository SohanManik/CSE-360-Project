package controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;

public class HomeController {

    // Reference to the primary stage, the current user, and the user's role
    private Stage primaryStage;
    private User user;
    private String role;

    // Constructor initializing the primary stage, user, and role
    public HomeController(Stage primaryStage, User user, String role) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.role = role;
    }

    // Method to display the home page with content based on the user's role
    public void showHomePage() {
        VBox vbox = new VBox(10);
        vbox.getChildren().add(new Label("  Welcome, " + role + " " + user.getPreferredFirstNameOrDefault() + "!"));
        
        // If the user is an Administrator, display all admin tabs
        if ("Administrator".equals(role)) {
            TabPane adminTabs = new TabPane();
            adminTabs.getTabs().addAll(
            		new Tab("Add Article", AdminTabs.createAddArticleTab()),
            		new Tab("List Article", AdminTabs.createListArticlesTab()),
            		new Tab("Manage Access", AdminTabs.createManageAccessRightsTab()),
            		new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
            		new Tab("Backup Article", AdminTabs.createBackupArticlesTab()),
            		new Tab("Restore Article", AdminTabs.createRestoreArticlesTab()),
                    new Tab("Invite User", AdminTabs.createInviteUserTab()),
                    new Tab("Reset Account", AdminTabs.createResetUserTab()),
                    new Tab("Delete User", AdminTabs.createDeleteUserTab()),
                    new Tab("List Users", AdminTabs.createListUsersTab()),
                    new Tab("Manage Roles", AdminTabs.createManageRolesTab())
            );
            vbox.getChildren().add(adminTabs);
        }
        
        // If the user is an Instructor, display a limited set of tabs
        else if ("Instructor".equals(role)) {
            TabPane adminTabs = new TabPane();
            adminTabs.getTabs().addAll(
            		new Tab("Add Article", AdminTabs.createAddArticleTab()),
            		new Tab("List Article", AdminTabs.createListArticlesTab()),
            		new Tab("Manage Group", AdminTabs.createViewArticleTabForInstructor()),
            		new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
            		new Tab("Backup Article", AdminTabs.createBackupArticlesTab()),
            		new Tab("Restore Article", AdminTabs.createRestoreArticlesTab()),
            		new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
            		new Tab("View Articles", AdminTabs.createViewArticleTab())
            );
            vbox.getChildren().add(adminTabs);
        }
        
        else if ("Student".equals(role)) {
            TabPane studentTabs = new TabPane();
            studentTabs.getTabs().addAll(
                new Tab("Help System", StudentTabs.createHelpSystemTab()),
                new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
                new Tab("View Articles", AdminTabs.createViewArticleTab())
            );
            vbox.getChildren().add(studentTabs);
        }
        
        // Logout button for navigating back to the login page
        Button logoutButton = new Button("Logout");
        
        logoutButton.setOnAction(e -> {
            // Redirect to the login page by initializing a new LoginController
            LoginController loginController = new LoginController(primaryStage);
            loginController.showLoginPage();
        });
        
        vbox.getChildren().add(logoutButton);				// Adding the logout button to the layout
        
        primaryStage.setScene(new Scene(vbox, 520, 560));	// Setting the scene with specified dimensions
    }
}
