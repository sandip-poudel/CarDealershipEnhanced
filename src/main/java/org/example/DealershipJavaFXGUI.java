package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DealershipJavaFXGUI class provides a JavaFX-based graphical user interface for managing
 * vehicle dealership operations. This class handles all user interactions, input validation,
 * and visual display of dealership data.
 */
public class DealershipJavaFXGUI extends Application {
    // Constants for file paths and colors
    private static final String INVENTORY_PATH = "src/main/resources/inventory.json";
    private static final String EXPORT_PATH = "src/main/resources/export.json";
    private static final String APP_TITLE = "Dealership Management System";
    private static final Color THEME_COLOR = Color.DODGERBLUE; // Main color

    // Dark mode properties
    private boolean darkModeEnabled = false;
    private final String LIGHT_THEME_STYLE = "-fx-background-color: white; -fx-text-fill: black;";
    private final String DARK_THEME_STYLE = "-fx-background-color: #333333; -fx-text-fill: white;";
    private final Color LIGHT_HEADER_COLOR = Color.DARKBLUE;
    private final Color DARK_HEADER_COLOR = Color.rgb(0, 102, 102); // Dark blue

    // Core business logic manager
    private DealershipManager manager;

    // Input fields
    private TextField dealerIdField;
    private ComboBox<String> dealerIdComboBox;
    private ComboBox<String> vehicleTypeComboBox;
    private TextField vehicleIdField;
    private TextField manufacturerField;
    private TextField modelField;
    private TextField priceField;

    // Search components
    private TextField searchField;
    private ComboBox<String> searchTypeComboBox;

    // Main GUI components
    private TextArea displayArea;
    private FileChooser fileChooser;

    @Override
    public void start(Stage primaryStage) {
        manager = new DealershipManager();

        // Set up the main layout
        BorderPane mainLayout = new BorderPane();

        // Create header
        VBox headerPane = createHeaderPanel();
        mainLayout.setTop(headerPane);

        // Create search area and add it below the header
        HBox searchArea = createSearchArea();

        // Create center section with input fields and buttons
        SplitPane centerPane = new SplitPane();
        centerPane.getItems().addAll(createInputPanel(), createButtonPanel());
        centerPane.setDividerPositions(0.6);

        // Put search and center pane in a VBox
        VBox centerContent = new VBox(searchArea, centerPane);
        VBox.setVgrow(centerPane, Priority.ALWAYS);

        // Create display area with resizable capability
        displayArea = createDisplayArea();
        ScrollPane scrollPane = new ScrollPane(displayArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Make it resizable
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplitPane.getItems().addAll(centerContent, scrollPane);
        mainSplitPane.setDividerPositions(0.6);
        mainLayout.setCenter(mainSplitPane);

        // Set up the scene
        Scene scene = new Scene(mainLayout, 1000, 700);

        // Configure the stage
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);

        // Add dark mode toggle
        setupDarkModeToggle(mainLayout, primaryStage);

        // Show the stage
        primaryStage.show();

        // Load initial data
        loadInitialInventory();
    }

    /**
     * Creates the header panel with title
     */
    private VBox createHeaderPanel() {
        VBox header = new VBox();
        header.setPadding(new Insets(15, 15, 15, 15));
        String colorHex = String.format("#%02X%02X%02X",
                (int) (THEME_COLOR.getRed() * 255),
                (int) (THEME_COLOR.getGreen() * 255),
                (int) (THEME_COLOR.getBlue() * 255));
        header.setStyle("-fx-background-color: " + colorHex + ";");

        Label titleLabel = new Label(APP_TITLE);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);

