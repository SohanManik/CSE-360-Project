package application;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.*;

/**
 * The Login System uses JavaFX application that simulates a user registration and login system. It allows new users to register, 
 * allows exiting users to login, and allows administrators to manage user roles. It has support for an invitation code system 
 * for specific role-based registration and account set-up. There is also password reset functionality.
 * 
 * @author Jin Paomey, Jaylen Simmons, Bradyn Flahart, Zachary Riedel, Sohan Manik
 * 
 * @verion 1.00 general code JavaFx application
 * @version 1.1 added role functionality
 * @version 1.2 implementation of admin functionality 
 * @version 1.3 invitation code implementation
 */
public class LoginSystemJavaFX extends Application {
	
	// list to hold all registered users
    private static List<User> userList = new ArrayList<>();
    
    // map to hold invitation codes and the roles that are assigned with each code
    private static Map<String, Invitation> invitations = new HashMap<>();
    
    // Primary stage of JavaFX application
    private Stage primaryStage;
    
    // text fields used for username, password, invitation code, and confirm password
    private TextField usernameField = new TextField(), invitationCodeField = new TextField();
    private PasswordField passwordField = new PasswordField(), confirmPasswordField = new PasswordField();

    
    /**
     * Represents the main entry point of the application, initializes primary stage and displays the login page to the user
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Login System");
        showLoginPage(); //display the initial login page
    }

    /**
     * Displays the login page with input fields including Username, password, confirm password, and an invitational code
     */
    private void showLoginPage() {
        GridPane grid = new GridPane();
        Label messageLabel = new Label(); //label to display login messages
        grid.setVgap(8); grid.setHgap(10); //used to set grid spacing
        
        // used to add username, password, confirm password, and invitation code fields
        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Password:"), passwordField);
        grid.addRow(2, new Label("Confirm Password:"), confirmPasswordField);
        grid.addRow(3, new Label("Invitation Code:"), invitationCodeField);
        
        // create the login/register button
        Button loginButton = new Button("Login / Register");
        loginButton.setOnAction(e -> handleLoginOrRegister(messageLabel)); //calls the login register handle
        
        //adds button/message label to grid
        grid.addRow(4, loginButton, messageLabel);
        primaryStage.setScene(new Scene(grid, 400, 300));
        primaryStage.show();
    }

    /**
     * Handles the login or registration based on the input provided by the user. If an invitation code is used,
     * the system checks the invitation code and proceeds. If no users exist yet, it registers as an admin.
     * 
     * @param messageLabel the label to display messages like error/success
     */
    private void handleLoginOrRegister(Label messageLabel) {
        String username = usernameField.getText().trim(), password = passwordField.getText(), // get username and password from textbox
                confirmPassword = confirmPasswordField.getText(), invitationCode = invitationCodeField.getText().trim(); // get confirm password and invitation code from textbox
        
        //if there is an invitation code, handle it
        if (!invitationCode.isEmpty()) handleInvitationCode(invitationCode, messageLabel); 
        
        //if no users currently exit, make an admin account
        else if (userList.isEmpty() && validatePassword(password, confirmPassword, messageLabel))
            registerAdmin(username, password, messageLabel);
        else {
        	//try to find user by username
            User user = findUserByUsername(username);
            
            if (user != null) {
            	//handles password reset if user needs to reset their password
                if (user.isPasswordResetRequired()) handlePasswordReset(user, password, messageLabel);
                
                //if the password matches, login
                else if (user.getPassword().equals(password)) {
                	//if account set-up is incomplete, show the account set-up page
                    if (!user.isAccountSetupComplete()) showAccountSetupPage(user);
                    
                    //otherwise, transport to the home page after successful login
                    else proceedAfterLogin(user);
                } else messageLabel.setText("Invalid username or password."); //if username/password is incorrect, display error  
            } 
            
            //if the user does not exist, and criteria is met, allow user to select role
            else if (validatePassword(password, confirmPassword, messageLabel))
                showRoleSelectionForRegistration(username, password);
            else messageLabel.setText("Invalid login or registration details.");
        }
        //clear the input fields
        clearFields();
    }

    /**
     * This is used for handling the invitation code input, by checking if the invitation code is valid and then proceeding with registration
     * if the code provided is valid
     * @param code the invitation code
     * @param messageLabel the label to display messages to the user trying to login
     */
    
    private void handleInvitationCode(String code, Label messageLabel) {
        Invitation invitation = invitations.get(code); //Look up the invitation code
        
        //if the invitation is correct, proceed to the login page with the roles provided by the code
        if (invitation != null) showRegistrationPageWithRoles(invitation.getRoles(), code);
        
        //display error message if code is invalid
        else messageLabel.setText("Invalid invitation code.");
    }

