package controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;

/**
 * The HomeController class handles the logic and UI for displaying
 * the home page of the application. The home page content is dynamically
 * generated based on the role of the logged-in user (Administrator, Instructor, or Student).
 */

public class HomeController {

    // Primary stage used to display scenes
    private Stage primaryStage;

    // The currently logged-in user
    private User user;

    // The role of the currently logged-in user (e.g., Administrator, Instructor, Student)
    private String role;

    /**
     * Constructor for the HomeController.
     *
     * @param primaryStage the main stage where the application is displayed
     * @param user         the current user object containing user details
     * @param role         the role of the current user
     */
    public HomeController(Stage primaryStage, User user, String role) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.role = role;
    }

    /**
     * Displays the home page with content and options tailored to the user's role.
     */
    public void showHomePage() {
        // Create a VBox layout with a spacing of 10 pixels
        VBox vbox = new VBox(10);

        // Welcome message displaying the user's role and name
        vbox.getChildren().add(new Label("  Welcome, " + role + " " + user.getPreferredFirstNameOrDefault() + "!"));

        // Add specific tabs and functionalities based on the user's role
        if ("Administrator".equals(role)) {
            // Administrator-specific tabs
            TabPane adminTabs = new TabPane();
            adminTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); // Prevent tabs from being closed

            // Add tabs for various administrative functionalities
            adminTabs.getTabs().addAll(
                new Tab("Add Article", AdminTabs.createAddArticleTab()),
                new Tab("List Articles", AdminTabs.createListArticlesTab()),
                new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
                new Tab("Backup Articles", AdminTabs.createBackupArticlesTab()),
                new Tab("Restore Articles", AdminTabs.createRestoreArticlesTab()),
                new Tab("Invite User", AdminTabs.createInviteUserTab()),
                new Tab("Reset Account", AdminTabs.createResetUserTab()),
                new Tab("Delete User", AdminTabs.createDeleteUserTab()),
                new Tab("List Users", AdminTabs.createListUsersTab()),
                new Tab("Manage Roles", AdminTabs.createManageRolesTab()),
                new Tab("Manage Groups", AdminTabs.createManageGroupsTab()),
                new Tab("View Group Users", AdminTabs.createViewGroupUsersTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())
            );
            vbox.getChildren().add(adminTabs);
        } else if ("Instructor".equals(role)) {
            // Instructor-specific tabs
            TabPane instructorTabs = new TabPane();
            instructorTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            // Add tabs for various instructor functionalities
            instructorTabs.getTabs().addAll(
                new Tab("Add Article", AdminTabs.createAddArticleTab()),
                new Tab("List Articles", AdminTabs.createListArticlesTab()),
                new Tab("View Articles", AdminTabs.createViewArticleTab()),
                new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
                new Tab("Backup Articles", AdminTabs.createBackupArticlesTab()),
                new Tab("Restore Articles", AdminTabs.createRestoreArticlesTab()),
                new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
                new Tab("Manage Groups", AdminTabs.createManageGroupsTab()),
                new Tab("Manage Group Users", AdminTabs.createViewGroupUsersTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())
            );
            vbox.getChildren().add(instructorTabs);
        } else if ("Student".equals(role)) {
            // Student-specific tabs
            TabPane studentTabs = new TabPane();
            studentTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            // Add tabs for various student functionalities
            studentTabs.getTabs().addAll(
                new Tab("Help System", StudentTabs.createHelpSystemTab()),
                new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
                new Tab("View Articles", AdminTabs.createViewArticleTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())
            );
            vbox.getChildren().add(studentTabs);
        }

        // Add a Logout button to the home page
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            // On logout, redirect to the login page
            LoginController loginController = new LoginController(primaryStage);
            loginController.showLoginPage();
        });
        vbox.getChildren().add(logoutButton);

        // Set the scene with the VBox layout and display it on the primary stage
        primaryStage.setScene(new Scene(vbox, 520, 560));
    }
}
