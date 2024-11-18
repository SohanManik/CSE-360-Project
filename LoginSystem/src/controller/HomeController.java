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

        if ("Administrator".equals(role)) {
            // Administrator Tabs
            TabPane adminTabs = new TabPane();
            adminTabs.getTabs().addAll(
                new Tab("Add Article", AdminTabs.createAddArticleTab()),
                new Tab("List Articles", AdminTabs.createListArticlesTab()),
                new Tab("Manage Special Groups", AdminTabs.createManageSpecialAccessGroupsTab()),
                new Tab("Grant Rights in Groups", AdminTabs.createManageAccessRightsTab()),
                new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
                new Tab("Backup Articles", AdminTabs.createBackupArticlesTab()),
                new Tab("Restore Articles", AdminTabs.createRestoreArticlesTab()),
                new Tab("Invite User", AdminTabs.createInviteUserTab()),
                new Tab("Reset Account", AdminTabs.createResetUserTab()),
                new Tab("Delete User", AdminTabs.createDeleteUserTab()),
                new Tab("List Users", AdminTabs.createListUsersTab()),
                new Tab("Manage Roles", AdminTabs.createManageRolesTab()),
                new Tab("View Group Users", AdminTabs.createViewGroupUsersTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())

            );
            vbox.getChildren().add(adminTabs);
        } else if ("Instructor".equals(role)) {
            // Instructor Tabs
            TabPane instructorTabs = new TabPane();
            instructorTabs.getTabs().addAll(
                new Tab("Add Article", AdminTabs.createAddArticleTab()),
                new Tab("List Articles", AdminTabs.createListArticlesTab()),
                new Tab("Manage Special Groups", AdminTabs.createManageSpecialAccessGroupsTab()),
                new Tab("View Articles", AdminTabs.createViewArticleTab()),
                new Tab("Delete Article", AdminTabs.createDeleteArticleTab()),
                new Tab("Backup Articles", AdminTabs.createBackupArticlesTab()),
                new Tab("Restore Articles", AdminTabs.createRestoreArticlesTab()),
                new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
                new Tab("Manage Group Users", AdminTabs.createViewGroupUsersTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())


            );
            vbox.getChildren().add(instructorTabs);
        } else if ("Student".equals(role)) {
            TabPane studentTabs = new TabPane();
            studentTabs.getTabs().addAll(
                new Tab("Help System", StudentTabs.createHelpSystemTab()),
                new Tab("Search Articles", StudentTabs.createSearchArticlesTab()),
                new Tab("View Articles", AdminTabs.createViewArticleTab()),
                new Tab("View Articles in Group", AdminTabs.createViewArticlesInGroupTab())

            );
            vbox.getChildren().add(studentTabs);
        }

        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> {
            LoginController loginController = new LoginController(primaryStage);
            loginController.showLoginPage();
        });
        vbox.getChildren().add(logoutButton);

        primaryStage.setScene(new Scene(vbox, 520, 560));
    }
}