    /**
     * Handles password reset requests when used resets their password. Will also verify one time password if it is valid
     * @param user the user resetting their password
     * @param oneTimePassword the password provided by the user for reset
     * @param messageLabel the label to display messages to the user
     */
    
    private void handlePasswordReset(User user, String oneTimePassword, Label messageLabel) {
    	//Verify the one-time
        if (oneTimePassword.equals(user.getOneTimePassword()) && LocalDateTime.now().isBefore(user.getPasswordExpiry()))
            showPasswordResetPage(user); //show the password reset page if valid
        
        //Display an error message if one-time password is invalid
        else messageLabel.setText("Invalid or expired one-time password.");
    }

    /**
     * Displays the password reset page so the user can create and confirm their new password
     * @param user user resetting password
     */
    
    private void showPasswordResetPage(User user) {
        VBox vbox = new VBox(10); //vertical box layout
        
        //create a new password and confirm fields
        PasswordField newPasswordField = new PasswordField(), confirmNewPasswordField = new PasswordField();
        Label messageLabel = new Label(); //label for displaying password reset messages
        
        //button used to reset passwprd
        Button resetButton = new Button("Reset Password");
        resetButton.setOnAction(e -> {
            String newPassword = newPasswordField.getText(), confirmNewPassword = confirmNewPasswordField.getText();
            
            //check if the passwords are empty, and check if they match
            if (newPassword.equals(confirmNewPassword) && !newPassword.isEmpty()) {
                user.setPassword(newPassword); //update password
                user.clearPasswordReset(); //clear the password reset request
                showLoginPage(); //return to the login page
            } else messageLabel.setText("Passwords do not match or are empty."); //show error if passwords are empty or dont match
        });
        
        //add fields and button to the vertical box layout
        vbox.getChildren().addAll(new Label("Enter New Password:"), newPasswordField,
                new Label("Confirm New Password:"), confirmNewPasswordField, resetButton, messageLabel);
        //set the scene and show it on the primary stage
        primaryStage.setScene(new Scene(vbox, 300, 200));
    }

