package controller;

import model.DatabaseHelper;
import model.DataStore;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.util.List;
import model.HelpSystem;

	public class StudentTabs {
	    private static DatabaseHelper databaseHelper;

	    static {
	        try {
	            databaseHelper = DatabaseHelper.getInstance();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    private static VBox createVBoxWithPadding() {
	        VBox vbox = new VBox(10);
	        vbox.setPadding(new Insets(10));
	        return vbox;
	    }

	    private static void setupLabelAndField(VBox vbox, String labelText, Control field) {
	        vbox.getChildren().addAll(new Label(labelText), field);
	    }

	    public static VBox createHelpSystemTab() {
	        VBox vbox = createVBoxWithPadding();

	        // Generic help message
	        TextField genericMessageField = new TextField();
	        Button sendGenericButton = new Button("Send Generic Message");
	        Label genericMessageLabel = new Label();

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

	        // Specific help message
	        TextField queryField = new TextField();
	        TextArea specificMessageField = new TextArea();
	        Button sendSpecificButton = new Button("Send Specific Message");
	        Label specificMessageLabel = new Label();

	        sendSpecificButton.setOnAction(e -> {
	            String query = queryField.getText().trim();
	            String message = specificMessageField.getText().trim();

	            if (!query.isEmpty() && !message.isEmpty()) {
	                HelpSystem.sendSpecificMessage(query, message); // Correct method call
	                specificMessageLabel.setText("Specific message sent for query: " + query);
	                queryField.clear();
	                specificMessageField.clear();
	            } else {
	                specificMessageLabel.setText("Please enter a query and message.");
	            }
	        });

	        // Add components to the VBox
	        setupLabelAndField(vbox, "Generic Message:", genericMessageField);
	        vbox.getChildren().addAll(sendGenericButton, genericMessageLabel);

	        setupLabelAndField(vbox, "Specific Query:", queryField);
	        setupLabelAndField(vbox, "Specific Message:", specificMessageField);
	        vbox.getChildren().addAll(sendSpecificButton, specificMessageLabel);

	        return vbox;
	    }

	    public static VBox createSearchArticlesTab() {
	        VBox vbox = createVBoxWithPadding();
	        
	        // Search-related fields and controls
	        TextField searchField = new TextField();
	        ChoiceBox<String> levelChoiceBox = new ChoiceBox<>();
	        ChoiceBox<String> groupChoiceBox = new ChoiceBox<>();
	        ListView<String> resultsListView = new ListView<>();
	        Label messageLabel = new Label();
	        Button searchButton = new Button("Search");
	        //Button viewArticleButton = new Button("View Article");
	        
	        // Populate level choices
	        levelChoiceBox.getItems().addAll("All", "Beginner", "Intermediate", "Advanced", "Expert");
	        levelChoiceBox.setValue("All"); // Default
	        
	        // Populate group choices (fetch from database or predefined list)
	        groupChoiceBox.getItems().addAll("All", "Assignment 1", "Assignment 2"); // Example groups
	        groupChoiceBox.setValue("All"); // Default
	        
	        searchButton.setOnAction(e -> {
	            try {
	                String searchText = searchField.getText().trim();
	                String level = levelChoiceBox.getValue();
	                String group = groupChoiceBox.getValue();

	                // Perform search
	                List<String> results = databaseHelper.searchArticles(searchText, level, group);
	                resultsListView.getItems().setAll(results);

	                // Display active group and level stats
	                String activeGroup = "Active Group: " + group;
	                String levelStats = databaseHelper.getLevelStatistics(results);
	                messageLabel.setText(activeGroup + "\n" + levelStats);
	            } catch (Exception ex) {
	                messageLabel.setText("Error during search: " + ex.getMessage());
	            }
	        });



	        setupLabelAndField(vbox, "Search Text:", searchField);
	        vbox.getChildren().addAll(new Label("Content Level:"), levelChoiceBox);
	        vbox.getChildren().addAll(new Label("Group:"), groupChoiceBox);
	        vbox.getChildren().addAll(searchButton, resultsListView, messageLabel);
	        return vbox;
	    }
	}

