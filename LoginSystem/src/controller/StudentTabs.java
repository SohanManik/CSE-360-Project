package controller;

import model.DatabaseHelper;
import model.DataStore;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.util.List;
import model.HelpSystem;

/**
 * The StudentTabs class provides UI components for the student users of the help system.
 * This class contains methods for creating tabs, such as a help system tab and an article search tab.
 */
public class StudentTabs {
    // DatabaseHelper instance for interacting with the database
    private static DatabaseHelper databaseHelper;

    // Static block to initialize the databaseHelper instance
    static {
        try {
            databaseHelper = DatabaseHelper.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a VBox with padding to be used for layout purposes.
     * @return a VBox with 10px spacing and 10px padding.
     */
    private static VBox createVBoxWithPadding() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    /**
     * Adds a label and input field to the given VBox.
     * @param vbox The VBox to which the label and input field will be added.
     * @param labelText The text for the label.
     * @param field The input field (TextField, TextArea, etc.) to be added.
     */
    private static void setupLabelAndField(VBox vbox, String labelText, Control field) {
        vbox.getChildren().addAll(new Label(labelText), field);
    }

    /**
     * Creates the Help System tab for students to send generic or specific messages.
     * This tab contains fields to send either a general help message or a specific query.
     * @return a VBox containing UI components for the help system.
     */
    public static VBox createHelpSystemTab() {
        VBox vbox = createVBoxWithPadding();

        // Components for sending a generic help message
        TextField genericMessageField = new TextField();
        Button sendGenericButton = new Button("Send Generic Message");
        Label genericMessageLabel = new Label();

        // Action for sending a generic message
        sendGenericButton.setOnAction(e -> {
            String message = genericMessageField.getText().trim();
            if (!message.isEmpty()) {
                HelpSystem.sendGenericMessage(message);
                genericMessageLabel.setText("Generic message sent.");
                genericMessageField.clear();
            } else {
                genericMessageLabel.setText("Please enter a message.");
            }
        });

        // Components for sending a specific help message
        TextField queryField = new TextField();
        TextArea specificMessageField = new TextArea();
        Button sendSpecificButton = new Button("Send Specific Message");
        Label specificMessageLabel = new Label();

        // Action for sending a specific message
        sendSpecificButton.setOnAction(e -> {
            String query = queryField.getText().trim();
            String message = specificMessageField.getText().trim();

            if (!query.isEmpty() && !message.isEmpty()) {
                HelpSystem.sendSpecificMessage(query, message);
                specificMessageLabel.setText("Specific message sent for query: " + query);
                queryField.clear();
                specificMessageField.clear();
            } else {
                specificMessageLabel.setText("Please enter a query and message.");
            }
        });

        // Adding components to the VBox
        setupLabelAndField(vbox, "Generic Message:", genericMessageField);
        vbox.getChildren().addAll(sendGenericButton, genericMessageLabel);

        setupLabelAndField(vbox, "Specific Query:", queryField);
        setupLabelAndField(vbox, "Specific Message:", specificMessageField);
        vbox.getChildren().addAll(sendSpecificButton, specificMessageLabel);

        return vbox;
    }

    /**
     * Creates the Search Articles tab for students to search help articles.
     * This tab allows students to search articles by text, content level, and group.
     * @return a VBox containing UI components for searching articles.
     */
    public static VBox createSearchArticlesTab() {
        VBox vbox = createVBoxWithPadding();

        // Search-related components
        TextField searchField = new TextField();
        ChoiceBox<String> levelChoiceBox = new ChoiceBox<>();
        ChoiceBox<String> groupChoiceBox = new ChoiceBox<>();
        ListView<String> resultsListView = new ListView<>();
        Label messageLabel = new Label();
        Button searchButton = new Button("Search");

        // Populate level choices
        levelChoiceBox.getItems().addAll("All", "Beginner", "Intermediate", "Advanced", "Expert");
        levelChoiceBox.setValue("All"); // Default value

        // Populate group choices (fetch from database or predefined list)
        groupChoiceBox.getItems().addAll("All", "Assignment 1", "Assignment 2"); // Example groups
        groupChoiceBox.setValue("All"); // Default value

        // Action for performing a search
        searchButton.setOnAction(e -> {
            try {
                String searchText = searchField.getText().trim();
                String level = levelChoiceBox.getValue();
                String group = groupChoiceBox.getValue();

                // Perform search
                List<String> results = databaseHelper.searchArticles(searchText, level, group);
                resultsListView.getItems().setAll(results);

                // Display active group and level statistics
                String activeGroup = "Active Group: " + group;
                String levelStats = databaseHelper.getLevelStatistics(results);
                messageLabel.setText(activeGroup + "\n" + levelStats);
            } catch (Exception ex) {
                messageLabel.setText("Error during search: " + ex.getMessage());
            }
        });

        // Adding components to the VBox
        setupLabelAndField(vbox, "Search Text:", searchField);
        vbox.getChildren().addAll(new Label("Content Level:"), levelChoiceBox);
        vbox.getChildren().addAll(new Label("Group:"), groupChoiceBox);
        vbox.getChildren().addAll(searchButton, resultsListView, messageLabel);

        return vbox;
    }
}