        header.getChildren().add(titleLabel);
        return header;
    }

    /**
     * Creates and sets up the search area
     */
    /**
     * Creates and sets up the search area
     */
    private HBox createSearchArea() {
        HBox searchBox = new HBox(10);
        searchBox.setPadding(new Insets(10, 10, 10, 10));
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Create search label
        Label searchLabel = new Label("Search:");
        searchLabel.setStyle("-fx-font-weight: bold;");

        // Create search type combo box
        searchTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "ID", "Manufacturer", "Model", "Dealer ID", "Type", "All Fields"));
        searchTypeComboBox.setValue("All Fields");
        searchTypeComboBox.setPrefWidth(150);

        // Create search text field
        searchField = new TextField();
        searchField.setPromptText("Enter search query");
        searchField.setPrefWidth(250);

        // Remove the listener that performs search as user types
        // Instead, only search when the button is clicked or Enter is pressed
        searchField.setOnAction(e -> performSearch(searchField.getText()));

        // Still show all vehicles when search field is cleared
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                refreshDisplay(); // Show all vehicles when search field is cleared
            }
        });

        // Create search button
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch(searchField.getText()));

        // Create dashboard button
        Button dashboardButton = new Button("Dashboard");
        dashboardButton.setOnAction(e -> showDashboard());
        dashboardButton.setStyle("-fx-font-weight: bold; -fx-background-color: lightgreen;");

        // Add components to search box
        searchBox.getChildren().addAll(searchLabel, searchTypeComboBox, searchField, searchButton, dashboardButton);

        return searchBox;
    }

    /**
     * Creates the input panel with form fields
     */
    private GridPane createInputPanel() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20, 20, 20, 20));
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        // Initialize input fields
        dealerIdField = new TextField();

        // Initialize dealership dropdown
        dealerIdComboBox = new ComboBox<>();
        dealerIdComboBox.setPromptText("Select existing dealer");
        dealerIdComboBox.setEditable(false);

        // Create a HBox to hold both dealer ID options
        HBox dealerSelectionBox = new HBox(10);
        dealerSelectionBox.getChildren().addAll(dealerIdComboBox, dealerIdField);

        // Add null/clear option to the dropdown
        dealerIdComboBox.getItems().add("-- New Dealer --");

        // Set up dealership dropdown listener
        dealerIdComboBox.setOnAction(e -> {
            if (dealerIdComboBox.getValue() != null) {
                if (dealerIdComboBox.getValue().equals("-- New Dealer --")) {
                    // Selected the "New Dealer" option
                    dealerIdField.clear();
                    dealerIdField.setDisable(false);
                } else {
                    // Selected an existing dealer
                    dealerIdField.clear();
                    dealerIdField.setDisable(true);
                }
            } else {
                dealerIdField.setDisable(false);
            }
        });

        // Set up dealerIdField listener to clear dropdown when text is entered
        dealerIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                dealerIdComboBox.setValue(null);
            }
        });

        vehicleTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "SUV", "Sedan", "Pickup", "Sports Car"));
        vehicleTypeComboBox.setValue("SUV");
        vehicleIdField = new TextField();
        manufacturerField = new TextField();
        modelField = new TextField();
        priceField = new TextField();

        // Create a large rent vehicle button
        Button rentVehicleBtn = new Button("Rent Vehicle");
        rentVehicleBtn.setPrefWidth(250);
        rentVehicleBtn.setPrefHeight(45);
        rentVehicleBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #A0E0E0;");
        rentVehicleBtn.setOnAction(e -> handleRentVehicle());

        // Create a large return vehicle button
        Button returnVehicleBtn = new Button("Return Vehicle");
        returnVehicleBtn.setPrefWidth(250);
        returnVehicleBtn.setPrefHeight(45);
        returnVehicleBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #E0A0A0;"); // Light red color
        returnVehicleBtn.setOnAction(e -> handleReturnVehicle());

        // Style fields to be consistent size
        dealerIdField.setPrefWidth(120);
        dealerIdComboBox.setPrefWidth(120);
        vehicleTypeComboBox.setPrefWidth(250);
        vehicleIdField.setPrefWidth(250);
        manufacturerField.setPrefWidth(250);
        modelField.setPrefWidth(250);
        priceField.setPrefWidth(250);

        // Add field labels
        gridPane.add(new Label("Dealer ID:"), 0, 0);
        gridPane.add(dealerSelectionBox, 1, 0);

        gridPane.add(new Label("Vehicle Type:"), 0, 1);
        gridPane.add(vehicleTypeComboBox, 1, 1);

        gridPane.add(new Label("Vehicle ID:"), 0, 2);
        gridPane.add(vehicleIdField, 1, 2);

        gridPane.add(new Label("Manufacturer:"), 0, 3);
        gridPane.add(manufacturerField, 1, 3);

        gridPane.add(new Label("Model:"), 0, 4);
        gridPane.add(modelField, 1, 4);

        gridPane.add(new Label("Price:"), 0, 5);
        gridPane.add(priceField, 1, 5);

        // Add rental and return buttons in rows 6 and 7
        gridPane.add(new Label(""), 0, 6);
        gridPane.add(rentVehicleBtn, 1, 6);
        gridPane.add(new Label(""), 0, 7);
        gridPane.add(returnVehicleBtn, 1, 7);

        return gridPane;
    }

    /**
     * Creates the button panel with all action buttons
     */
    private VBox createButtonPanel() {
        VBox buttonPanel = new VBox(10);
        buttonPanel.setPadding(new Insets(20, 20, 20, 20));

        fileChooser = new FileChooser();

        // Create buttons with consistent styling
        Button addVehicleBtn = createStyledButton("Add Vehicle");
        addVehicleBtn.setOnAction(e -> handleAddVehicle());

        Button removeVehicleBtn = createStyledButton("Remove Vehicle");
        removeVehicleBtn.setOnAction(e -> handleRemoveVehicle());

        // Return Vehicle button is now in the input panel

        Button transferVehicleBtn = createStyledButton("Transfer Vehicle");
        transferVehicleBtn.setOnAction(e -> handleTransferVehicle());

        Button importXmlBtn = createStyledButton("Import XML");
        importXmlBtn.setOnAction(e -> handleImportXML());

        Button enableAcquisitionBtn = createStyledButton("Enable Acquisition");
        enableAcquisitionBtn.setOnAction(e -> handleEnableAcquisition());

        Button disableAcquisitionBtn = createStyledButton("Disable Acquisition");
        disableAcquisitionBtn.setOnAction(e -> handleDisableAcquisition());

        Button exportInventoryBtn = createStyledButton("Export Inventory");
        exportInventoryBtn.setOnAction(e -> handleExportInventory());

        Button clearExportBtn = createStyledButton("Clear Export");
        clearExportBtn.setOnAction(e -> handleClearExport());

        buttonPanel.getChildren().addAll(
                addVehicleBtn, removeVehicleBtn, transferVehicleBtn,
                importXmlBtn, enableAcquisitionBtn, disableAcquisitionBtn,
                exportInventoryBtn, clearExportBtn
        );

        return buttonPanel;
    }

    /**
     * Creates a styled button with consistent look and feel
     */
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        button.setPrefHeight(35);
        button.setStyle("-fx-font-size: 14px;");
        return button;
    }

    /**
     * Sets up the dark mode toggle
     */
    private void setupDarkModeToggle(BorderPane mainLayout, Stage primaryStage) {
        // Create dark mode toggle button
        ToggleButton darkModeToggle = new ToggleButton("Dark Mode");
        darkModeToggle.setStyle("-fx-font-size: 12px;");

        // Place it in the top-right corner
        HBox topRightBox = new HBox(darkModeToggle);
        topRightBox.setAlignment(Pos.TOP_RIGHT);
        topRightBox.setPadding(new Insets(5, 10, 0, 0));

        // Get the existing header
        VBox headerPane = (VBox) mainLayout.getTop();

        // Create a new container with both header and toggle
        BorderPane headerContainer = new BorderPane();
        headerContainer.setCenter(headerPane);
        headerContainer.setRight(topRightBox);

        // Set the new combined header
        mainLayout.setTop(headerContainer);

        // Add toggle functionality
        darkModeToggle.setOnAction(e -> {
            darkModeEnabled = darkModeToggle.isSelected();
            applyTheme(mainLayout, primaryStage);
        });
    }

    /**
     * Apply theme changes based on dark mode toggle
     */
    private void applyTheme(BorderPane mainLayout, Stage primaryStage) {
        Scene scene = primaryStage.getScene();

        if (darkModeEnabled) {
            // Apply dark stylesheet to the entire scene
            scene.getStylesheets().add(getClass().getResource("/DarkMode.css").toExternalForm());

            // Update header color
            BorderPane headerContainer = (BorderPane) mainLayout.getTop();
            VBox headerPane = (VBox) headerContainer.getCenter();
            headerPane.setStyle("-fx-background-color: #006666;"); // Darker cyan
        } else {
            // Remove dark stylesheet
            scene.getStylesheets().remove(getClass().getResource("/DarkMode.css").toExternalForm());

            // Reset header color
            BorderPane headerContainer = (BorderPane) mainLayout.getTop();
            VBox headerPane = (VBox) headerContainer.getCenter();
            String colorHex = String.format("#%02X%02X%02X",
                    (int) (THEME_COLOR.getRed() * 255),
                    (int) (THEME_COLOR.getGreen() * 255),
                    (int) (THEME_COLOR.getBlue() * 255));
            headerPane.setStyle("-fx-background-color: " + colorHex + ";");
        }
    }

    /**
     * Creates and sets up the display area
     */
    private TextArea createDisplayArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(300);
        area.setFont(Font.font("Monospaced", 12));
        return area;
    }

    /**
     * Shows the dashboard dialog
     */
    /**
     * Shows the dashboard dialog
     */
    private void showDashboard() {
        // Create a new dashboard dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dealership Dashboard");
        dialog.setHeaderText("Vehicle Inventory Statistics");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Create dashboard layout
        VBox dashboardLayout = new VBox(20);
        dashboardLayout.setPadding(new Insets(20));
        dashboardLayout.setPrefWidth(800);
        dashboardLayout.setPrefHeight(500);

        // Get all vehicles for statistics
        List<Vehicle> allVehicles = manager.getVehiclesForDisplay();

        // Calculate statistics
        int totalVehicles = allVehicles.size();
        long rentedVehicles = allVehicles.stream().filter(Vehicle::isRented).count();
        long availableVehicles = totalVehicles - rentedVehicles;

        // Count by type
        Map<String, Integer> vehiclesByType = new HashMap<>();
        for (Vehicle vehicle : allVehicles) {
            String type = vehicle.getClass().getSimpleName();
            vehiclesByType.put(type, vehiclesByType.getOrDefault(type, 0) + 1);
        }

        // Count by dealer
        Map<String, Integer> vehiclesByDealer = new HashMap<>();
        for (Vehicle vehicle : allVehicles) {
            String dealerId = vehicle.getDealerId();
            vehiclesByDealer.put(dealerId, vehiclesByDealer.getOrDefault(dealerId, 0) + 1);
        }

        // Create summary labels
        Label summaryLabel = new Label("Inventory Summary");
        summaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label totalLabel = new Label("Total Vehicles: " + totalVehicles);
        totalLabel.setStyle("-fx-font-size: 14px;");

        Label rentedLabel = new Label("Currently Rented: " + rentedVehicles);
        rentedLabel.setStyle("-fx-font-size: 14px;");

        Label availableLabel = new Label("Available for Sale: " + availableVehicles);
        availableLabel.setStyle("-fx-font-size: 14px;");

        // Create type distribution section
        Label typeLabel = new Label("Vehicle Types");
        typeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create type chart (bar chart)
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> typeChart = new BarChart<>(xAxis, yAxis);
        typeChart.setTitle("Vehicles by Type");
        xAxis.setLabel("Vehicle Type");
        yAxis.setLabel("Count");

        XYChart.Series<String, Number> typeSeries = new XYChart.Series<>();
        typeSeries.setName("Count");
        for (Map.Entry<String, Integer> entry : vehiclesByType.entrySet()) {
            typeSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        typeChart.getData().add(typeSeries);
        typeChart.setPrefHeight(250);

        // Create dealer distribution section
        Label dealerLabel = new Label("Vehicles by Dealer");
        dealerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create a simple list view instead of a table
        ListView<String> dealerListView = new ListView<>();
        dealerListView.setPrefHeight(200);
        dealerListView.setMinHeight(150);  // Ensure minimum height

        // Create list items
        ObservableList<String> dealerItems = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : vehiclesByDealer.entrySet()) {
            dealerItems.add(String.format("Dealer ID: %s     Vehicle Count: %d",
                    entry.getKey(), entry.getValue()));
        }

        // Set items and add to layout
        dealerListView.setItems(dealerItems);
        dealerListView.setVisible(true);  // Ensure visibility
        dealerListView.setManaged(true);  // Ensure it's managed by layout

        // Debug output
        System.out.println("Dealer items count: " + dealerItems.size());
        for (String item : dealerItems) {
            System.out.println(item);
        }

        // Add all components to the dashboard
        dashboardLayout.getChildren().addAll(
                summaryLabel, totalLabel, rentedLabel, availableLabel,
                new Separator(), typeLabel, typeChart,
                new Separator(), dealerLabel, dealerListView
        );

        // Set the dialog content
        ScrollPane scrollPane = new ScrollPane(dashboardLayout);
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);

        // Show dialog
        dialog.showAndWait();
    }

    /**
     * Performs a search based on the query and search type
     */
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // If search is empty, show all vehicles
            refreshDisplay();
            return;
        }

        // Convert query to lowercase for case-insensitive search
        String searchQuery = query.toLowerCase().trim();

        // Get search type
        String searchType = searchTypeComboBox.getValue();

        // Get all vehicles
        List<Vehicle> allVehicles = manager.getVehiclesForDisplay();

        // Filter vehicles based on search criteria
        List<Vehicle> filteredVehicles = allVehicles.stream()
                .filter(vehicle -> matchesSearchCriteria(vehicle, searchType, searchQuery))
                .collect(Collectors.toList());

        // Display filtered vehicles
        displayFilteredVehicles(filteredVehicles);
    }

    /**
     * Helper method to check if a vehicle matches search criteria
     */
    private boolean matchesSearchCriteria(Vehicle vehicle, String searchType, String query) {
        switch (searchType) {
            case "ID":
                return vehicle.getVehicleId().toLowerCase().contains(query);
            case "Manufacturer":
                return vehicle.getManufacturer().toLowerCase().contains(query);
            case "Model":
                return vehicle.getModel().toLowerCase().contains(query);
            case "Dealer ID":
                return vehicle.getDealerId().toLowerCase().contains(query);
            case "Type":
                return vehicle.getClass().getSimpleName().toLowerCase().contains(query);
            case "All Fields":
            default:
                return vehicle.getVehicleId().toLowerCase().contains(query) ||
                        vehicle.getManufacturer().toLowerCase().contains(query) ||
                        vehicle.getModel().toLowerCase().contains(query) ||
                        vehicle.getDealerId().toLowerCase().contains(query) ||
                        vehicle.getClass().getSimpleName().toLowerCase().contains(query);
        }
    }

    /**
     * Display filtered vehicles in the display area
     */
    private void displayFilteredVehicles(List<Vehicle> vehicles) {
        StringBuilder sb = new StringBuilder("Search Results:\n\n");

        if (vehicles.isEmpty()) {
            sb.append("No vehicles match your search criteria.\n");
        } else {
            vehicles.forEach(vehicle -> {
                String rentalStatus = vehicle.isRented() ? "RENTED" : "AVAILABLE";
                if (vehicle instanceof SportsCar) {
                    rentalStatus = "NOT RENTABLE";
                }

                String dealerInfo = vehicle.getDealerId();
                if (vehicle.getMetadata().containsKey("dealer_name")) {
                    dealerInfo += " (" + vehicle.getMetadata().get("dealer_name") + ")";
                }

                sb.append(String.format("Type: %s, ID: %s, Manufacturer: %s, Model: %s, Price: $%.2f, Dealer: %s, Status: %s\n",
                        vehicle.getClass().getSimpleName(),
                        vehicle.getVehicleId(),
                        vehicle.getManufacturer(),
                        vehicle.getModel(),
                        vehicle.getPrice(),
                        dealerInfo,
                        rentalStatus));
            });
        }

        displayArea.setText(sb.toString());
    }

    /**
     * Loads initial inventory data from file if it exists
     */
    private void loadInitialInventory() {
        File initialFile = new File(INVENTORY_PATH);
        if (initialFile.exists()) {
            manager.readInventoryFile(initialFile);
            refreshDisplay();
            updateDealerDropdown();
        }
    }

    /**
     * Updates the dealer dropdown with current dealerships in the system
     */
    private void updateDealerDropdown() {
        // Get all unique dealer IDs from current inventory
        java.util.Set<String> dealerIds = new java.util.HashSet<>();

        for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
            dealerIds.add(vehicle.getDealerId());
        }

        // Update the combo box
        dealerIdComboBox.getItems().clear();
        dealerIdComboBox.getItems().add("-- New Dealer --");
        dealerIdComboBox.getItems().addAll(dealerIds);
    }

    /**
     * Updates the display area with current inventory information
     */
    private void refreshDisplay() {
        StringBuilder sb = new StringBuilder("Current Inventory:\n\n");

        manager.getVehiclesForDisplay().forEach(vehicle -> {
            String rentalStatus = vehicle.isRented() ? "RENTED" : "AVAILABLE";
            if (vehicle instanceof SportsCar) {
                rentalStatus = "NOT RENTABLE";
            }

            String dealerInfo = vehicle.getDealerId();
            if (vehicle.getMetadata().containsKey("dealer_name")) {
                dealerInfo += " (" + vehicle.getMetadata().get("dealer_name") + ")";
            }

            sb.append(String.format("Type: %s, ID: %s, Manufacturer: %s, Model: %s, Price: $%.2f, Dealer: %s, Status: %s\n",
                    vehicle.getClass().getSimpleName(),
                    vehicle.getVehicleId(),
                    vehicle.getManufacturer(),
                    vehicle.getModel(),
                    vehicle.getPrice(),
                    dealerInfo,
                    rentalStatus));
        });

        displayArea.setText(sb.toString());
    }

    /**
     * Validates all input fields before processing
     */
    private boolean validateFields() {
        StringBuilder errorMessage = new StringBuilder();

        // Check for dealer ID (either from dropdown or text field)
        String dealerId = getDealerId();
        if (dealerId.isEmpty()) {
            errorMessage.append("Dealer ID is required. Either select from dropdown or enter a new one.\n");
        }

        if (vehicleIdField.getText().trim().isEmpty()) {
            errorMessage.append("Vehicle ID is required.\n");
        }
        if (manufacturerField.getText().trim().isEmpty()) {
            errorMessage.append("Manufacturer is required.\n");
        }
        if (modelField.getText().trim().isEmpty()) {
            errorMessage.append("Model is required.\n");
        }
        if (priceField.getText().trim().isEmpty()) {
            errorMessage.append("Price is required.\n");
        }

        // Validate price format
        if (!priceField.getText().trim().isEmpty()) {
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) {
                    errorMessage.append("Price must be greater than 0!\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("Invalid price format! Enter a valid number.\n");
            }
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following errors:", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * Validates just dealer ID and vehicle ID fields
     */
    private boolean validateDealerAndVehicleIds() {
        StringBuilder errorMessage = new StringBuilder();

        String dealerId = getDealerId();
        if (dealerId.isEmpty()) {
            errorMessage.append("Dealer ID is required. Either select from dropdown or enter a new one.\n");
        }
        if (vehicleIdField.getText().trim().isEmpty()) {
            errorMessage.append("Vehicle ID is required.\n");
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following errors:", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * Displays an alert dialog with the given information
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Displays a success message
     */
    private void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", null, message);
    }

    /**
     * Displays an error message
     */
    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Error", null, message);
    }

    /**
     * Adds a message to the display area
     */
    private void showMessage(String message) {
        displayArea.appendText("\n>>> " + message + "\n");
    }

    /**
     * Returns the current dealer ID from either the dropdown or text field
     */
    private String getDealerId() {
        if (dealerIdComboBox.getValue() != null &&
                !dealerIdComboBox.getValue().isEmpty() &&
                !dealerIdComboBox.getValue().equals("-- New Dealer --")) {
            return dealerIdComboBox.getValue();
        } else {
            return dealerIdField.getText().trim();
        }
    }

    /**
     * Creates appropriate vehicle object based on selected type
     */
    private Vehicle createVehicleFromType() {
        return switch (vehicleTypeComboBox.getValue()) {
            case "SUV" -> new SUV();
            case "Sedan" -> new Sedan();
            case "Pickup" -> new Pickup();
            case "Sports Car" -> new SportsCar();
            default -> new SUV(); // Default to SUV if something goes wrong
        };
    }

    /**
     * Clears all input fields after successful operations
     */
    private void clearInputFields() {
        dealerIdField.clear();
        dealerIdField.setDisable(false);
        dealerIdComboBox.setValue(null);
        vehicleTypeComboBox.setValue("SUV");
        vehicleIdField.clear();
        manufacturerField.clear();
        modelField.clear();
        priceField.clear();
    }

    /**
     * Gets available vehicles for a specific dealer
     */
    private List<String> getAvailableVehiclesForDealer(String dealerId) {
        List<String> availableVehicles = new ArrayList<>();

        for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
            if (vehicle.getDealerId().equals(dealerId) &&
                    !(vehicle instanceof SportsCar) &&
                    !vehicle.isRented()) {
                availableVehicles.add(vehicle.getVehicleId() + " - " +
                        vehicle.getManufacturer() + " " +
                        vehicle.getModel());
            }
        }

        return availableVehicles;
    }

    /**
     * Gets rented vehicles for a specific dealer
     */
    private List<String> getRentedVehiclesForDealer(String dealerId) {
        List<String> rentedVehicles = new ArrayList<>();

        for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
            if (vehicle.getDealerId().equals(dealerId) && vehicle.isRented()) {
                rentedVehicles.add(vehicle.getVehicleId() + " - " +
                        vehicle.getManufacturer() + " " +
                        vehicle.getModel());
            }
        }

        return rentedVehicles;
    }

    /**
     * Gets the vehicle ID from a formatted vehicle string
     */
    private String getVehicleIdFromFormatted(String formattedVehicle) {
        if (formattedVehicle == null || formattedVehicle.isEmpty()) return "";
        int index = formattedVehicle.indexOf(" - ");
        if (index > 0) {
            return formattedVehicle.substring(0, index);
        }
        return formattedVehicle;
    }

    /**
     * Handles the addition of a new vehicle to the inventory
     */
    private void handleAddVehicle() {
        try {
            if (!validateFields()) return;

            String dealerId = getDealerId();
            Vehicle vehicle = createVehicleFromType();
            if (vehicle == null) return;

            vehicle.setVehicleId(vehicleIdField.getText().trim());
            vehicle.setManufacturer(manufacturerField.getText().trim());
            vehicle.setModel(modelField.getText().trim());
            vehicle.setPrice(Double.parseDouble(priceField.getText().trim()));
            vehicle.setAcquisitionDate(new Date());
            vehicle.setDealerId(dealerId);

            File inventoryFile = new File(INVENTORY_PATH);
            if (manager.addVehicleToInventory(vehicle, inventoryFile)) {
                refreshDisplay();
                updateDealerDropdown(); // Update dealer dropdown after adding
                clearInputFields();
                showSuccess("Vehicle added to inventory successfully!");
            } else {
                showError("Cannot add vehicle - Acquisition is disabled for dealer " + dealerId);
            }
        } catch (Exception ex) {
            showError("Error adding vehicle: " + ex.getMessage());
        }
    }

    /**
     * Handles the removal of a vehicle from the inventory
     */
    private void handleRemoveVehicle() {
        try {
            // Create a custom dialog for removing vehicles
            Dialog<RemoveVehicleInfo> dialog = new Dialog<>();
            dialog.setTitle("Remove Vehicle");
            dialog.setHeaderText("Select a vehicle to remove");

            // Set the button types
            ButtonType removeButtonType = new ButtonType("Remove Vehicle", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(removeButtonType, ButtonType.CANCEL);

            // Create the selection fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create dealer dropdown
            ComboBox<String> dealerCombo = new ComboBox<>();
            dealerCombo.setPromptText("Select a dealer");

            // Find all dealers that have vehicles
            Set<String> dealersWithVehicles = new HashSet<>();
            for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
                dealersWithVehicles.add(vehicle.getDealerId());
            }

            // If no vehicles, show a message and return
            if (dealersWithVehicles.isEmpty()) {
                showError("No vehicles in inventory");
                return;
            }

            dealerCombo.getItems().addAll(dealersWithVehicles);

            // Create vehicle dropdown (initially empty)
            ComboBox<String> vehicleCombo = new ComboBox<>();
            vehicleCombo.setPromptText("Select a vehicle");

            // Update vehicle dropdown when dealer changes
            dealerCombo.setOnAction(e -> {
                String selectedDealer = dealerCombo.getValue();
                if (selectedDealer != null) {
                    vehicleCombo.getItems().clear();
                    vehicleCombo.getItems().addAll(getVehiclesForDealer(selectedDealer));
                }
            });

            // Additional input fields
            TextField manufacturerField = new TextField();
            manufacturerField.setPromptText("Manufacturer");
            manufacturerField.setEditable(false);
            TextField modelField = new TextField();
            modelField.setPromptText("Model");
            modelField.setEditable(false);
            TextField priceField = new TextField();
            priceField.setPromptText("Price");
            priceField.setEditable(false);

            // Populate additional fields when vehicle is selected
            vehicleCombo.setOnAction(e -> {
                String selectedVehicle = vehicleCombo.getValue();
                if (selectedVehicle != null) {
                    String vehicleId = getVehicleIdFromFormatted(selectedVehicle);
                    Vehicle vehicle = findVehicleById(dealerCombo.getValue(), vehicleId);
                    if (vehicle != null) {
                        manufacturerField.setText(vehicle.getManufacturer());
                        modelField.setText(vehicle.getModel());
                        priceField.setText(String.format("%.2f", vehicle.getPrice()));
                    }
                }
            });

            grid.add(new Label("Dealer:"), 0, 0);
            grid.add(dealerCombo, 1, 0);
            grid.add(new Label("Vehicle:"), 0, 1);
            grid.add(vehicleCombo, 1, 1);
            grid.add(new Label("Manufacturer:"), 0, 2);
            grid.add(manufacturerField, 1, 2);
            grid.add(new Label("Model:"), 0, 3);
            grid.add(modelField, 1, 3);
            grid.add(new Label("Price:"), 0, 4);
            grid.add(priceField, 1, 4);

            dialog.getDialogPane().setContent(grid);

            // Apply dark mode styling if needed
            if (darkModeEnabled) {
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/DarkMode.css").toExternalForm());
            }

            // Convert the result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == removeButtonType) {
                    try {
                        return new RemoveVehicleInfo(
                                dealerCombo.getValue(),
                                getVehicleIdFromFormatted(vehicleCombo.getValue()),
                                manufacturerField.getText(),
                                modelField.getText(),
                                Double.parseDouble(priceField.getText())
                        );
                    } catch (NumberFormatException | NullPointerException ex) {
                        showError("Invalid input. Please select a vehicle.");
                        return null;
                    }
                }
                return null;
            });

            Optional<RemoveVehicleInfo> result = dialog.showAndWait();

            result.ifPresent(info -> {
                if (info.dealerId == null || info.vehicleId == null) {
                    showError("Dealer and Vehicle must be selected");
                    return;
                }

                File inventoryFile = new File(INVENTORY_PATH);
                boolean success = manager.removeVehicleFromInventory(
                        info.dealerId,
                        info.vehicleId,
                        info.manufacturer,
                        info.model,
                        info.price,
                        inventoryFile
                );

                if (success) {
                    refreshDisplay();
                    clearInputFields();
                    updateDealerDropdown();
                    showSuccess("Vehicle removed successfully");
                } else {
                    showError("Failed to remove vehicle. Vehicle may be rented or not found.");
                }
            });
        } catch (Exception ex) {
            showError("Error removing vehicle: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Handles renting a vehicle with a simpler data structure
     */
    private void handleRentVehicle() {
        try {
            // Create a custom dialog for rental
            Dialog<RentalInfo> dialog = new Dialog<>();
            dialog.setTitle("Rent Vehicle");
            dialog.setHeaderText("Select vehicle and rental period");

            // Set the button types
            ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

            // Create the selection fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create dealer dropdown
            ComboBox<String> rentalDealerCombo = new ComboBox<>();
            rentalDealerCombo.setPromptText("Select a dealer");
            java.util.Set<String> dealerIds = new java.util.HashSet<>();

            for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
                dealerIds.add(vehicle.getDealerId());
            }
            rentalDealerCombo.getItems().addAll(dealerIds);

            // Create vehicle dropdown (initially empty)
            ComboBox<String> vehicleCombo = new ComboBox<>();
            vehicleCombo.setPromptText("Select a vehicle");

            // Update vehicle dropdown when dealer changes
            rentalDealerCombo.setOnAction(e -> {
                String selectedDealer = rentalDealerCombo.getValue();
                if (selectedDealer != null) {
                    vehicleCombo.getItems().clear();
                    vehicleCombo.getItems().addAll(getAvailableVehiclesForDealer(selectedDealer));
                }
            });

            // Date fields
            TextField startDateField = new TextField();
            startDateField.setPromptText("MM/DD/YYYY");
            TextField endDateField = new TextField();
            endDateField.setPromptText("MM/DD/YYYY");

            grid.add(new Label("Dealer:"), 0, 0);
            grid.add(rentalDealerCombo, 1, 0);
            grid.add(new Label("Vehicle:"), 0, 1);
            grid.add(vehicleCombo, 1, 1);
            grid.add(new Label("Start Date:"), 0, 2);
            grid.add(startDateField, 1, 2);
            grid.add(new Label("End Date:"), 0, 3);
            grid.add(endDateField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Convert the result using a custom class instead of nested Pairs
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    RentalInfo info = new RentalInfo();
                    info.dealerId = rentalDealerCombo.getValue();
                    info.vehicleId = getVehicleIdFromFormatted(vehicleCombo.getValue());
                    info.startDate = startDateField.getText();
                    info.endDate = endDateField.getText();
                    return info;
                }
                return null;
            });

            Optional<RentalInfo> result = dialog.showAndWait();

            result.ifPresent(info -> {
                if (info.dealerId == null || info.vehicleId == null ||
                        info.startDate.isEmpty() || info.endDate.isEmpty()) {
                    showError("All fields are required");
                    return;
                }

                File inventoryFile = new File(INVENTORY_PATH);
                boolean success = manager.rentVehicle(
                        info.dealerId, info.vehicleId, info.startDate, info.endDate, inventoryFile);

                if (success) {
                    refreshDisplay();
                    showSuccess("Vehicle rented successfully");
                } else {
                    showError("Failed to rent vehicle. Vehicle may be already rented or not found.");
                }
            });
        } catch (Exception ex) {
            showError("Error renting vehicle: " + ex.getMessage());
        }
    }

    /**
     * Handles returning a rented vehicle
     */
    private void handleReturnVehicle() {
        try {
            // Create a custom dialog for returning vehicles
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Return Vehicle");
            dialog.setHeaderText("Select a rented vehicle to return");

            // Set the button types
            ButtonType confirmButtonType = new ButtonType("Return Vehicle", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

            // Get the return button and style it
            Button returnButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
            returnButton.setStyle("-fx-background-color: #E0A0A0;"); // Light red color

            // Create the selection fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create dealer dropdown
            ComboBox<String> dealerCombo = new ComboBox<>();
            dealerCombo.setPromptText("Select a dealer");

            // Find all dealers that have rented vehicles
            Set<String> dealersWithRentedVehicles = new HashSet<>();
            for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
                if (vehicle.isRented()) {
                    dealersWithRentedVehicles.add(vehicle.getDealerId());
                }
            }

            // If no rented vehicles, show a message and return
            if (dealersWithRentedVehicles.isEmpty()) {
                showError("No vehicles are currently rented");
                return;
            }

            dealerCombo.getItems().addAll(dealersWithRentedVehicles);

            // Create vehicle dropdown (initially empty)
            ComboBox<String> vehicleCombo = new ComboBox<>();
            vehicleCombo.setPromptText("Select a rented vehicle");

            // Update vehicle dropdown when dealer changes
            dealerCombo.setOnAction(e -> {
                String selectedDealer = dealerCombo.getValue();
                if (selectedDealer != null) {
                    vehicleCombo.getItems().clear();
                    vehicleCombo.getItems().addAll(getRentedVehiclesForDealer(selectedDealer));
                }
            });

            grid.add(new Label("Dealer:"), 0, 0);
            grid.add(dealerCombo, 1, 0);
            grid.add(new Label("Vehicle:"), 0, 1);
            grid.add(vehicleCombo, 1, 1);

            dialog.getDialogPane().setContent(grid);

            // Convert the result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    return getVehicleIdFromFormatted(vehicleCombo.getValue());
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(vehicleId -> {
                if (dealerCombo.getValue() == null || vehicleId == null || vehicleId.isEmpty()) {
                    showError("Both dealer and vehicle must be selected");
                    return;
                }

                String dealerId = dealerCombo.getValue();
                File inventoryFile = new File(INVENTORY_PATH);
                boolean success = manager.returnVehicle(dealerId, vehicleId, inventoryFile);

                if (success) {
                    refreshDisplay();
                    showSuccess("Vehicle returned successfully");
                } else {
                    showError("Failed to return vehicle. An unexpected error occurred.");
                }
            });
        } catch (Exception ex) {
            showError("Error returning vehicle: " + ex.getMessage());
        }
    }

    /**
     * Handles transferring a vehicle between dealerships
     */
    private void handleTransferVehicle() {
        try {
            // Create a custom dialog for vehicle transfer
            Dialog<TransferVehicleInfo> dialog = new Dialog<>();
            dialog.setTitle("Transfer Vehicle");
            dialog.setHeaderText("Transfer a vehicle between dealerships");

            // Set the button types
            ButtonType transferButtonType = new ButtonType("Transfer Vehicle", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(transferButtonType, ButtonType.CANCEL);

            // Create the selection fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Source dealer dropdown
            ComboBox<String> sourceDealerCombo = new ComboBox<>();
            sourceDealerCombo.setPromptText("Select Source Dealer");

            // Find all dealers that have vehicles
            Set<String> dealersWithVehicles = new HashSet<>();
            for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
                dealersWithVehicles.add(vehicle.getDealerId());
            }

            // If no vehicles, show a message and return
            if (dealersWithVehicles.isEmpty()) {
                showError("No vehicles in inventory");
                return;
            }

            sourceDealerCombo.getItems().addAll(dealersWithVehicles);

            // Vehicle dropdown for source dealer (initially empty)
            ComboBox<String> vehicleCombo = new ComboBox<>();
            vehicleCombo.setPromptText("Select a Vehicle");

            // Target dealer section
            Label targetDealerLabel = new Label("Target Dealer:");
            ComboBox<String> targetDealerCombo = new ComboBox<>();
            targetDealerCombo.setPromptText("Select Existing Dealer");
            TextField newDealerField = new TextField();
            newDealerField.setPromptText("Or Enter New Dealer ID");

            // Populate existing dealers in target dealer dropdown
            targetDealerCombo.getItems().addAll(dealersWithVehicles);
            targetDealerCombo.getItems().add("-- New Dealer --");

            // Listener to handle new dealer option
            targetDealerCombo.setOnAction(e -> {
                if ("-- New Dealer --".equals(targetDealerCombo.getValue())) {
                    newDealerField.setDisable(false);
                } else {
                    newDealerField.clear();
                    newDealerField.setDisable(true);
                }
            });

            // Update vehicle dropdown when source dealer changes
            sourceDealerCombo.setOnAction(e -> {
                String selectedDealer = sourceDealerCombo.getValue();
                if (selectedDealer != null) {
                    vehicleCombo.getItems().clear();
                    vehicleCombo.getItems().addAll(getAvailableVehiclesForDealer(selectedDealer));

                    // Update target dealer dropdown - remove the source dealer
                    targetDealerCombo.getItems().clear();
                    dealersWithVehicles.stream()
                            .filter(dealer -> !dealer.equals(selectedDealer))
                            .forEach(dealer -> targetDealerCombo.getItems().add(dealer));
                    targetDealerCombo.getItems().add("-- New Dealer --");
                }
            });

            grid.add(new Label("Source Dealer:"), 0, 0);
            grid.add(sourceDealerCombo, 1, 0);
            grid.add(new Label("Vehicle:"), 0, 1);
            grid.add(vehicleCombo, 1, 1);
            grid.add(targetDealerLabel, 0, 2);
            grid.add(targetDealerCombo, 1, 2);
            grid.add(new Label("New Dealer ID:"), 0, 3);
            grid.add(newDealerField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Disable new dealer field initially
            newDealerField.setDisable(true);

            // Apply dark mode styling if needed
            if (darkModeEnabled) {
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/DarkMode.css").toExternalForm());
            }

            // Convert the result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == transferButtonType) {
                    String targetDealerId = targetDealerCombo.getValue();
                    if ("-- New Dealer --".equals(targetDealerId)) {
                        targetDealerId = newDealerField.getText().trim();
                        if (targetDealerId.isEmpty()) {
                            showError("Please enter a dealer ID for the new dealer");
                            return null;
                        }
                    }

                    String vehicleId = getVehicleIdFromFormatted(vehicleCombo.getValue());
                    if (vehicleId == null) {
                        showError("Please select a vehicle to transfer");
                        return null;
                    }

                    return new TransferVehicleInfo(
                            sourceDealerCombo.getValue(),
                            targetDealerId,
                            vehicleId
                    );
                }
                return null;
            });

            Optional<TransferVehicleInfo> result = dialog.showAndWait();

            result.ifPresent(info -> {
                if (info.sourceDealerId == null || info.targetDealerId == null || info.vehicleId == null) {
                    showError("All fields must be filled");
                    return;
                }

                if (info.sourceDealerId.equals(info.targetDealerId)) {
                    showError("Source and target dealers cannot be the same");
                    return;
                }

                // Enable acquisition for target dealer if it's new
                if (!dealersWithVehicles.contains(info.targetDealerId)) {
                    manager.enableAcquisition(info.targetDealerId);
                }

                File inventoryFile = new File(INVENTORY_PATH);
                boolean success = manager.transferVehicle(
                        info.sourceDealerId,
                        info.targetDealerId,
                        info.vehicleId,
                        inventoryFile
                );

                if (success) {
                    refreshDisplay();
                    updateDealerDropdown();
                    showSuccess("Vehicle transferred successfully");
                } else {
                    showError("Failed to transfer vehicle. Check dealer IDs and vehicle status.");
                }
            });
        } catch (Exception ex) {
            showError("Error transferring vehicle: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Handles importing vehicles from XML file
     */
    private void handleImportXML() {
        try {
            fileChooser.setTitle("Open XML File");
            File selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                File inventoryFile = new File(INVENTORY_PATH);
                int importCount = manager.importXMLFile(selectedFile, inventoryFile);

                if (importCount > 0) {
                    refreshDisplay();
                    showSuccess("Successfully imported " + importCount + " vehicles from XML");
                } else {
                    showMessage("No vehicles were imported from XML");
                }
            }
        } catch (Exception ex) {
            showError("Error importing XML: " + ex.getMessage());
        }
    }

    /**
     * Handles enabling acquisition for a dealer
     */
    private void handleEnableAcquisition() {
        String dealerId = getDealerId();
        if (dealerId.isEmpty()) {
            showError("Dealer ID is required to enable acquisition!");
            return;
        }

        if (manager.enableAcquisition(dealerId)) {
            showSuccess("Acquisition enabled for dealer: " + dealerId);
        }
    }

    /**
     * Handles disabling acquisition for a dealer
     */
    private void handleDisableAcquisition() {
        String dealerId = getDealerId();
        if (dealerId.isEmpty()) {
            showError("Dealer ID is required to disable acquisition!");
            return;
        }

        if (manager.disableAcquisition(dealerId)) {
            showSuccess("Acquisition disabled for dealer: " + dealerId);
        }
    }

    /**
     * Handles exporting current inventory to export.json
     */
    private void handleExportInventory() {
        File inventoryFile = new File(INVENTORY_PATH);
        File exportFile = new File(EXPORT_PATH);

        if (!inventoryFile.exists()) {
            showError("inventory.json not found!");
            return;
        }

        try {
            if (manager.exportInventoryToExport(inventoryFile, exportFile)) {
                showSuccess("Successfully exported to export.json");
            } else {
                showError("Failed to export: No vehicles found in inventory");
            }
        } catch (Exception e) {
            showError("Error during export: " + e.getMessage());
        }
    }

    /**
     * Handles clearing the export.json file
     */
    private void handleClearExport() {
        try {
            manager.clearExportFile(new File(EXPORT_PATH));
            showSuccess("export.json has been cleared");
        } catch (Exception e) {
            showError("Error clearing export.json: " + e.getMessage());
        }
    }

    /**
     * Gets list of formatted vehicle strings for a dealer
     * @param dealerId The dealer ID
     * @return List of formatted vehicle strings
     */
    private List<String> getVehiclesForDealer(String dealerId) {
        List<String> formattedVehicles = new ArrayList<>();
        for (Vehicle vehicle : manager.getVehiclesForDisplay()) {
            if (vehicle.getDealerId().equals(dealerId)) {
                String formatted = String.format("%s - %s %s ($%.2f)",
                        vehicle.getVehicleId(),
                        vehicle.getManufacturer(),
                        vehicle.getModel(),
                        vehicle.getPrice());
                formattedVehicles.add(formatted);
            }
        }
        return formattedVehicles;
    }

    /**
     * Finds a vehicle by dealer ID and vehicle ID
     */
    private Vehicle findVehicleById(String dealerId, String vehicleId) {
        return manager.getVehiclesForDisplay().stream()
                .filter(vehicle -> vehicle.getDealerId().equals(dealerId) &&
                        vehicle.getVehicleId().equals(vehicleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper class to hold rental information
     */
    private static class RentalInfo {
        public String dealerId;
        public String vehicleId;
        public String startDate;
        public String endDate;
    }

    /**
     * Helper class to hold remove vehicle information
     */
    private static class RemoveVehicleInfo {
        public final String dealerId;
        public final String vehicleId;
        public final String manufacturer;
        public final String model;
        public final double price;

        public RemoveVehicleInfo(String dealerId, String vehicleId, String manufacturer, String model, double price) {
            this.dealerId = dealerId;
            this.vehicleId = vehicleId;
            this.manufacturer = manufacturer;
            this.model = model;
            this.price = price;
        }
    }

    /**
     * Helper class to hold transfer vehicle information
     */
    private static class TransferVehicleInfo {
        public final String sourceDealerId;
        public final String targetDealerId;
        public final String vehicleId;

        public TransferVehicleInfo(String sourceDealerId, String targetDealerId, String vehicleId) {
            this.sourceDealerId = sourceDealerId;
            this.targetDealerId = targetDealerId;
            this.vehicleId = vehicleId;
        }
    }
}