    /**
     * validates the password and confirm password fields
     * ensures that both passwords are not empty and match
     * @param password the password entered by the user
     * @param confirmPassword the confirmed password entered by the user
     * @param messageLabel the label to display error messages
     * @return true if the passwords are valid and work, will return false otherwise
     */
    private boolean validatePassword(String password, String confirmPassword, Label messageLabel) {
    	//check if password fields are empty
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Password fields cannot be empty.");
            return false;
        }
        //check if passwords match
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return false;
        }
        return true; //passwords are valid
    }

    /**
     * Register the first user as an admin if there are no users 
     * 
     * @param username username of admin user created
     * @param password the password of the admin user created
     * @param messageLabel the label to display error/confirm messages
     */
    
    private void registerAdmin(String username, String password, Label messageLabel) {
        User admin = new User(username, password); //create a new user with credentials
        admin.addRole("Administrator"); // assign admin role to user
        userList.add(admin); // add the admin to the list of users
        messageLabel.setText("Admin account created. Please log in again."); //display message to user 
    }

    /**
     * Searches for a user in the user list by username
     * @param username the username to look for
     * @return the user object is found will return true, otherwise null
     */
    private User findUserByUsername(String username) {
    	//Stream through the user list and return the match
        return userList.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    /**
     * clears all input fields after login or registration
     */
    
    private void clearFields() {
    	//clear username, password, confirm password, and invitation code
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        invitationCodeField.clear();
    }

    /**
     * Displays the account setup page for a user that has not completed setup
     * User must enter additional details like email and name
     * @param user the user completing account setup
     */
    private void showAccountSetupPage(User user) {
        GridPane grid = new GridPane(); //grid layout for account setup form
        
        //fields for additional info
        TextField emailField = new TextField(), firstNameField = new TextField(),
                middleNameField = new TextField(), lastNameField = new TextField(),
                preferredFirstNameField = new TextField();
        Label setupMessageLabel = new Label(); //label for displaying error messages during
        
        //add input fields for account details to the grid
        grid.addRow(0, new Label("Email:"), emailField);
        grid.addRow(1, new Label("First Name:"), firstNameField);
        grid.addRow(2, new Label("Middle Name:"), middleNameField);
        grid.addRow(3, new Label("Last Name:"), lastNameField);
        grid.addRow(4, new Label("Preferred First Name:"), preferredFirstNameField);
        
        //Button to complete the account setup
        Button finishSetupButton = new Button("Finish Setup");
        finishSetupButton.setOnAction(e -> {
        	//ensure all required fields are filled
            if (emailField.getText().trim().isEmpty() || firstNameField.getText().trim().isEmpty()
                    || lastNameField.getText().trim().isEmpty())
                setupMessageLabel.setText("Please fill in all required fields.");
            else {
            	//set the user's details on the account and change account to being fully setup
                user.setDetails(emailField.getText(), firstNameField.getText(), middleNameField.getText(),
                        lastNameField.getText(), preferredFirstNameField.getText());
                user.setAccountSetupComplete(true); //account is marked as being fully complete
                proceedAfterLogin(user); //redirect to the home page
            }
        });
        //finish button and message label to the grid
        grid.addRow(5, finishSetupButton, setupMessageLabel);
        
        //set the scene for the account setup and show it
        primaryStage.setScene(new Scene(grid, 400, 350));
    }

    /**
     * Redirect to the next page if the login attempt is successful
     * If the user logging in has multiple roles, show the role selection page
     * @param user logged in user
     */
    private void proceedAfterLogin(User user) {
    	//if the user has more than one role, show the role selection page to the user
        if (user.getRoles().size() > 1) showRoleSelectionPage(user);
        else showHomePage(user, user.getRoles().get(0)); //otherwise go right to the home page
    }

    /**
     * Role selection page for users with multiple roles
     * User chooses which role they want to log in with
     * @param user logged in user
     */
    
    private void showRoleSelectionPage(User user) {
        VBox vbox = new VBox(10); //vertical box layout 
        ToggleGroup roleGroup = new ToggleGroup();
        
        //radio button for each role the user currently has
        user.getRoles().forEach(role -> {
            RadioButton roleButton = new RadioButton(role);
            roleButton.setToggleGroup(roleGroup);
            vbox.getChildren().add(roleButton); //each role is added as a radio button
        });
        
        //button to proceed with the selected role
        Button proceedButton = new Button("Proceed");
        proceedButton.setOnAction(e -> {
            RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle(); //give the user the selected role
            if (selectedRole != null) showHomePage(user, selectedRole.getText()); //show home page now with selected role
        });
        
        //add a proceed button to the layout
        vbox.getChildren().add(proceedButton);
        primaryStage.setScene(new Scene(vbox, 300, 200));
    }

    /**
     * displays the home page if the user has successfully logged in
     * the home page depends on the user's role
     * @param user user who is logged in
     * @param role the role of the user logged in for the current session
     */
    
    private void showHomePage(User user, String role) {
        VBox vbox = new VBox(10); //vertical box layout
        vbox.getChildren().add(new Label("Welcome, " + role + " " + user.getPreferredFirstNameOrDefault() + "!")); //welcome message
        
        //if the user is an admin, show admin control panel
        if ("Administrator".equals(role)) {
            TabPane adminTabs = new TabPane(); //tab for admin functions
            
            //various admin tabs for managing roles ad users when admin
            adminTabs.getTabs().addAll(
                    new Tab("Invite User", createInviteUserTab()), //user invite
                    new Tab("Reset Account", createResetUserTab()), //reset user
                    new Tab("Delete User", createDeleteUserTab()), //delete user
                    new Tab("List Users", createListUsersTab()), //list users
                    new Tab("Manage Roles", createManageRolesTab()) //manage roles
            );
            vbox.getChildren().add(adminTabs); //admin tabs are added to layout
        }
        //logout button to go back to home page
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> showLoginPage()); //show login oage when logout is pressed
        vbox.getChildren().add(logoutButton);
        //set the scene for homepage, then display
        primaryStage.setScene(new Scene(vbox, 600, 500));
    }

    /**
     * Creates the "Invite user" tab, this allows admins to invite users by generating a code
     * @return a Pane with controls for inviting users
     */
    private Pane createInviteUserTab() {
        VBox vbox = new VBox(10); //vertical box
        
        //checkboxes for selecting roles for users
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor");
        Label codeLabel = new Label(), messageLabel = new Label();
        
        //button to generate an invitation code that is based on role permissions for the user
        Button generateCodeButton = new Button("Generate Invitation Code");
        generateCodeButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");
            
            //used to ensure at least one role is slected before generating a code
            if (!roles.isEmpty()) {
                String code = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
                invitations.put(code, new Invitation(roles)); //store the invitation code for checking later
                codeLabel.setText("Invitation Code: " + code); //display the generated invitation code
            } else messageLabel.setText("Select at least one role."); //error message if no roles are selected
        });
        
        //add elements to the layout
        vbox.getChildren().addAll(new Label("Select Roles for Invitation:"), studentCheckBox, instructorCheckBox,
                generateCodeButton, codeLabel, messageLabel);
        return vbox; // return the layout for the invite user pane
    }

    /**
     * creates the "reset account" pane for admins
     * @return a Pane containing the controls for resetting accounts for admins
     */
    private Pane createResetUserTab() {
        VBox vbox = new VBox(10);//vertical box
        
        //input fields for usernam and password expiration time
        TextField usernameField = new TextField(), expiryTimeField = new TextField();
        DatePicker expiryDatePicker = new DatePicker();
        Label messageLabel = new Label();
        
        //button to reset a user account, generates a one time password
        Button resetButton = new Button("Reset User Account");
        resetButton.setOnAction(e -> {
            User user = findUserByUsername(usernameField.getText().trim());
            if (user != null) {
            	// generate a one time password and set an expiry time for the password
                String oneTimePassword = UUID.randomUUID().toString().substring(0, 4);
                LocalDateTime expiryDateTime = expiryDatePicker.getValue().atTime(
                        Integer.parseInt(expiryTimeField.getText().split(":")[0]),
                        Integer.parseInt(expiryTimeField.getText().split(":")[1]));
                
                user.setOneTimePassword(oneTimePassword, expiryDateTime); //set the one tie password expiry time
                messageLabel.setText("One-time password set: " + oneTimePassword); //display one time password
            } else messageLabel.setText("User not found."); // error message if user doesn't exist
        });
        
        // add elements to the layout
        vbox.getChildren().addAll(new Label("Username:"), usernameField, new Label("Expiry Date:"),
                expiryDatePicker, new Label("Expiry Time (HH:MM):"), expiryTimeField, resetButton, messageLabel);
        return vbox; //return the layout
    }

    /**
     * creates the delete user tab for the purpose of deleting users as an admin
     * @return delete user pane
     */
    
    
    private Pane createDeleteUserTab() {
        VBox vbox = new VBox(10);//vertical box
        
        //input field for the username of the user that is going to be deleted
        TextField usernameField = new TextField();
        Label messageLabel = new Label();
        
        //button to delete the user account
        Button deleteButton = new Button("Delete User Account");
        deleteButton.setOnAction(e -> {
            User user = findUserByUsername(usernameField.getText().trim()); //find the user by username
            if (user != null) {
            	//confirmation of account deletion
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        userList.remove(user); //remove the user from the list
                        messageLabel.setText("User account deleted."); //display confirmation that account was deleted
                    }
                });
            } else messageLabel.setText("User not found."); //error message if user does not exist 
        });
        
        //add elements to the layout
        vbox.getChildren().addAll(new Label("Username:"), usernameField, deleteButton, messageLabel);
        return vbox; //return the layout
    }

    /**
     * Creates the List users tab, lists all registered users and roles for admins
     * @return a pane containing the list users tab
     */
    
    private Pane createListUsersTab() {
        VBox vbox = new VBox(10); //vertical box layout
        
        //List view to display the list of users
        ListView<String> userListView = new ListView<>();
        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(e -> {
            userListView.getItems().clear(); //clear the existing list
            
            //populate the list with user details
            userList.forEach(user -> userListView.getItems().add(
                    "Username: " + user.getUsername() + ", Name: " + user.getFullName() + ", Roles: " + String.join(", ", user.getRoles())));
        });
        
        //add elements to layout
        vbox.getChildren().addAll(refreshButton, userListView);
        return vbox; //return elemts to the layout
    }

    /**
     * Creates the manage roles tab, allows admin to update roles of users 
     * @return a Pane containing the controls for managing user's roles
     */
    
    private Pane createManageRolesTab() {
        VBox vbox = new VBox(10); //vertical box layout
        
        //input field for username and roles checkboxes
        TextField usernameField = new TextField();
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor"),
                adminCheckBox = new CheckBox("Administrator");
        Label messageLabel = new Label();
        
        //button to update the roles of the user that is specified by the admin
        Button updateRolesButton = new Button("Update Roles");
        updateRolesButton.setOnAction(e -> {
            User user = findUserByUsername(usernameField.getText().trim()); //find the username to manage roles on
            if (user != null) {
                List<String> roles = new ArrayList<>();
                
                //add selected roles to the user's roles list. 
                if (studentCheckBox.isSelected()) roles.add("Student");
                if (instructorCheckBox.isSelected()) roles.add("Instructor");
                if (adminCheckBox.isSelected()) roles.add("Administrator");
                
                //ensure at least one role is selected 
                if (!roles.isEmpty()) {
                    user.setRoles(roles); //update the user's roles to the new roles
                    messageLabel.setText("Roles updated."); //user confirmation message
                } else messageLabel.setText("Select at least one role."); //error message if at least one role isnt selected
            } else messageLabel.setText("User not found."); //error message if user is not found
        });
        //add elements to layout
        vbox.getChildren().addAll(new Label("Username:"), usernameField, new Label("Assign Roles:"),
                studentCheckBox, instructorCheckBox, adminCheckBox, updateRolesButton, messageLabel);
        return vbox; //return elements to the layout
    } 

    /**
     * Displays the registration page where the user can register with the roles provided
     * @param roles the roles that the user can choose during registration
     * @param invitationCode the invitation code used for registration
     */
    
    private void showRegistrationPageWithRoles(List<String> roles, String invitationCode) {
        VBox vbox = new VBox(10); //vertical box layout
        
        //input fields for username and passwords
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField(), confirmPasswordField = new PasswordField();
        Label messageLabel = new Label();
        
        //button to register the user with the roles provided in the invitation
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> {
        	
        	//validate the password fields
            if (validatePassword(passwordField.getText(), confirmPasswordField.getText(), messageLabel)) {
                User newUser = new User(usernameField.getText().trim(), passwordField.getText(), roles);
                userList.add(newUser); //add the new user to the list
                invitations.remove(invitationCode); //remove the used invitation code
                showLoginPage(); //return to the login page after successful registration
            }
        });
        
        //add elements to the layout
        vbox.getChildren().addAll(new Label("Username:"), usernameField, new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmPasswordField, registerButton, messageLabel);
        //set the scene and display it
        primaryStage.setScene(new Scene(vbox, 300, 250));
    }

    /**
     * displays the role selection page for user registration
     * users can select roles, teacher or instructor before registering
     * @param username the username of the user being registered
     * @param password the password of the user being registered 
     */
    
    private void showRoleSelectionForRegistration(String username, String password) {
        VBox vbox = new VBox(10); //vertical box layout
        
        //checkboxes for role selection
        CheckBox studentCheckBox = new CheckBox("Student"), instructorCheckBox = new CheckBox("Instructor");
        Label messageLabel = new Label();
        
        //button to register the user with the selected roles
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> {
            List<String> roles = new ArrayList<>();
            if (studentCheckBox.isSelected()) roles.add("Student");
            if (instructorCheckBox.isSelected()) roles.add("Instructor");
            
            //ensure at least one role is selected before proceeding
            if (!roles.isEmpty()) {
                User newUser = new User(username, password, roles); //create a new user with roles selected
                userList.add(newUser); //add the new user to the user list
                showLoginPage(); //return to the login page after registration
            } else messageLabel.setText("Please select at least one role."); //error message if at least one role isnt selected
        });
        //add elements to the layout
        vbox.getChildren().addAll(new Label("Select your roles:"), studentCheckBox, instructorCheckBox, registerButton, messageLabel);
        
        //set the scene and display it
        primaryStage.setScene(new Scene(vbox, 300, 200));
    }

    public static void main(String[] args) {
        launch(args); //launch the JavaFX application
    }
    
    /**
     * user class represents a user in the system with attributes like username, password, emails, and roles assigned
     */

    public static class User {
        private String username, password, email, firstName, middleName, lastName, preferredFirstName, oneTimePassword;
        private List<String> roles = new ArrayList<>();
        private boolean accountSetupComplete = false;
        private LocalDateTime passwordExpiry;

        public User(String username, String password) {
            this.username = username; this.password = password;
        }

        public User(String username, String password, List<String> roles) {
            this(username, password); this.roles.addAll(roles);
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public List<String> getRoles() { return roles; }
        public boolean isAccountSetupComplete() { return accountSetupComplete; }
        public void setPassword(String password) { this.password = password; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public void setDetails(String email, String firstName, String middleName, String lastName, String preferredFirstName) {
            this.email = email; this.firstName = firstName; this.middleName = middleName;
            this.lastName = lastName; this.preferredFirstName = preferredFirstName;
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
            this.oneTimePassword = oneTimePassword; this.passwordExpiry = expiry;
        }

        public boolean isPasswordResetRequired() {
            return oneTimePassword != null && LocalDateTime.now().isBefore(passwordExpiry);
        }

        public void clearPasswordReset() {
            this.oneTimePassword = null; this.passwordExpiry = null;
        }

        public String getFullName() {
            return String.format("%s %s %s", firstName, middleName != null ? middleName : "", lastName).trim();
        }
    }

    /**
     * invitation class represents an invitation to register with a set of predefined roles
     */
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
