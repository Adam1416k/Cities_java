import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;

public class PathFinder extends Application {
    private static final ButtonType CANCEL_BUTTON = new ButtonType("Cancel");
    private static final ButtonType OK_BUTTON = new ButtonType("OK");
    private Scene scene;
    private boolean unsavedChanges = false;
    private String currentFilePath = null;
    private Button findPathButton;
    private Button showConnectionButton;
    private Button newPlaceButton;
    private Button newConnectionButton;
    private Button changeConnectionButton;
    private List<Edge<T>> connections = new ArrayList<>();
    private List<T> selectedPlaces = new ArrayList<>();
    private ListGraph<T> graph = new ListGraph<>();
    private Map<String, T> nameToNodeMap = new HashMap<>();
    private Set<String> existingConnections = new HashSet<>();
    private Group drawingGroup;

    @Override
    public void start(Stage primaryStage) {
        Menu fileMenu = new Menu("File");
        fileMenu.setId("menuFile");

        MenuItem newMap = new MenuItem("New Map");
        newMap.setOnAction(e -> loadBackgroundImage("europa.gif"));
        newMap.setId("menuNewMap");

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> openFile(primaryStage));
        openItem.setId("menuOpenFile");

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> saveToFile("europa.graph"));
        saveItem.setId("menuSaveFile");

        MenuItem saveImage = new MenuItem("Save Image");
        saveImage.setOnAction(e -> saveImage());
        saveImage.setId("menuSaveImage");

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> exitApplication(primaryStage));
        exitItem.setId("menuExit");

        fileMenu.getItems().addAll(newMap, openItem, saveItem, saveImage, exitItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(fileMenu);
        menuBar.setId("menu");

        findPathButton = new Button("Find Path");
        findPathButton.setOnAction(e -> findPath());
        findPathButton.setId("btnFindPath");

        showConnectionButton = new Button("Show Connection");
        showConnectionButton.setOnAction(e -> {
            showConnectionDetails();
        });
        showConnectionButton.setId("btnShowConnection");

        newPlaceButton = new Button("New Place");
        newPlaceButton.setOnAction(e -> newPlaceCreate());
        newPlaceButton.setId("btnNewPlace");

        newConnectionButton = new Button("New Connection");
        newConnectionButton.setOnAction(e -> {
            if (selectedPlaces.size() != 2) {
                showAlert("Two places must be selected!");
                return;
            }
        
            T fromPlace = selectedPlaces.get(0);
            T toPlace = selectedPlaces.get(1);
        
            boolean connectionExists = connections.stream().anyMatch(connection ->
                connection.getFrom().equals(fromPlace) && connection.getDestination().equals(toPlace)
                || connection.getFrom().equals(toPlace) && connection.getDestination().equals(fromPlace));
        
            if (connectionExists) {
                showAlert("A connection already exists between the selected places");
                return;
            }
        
            Pair<String, Integer> result = getConnectionDetails(fromPlace, toPlace);
        
            if (result != null) {
                String errorMessage = validateConnectionDetails(result);
        
                if (!errorMessage.isEmpty()) {
                    showAlert(errorMessage.trim());
                } else {
                    drawConnection(fromPlace, toPlace, result);
                }
            }
            newConnectionButton.setDisable(false);
        });        
        newConnectionButton.setId("btnNewConnection");

        changeConnectionButton = new Button("Change Connection");
        changeConnectionButton.setOnAction(e -> {
            if (!validatePlacesSelection()) {
                showAlert("Two places must be selected to change a connection");
                return;
            }
        
            T fromPlace = selectedPlaces.get(0);
            T toPlace = selectedPlaces.get(1);
        
            Optional<Edge<T>> connection = getConnectionBetweenPlaces(fromPlace, toPlace);
            if (!connection.isPresent()) {
                showAlert("No connection between the selected places");
                return;
            }
        
            Integer newWeight = showConnectionDialog(connection);
            if (newWeight != null) {
                updateConnection(connection, newWeight);
            }
        });
        changeConnectionButton.setId("btnChangeConnection");

        findPathButton.setDisable(true);
        showConnectionButton.setDisable(true);
        newPlaceButton.setDisable(true);
        newConnectionButton.setDisable(true);
        changeConnectionButton.setDisable(true);

        HBox buttonContainer = new HBox(10);
        buttonContainer.getChildren().addAll(
                findPathButton, showConnectionButton, newPlaceButton, newConnectionButton, changeConnectionButton
        );

        HBox buttonContainerWrapper = new HBox(buttonContainer);
        HBox.setHgrow(buttonContainerWrapper, Priority.ALWAYS);
        buttonContainerWrapper.setAlignment(Pos.CENTER);

        VBox root = new VBox();
        root.getChildren().addAll(menuBar, buttonContainerWrapper);

        drawingGroup = new Group();
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(drawingGroup);
        borderPane.setTop(root);
        borderPane.setId("outputArea");

        scene = new Scene(borderPane);
        primaryStage.setTitle("PathFinder");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            exitApplication(primaryStage);
        });
    }

    private void loadBackgroundImage(String imageUrl) {
        clearSelected();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        File imageFile = new File(imageUrl);

        if (!imageFile.exists()) {
            return;
        }

        try {
            Image backgroundImage = new Image(imageFile.toURI().toString());
            ImageView backgroundImageView = new ImageView(backgroundImage);

            drawingGroup.getChildren().clear();
            drawingGroup.getChildren().add(0, backgroundImageView);

            if (((BorderPane) scene.getRoot()).getCenter() != drawingGroup) {
                ((BorderPane) scene.getRoot()).setCenter(drawingGroup);
            }

            Screen screen = Screen.getPrimary();
            double screenHeight = screen.getBounds().getHeight();

            Stage primaryStage = (Stage) scene.getWindow();
            primaryStage.setWidth(backgroundImage.getWidth());
            primaryStage.setHeight(screenHeight);

            primaryStage.setY(0);

            findPathButton.setDisable(false);
            showConnectionButton.setDisable(false);
            newPlaceButton.setDisable(false);
            newConnectionButton.setDisable(false);
            changeConnectionButton.setDisable(false);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        unsavedChanges = true;
    }
    
    private Optional<ButtonType> showConfirmation(String header, String message) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, message, OK_BUTTON, CANCEL_BUTTON);
        confirmationAlert.setTitle("Warning!");
        confirmationAlert.setHeaderText(header);
        return confirmationAlert.showAndWait();
    }

    private void clearSelected() {
        selectedPlaces.clear();
    }

    private void resetData() {
        connections.clear();
        selectedPlaces.clear();
        graph.clear();
        nameToNodeMap.clear();
        existingConnections.clear();
    }    
    
    private void openFile(Stage primaryStage) {
        clearSelected();
        if (unsavedChanges) {
            Optional<ButtonType> result = showConfirmation("Unsaved changes", "Continue anyway?");
            if (result.isPresent() && result.get() == OK_BUTTON) {
                drawingGroup.getChildren().clear();
                resetData();
                updateFile(primaryStage);
            } else {
                return;
            }
        } else {
            drawingGroup.getChildren().clear();
            updateFile(primaryStage);
        }
        unsavedChanges = true;
    }     

    private void updateFile(Stage primaryStage) {
        File selectedFile = new File("europa.graph");
    
        if (selectedFile != null) {
            drawingGroup.getChildren().clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                String file = reader.readLine();
                if (file == null) return;
    
                file = file.trim();
                ImageView backgroundImageView = createBackgroundImageView(file);
                if (backgroundImageView != null) drawingGroup.getChildren().add(0, backgroundImageView);
    
                readAndCreateNodes(reader);
                readAndCreateEdges(reader);
                configureScene();
    
                currentFilePath = selectedFile.getAbsolutePath();
                unsavedChanges = false;
    
                if (backgroundImageView != null) {
                    Image backgroundImage = backgroundImageView.getImage();
                    Screen screen = Screen.getPrimary();
                    double screenHeight = screen.getBounds().getHeight();
                    primaryStage.setWidth(backgroundImage.getWidth());
                    primaryStage.setHeight(screenHeight);
                    primaryStage.setY(0);
                }
    
            } catch (FileNotFoundException fileNotFound) {
                showAlert("The File Does Not Exist!");
            } catch (IOException ex) {
                showAlert("An error occurred while reading the file");
                ex.printStackTrace();
            }
        }
    }                
    
    private void readAndCreateNodes(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        String[] cityParts = line.split(";");
        for (int i = 0; i < cityParts.length; i += 3) {
            String placeName = cityParts[i].trim();
            double x = Double.parseDouble(cityParts[i + 1].trim());
            double y = Double.parseDouble(cityParts[i + 2].trim());
    
            T newT = new T(placeName, x, y);
            graph.add(newT);
            nameToNodeMap.put(placeName, newT);
    
            Circle placeCircle = new Circle(x, y, 10, Color.BLUE);
            Text placeText = new Text(x + 10, y, placeName);
            drawingGroup.getChildren().addAll(placeCircle, placeText);
            placeCircle.setId(placeName);
    
            placeCircle.setOnMouseClicked(event -> handlePlaceSelection(newT, placeCircle));
        }
    }    
    
    private void readAndCreateEdges(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            if (parts.length < 4) continue;
    
            T place1 = nameToNodeMap.get(parts[0].trim());
            T place2 = nameToNodeMap.get(parts[1].trim());
    
            if (place1 != null && place2 != null) {
                String connectionIdentifier = createConnectionIdentifier(place1.getName(), place2.getName());
                if (existingConnections.contains(connectionIdentifier)) continue;
    
                existingConnections.add(connectionIdentifier);
                String name = parts[2].trim();
                int weight = Integer.parseInt(parts[3].trim());
    
                Edge<T> edge = new Edge<>(place1, place2, name, weight);
                graph.connect(place1, place2, name, weight);
                connections.add(edge);
    
                Line connectionLine = new Line(place1.getX(), place1.getY(), place2.getX(), place2.getY());
                connectionLine.setStroke(Color.BLACK);
                connectionLine.setStrokeWidth(3);
                drawingGroup.getChildren().add(connectionLine);
            }
        }
    }    

    private String createConnectionIdentifier(String place1, String place2) {
        return place1.compareTo(place2) < 0 ? place1 + "->" + place2 : place2 + "->" + place1;
    }
    
    private void configureScene() {
        ((BorderPane) scene.getRoot()).setCenter(drawingGroup);
        scene.getWindow().sizeToScene();
        findPathButton.setDisable(false);
        showConnectionButton.setDisable(false);
        newPlaceButton.setDisable(false);
        newConnectionButton.setDisable(false);
        changeConnectionButton.setDisable(false);
    }          

    private ImageView createBackgroundImageView(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            Image backgroundImage = new Image(imageUrl);
            ImageView backgroundImageView = new ImageView(backgroundImage);
            return backgroundImageView;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            Object center = ((BorderPane) scene.getRoot()).getCenter();

            if (center instanceof ImageView) {
                ImageView backgroundImageView = (ImageView) center;
                if (backgroundImageView.getImage() != null) {
                    writer.write(backgroundImageView.getImage().getUrl());
                    writer.newLine();
                }
            } else if (center instanceof Group && !((Group) center).getChildren().isEmpty() && ((Group) center).getChildren().get(0) instanceof ImageView) {
                ImageView backgroundImageView = (ImageView) ((Group) center).getChildren().get(0);
                if (backgroundImageView.getImage() != null) {
                    writer.write(backgroundImageView.getImage().getUrl());
                    writer.newLine();
                }
            }
    
            StringBuilder node = new StringBuilder();
            for (T place : graph.getNodes()) {
                node.append(place.getName()).append(";")
                    .append(place.getX()).append(";")
                    .append(place.getY()).append(";");
            }

            if (node.length() > 0) {
                node.setLength(node.length() - 1);
            }
            writer.write(node.toString());
            writer.newLine();
    
            for (Edge<T> edge : connections) {
                writer.write(edge.getFrom().getName() + ";"
                    + edge.getDestination().getName() + ";"
                    + edge.getName() + ";"
                    + edge.getWeight());
                writer.newLine();
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        unsavedChanges = false;
    }
    
    private void saveImage() {
        WritableImage image = scene.snapshot(null);
    
        File file = new File("capture.png");
        
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void exitApplication(Stage stage) {
        if (unsavedChanges) {
            Alert unsavedChangesAlert = new Alert(Alert.AlertType.CONFIRMATION);
            unsavedChangesAlert.setTitle("Warning!");
            unsavedChangesAlert.setHeaderText("Unsaved changes, exit anyway?");
    
            ButtonType okButton = new ButtonType("OK");
            ButtonType cancelButton = new ButtonType("Cancel");
    
            unsavedChangesAlert.getButtonTypes().setAll(okButton, cancelButton);
    
            Optional<ButtonType> result = unsavedChangesAlert.showAndWait();
    
            if (result.isPresent() && result.get() == okButton) {
                stage.close();
            }
        } else {
            stage.close();
        }
    }

    private void newPlaceCreate() {
        scene.setCursor(Cursor.CROSSHAIR);
        newPlaceButton.setDisable(true);
        
        scene.setOnMouseClicked(mapClickEvent -> {
            createNewPlace(mapClickEvent.getX(), mapClickEvent.getY());
            scene.setOnMouseClicked(null);
        });
    }

    private void createNewPlace(double x, double y) {
        Point2D point = drawingGroup.sceneToLocal(x, y);
        x = point.getX();
        y = point.getY();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Name");
        dialog.setHeaderText("");
        dialog.setContentText("Name of place:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String placeName = result.get();

            if (!nameToNodeMap.containsKey(placeName)) {
                T newPlace = new T(placeName, x, y);
                graph.add(newPlace);
                nameToNodeMap.put(placeName, newPlace);

                Circle placeCircle = new Circle(x, y, 10, Color.BLUE);
                Text placeText = new Text(x + 10, y, placeName);
                
                placeCircle.setId(placeName);
                placeText.setId("text_" + placeName);

                drawingGroup.getChildren().addAll(placeCircle, placeText);

                placeCircle.setOnMouseClicked(event -> {
                    handlePlaceSelection(newPlace, placeCircle);
                });

                unsavedChanges = true;
            } else {
                showAlert("A place with this name already exists!");
            }
        }
        scene.setCursor(Cursor.DEFAULT);
        newPlaceButton.setDisable(false);
    }        

    private void handlePlaceSelection(T place, Circle circle) {
        if (selectedPlaces.contains(place)) {
            circle.setFill(Color.BLUE);
            selectedPlaces.remove(place);
        } else {
            if (selectedPlaces.size() < 2) {
                circle.setFill(Color.RED);
                selectedPlaces.add(place);
            }
        }
    }
    
    private Pair<String, Integer> getConnectionDetails(T fromPlace, T toPlace) {
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Connection");
        dialog.setHeaderText("Connection from " + fromPlace.getName() + " to " + toPlace.getName());
    
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
    
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField weightField = new TextField();
        weightField.setPromptText("Time");
    
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(weightField, 1, 1);
        dialog.getDialogPane().setContent(grid);
    
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    return new Pair<>(nameField.getText(), Integer.parseInt(weightField.getText()));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });
    
        Optional<Pair<String, Integer>> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    private String validateConnectionDetails(Pair<String, Integer> details) {
        String errorMessage = "";
        if (details.getKey() == null || details.getKey().isEmpty() || !details.getKey().matches("^[a-zA-Z]+$")) {
            errorMessage += "The name field should contain only letters\n";
        }
        if (details.getValue() == null || details.getValue() < 0) {
            errorMessage += "The time field should contain only positive numbers";
        }
        return errorMessage;
    }
    
    private void drawConnection(T fromPlace, T toPlace, Pair<String, Integer> details) {
        Line connectionLine = new Line(fromPlace.getX(), fromPlace.getY(), toPlace.getX(), toPlace.getY());
        connectionLine.setStroke(Color.BLACK);
        connectionLine.setStrokeWidth(3);
    
        drawingGroup.getChildren().add(connectionLine);
    
        connections.add(new Edge<>(fromPlace, toPlace, details.getKey(), details.getValue()));
        graph.connect(fromPlace, toPlace, details.getKey(), details.getValue());
    }    
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private boolean validatePlacesSelection() {
        return selectedPlaces.size() == 2;
    }
    
    private Optional<Edge<T>> getConnectionBetweenPlaces(T fromPlace, T toPlace) {
        return connections.stream().filter(edge ->
            edge.getFrom().equals(fromPlace) && edge.getDestination().equals(toPlace) ||
            edge.getFrom().equals(toPlace) && edge.getDestination().equals(fromPlace)
        ).findFirst();
    }
    
    private Integer showConnectionDialog(Optional<Edge<T>> connection) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Connection");
        dialog.setHeaderText("Changing connection from " + connection.get().getFrom().getName() + " to " + connection.get().getDestination().getName());
    
        TextField nameField = new TextField(connection.get().getName());
        nameField.setEditable(false);
        TextField weightField = new TextField();
        weightField.setPromptText("Time");
    
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(weightField, 1, 1);
        dialog.getDialogPane().setContent(grid);
    
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
    
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    return Integer.parseInt(weightField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });
    
        return dialog.showAndWait().orElse(null);
    }
    
    private void updateConnection(Optional<Edge<T>> connection, Integer newWeight) {
        if (newWeight == null) {
            showAlert("The time field must consist of numbers");
        } else {
            connection.get().setWeight(newWeight);
            graph.setConnectionWeight(connection.get().getFrom(), connection.get().getDestination(), newWeight);
            connections.stream()
                .filter(edge ->
                    edge.getDestination().equals(connection.get().getFrom()) && edge.getFrom().equals(connection.get().getDestination())
                )
                .findFirst()
                .ifPresent(edge -> edge.setWeight(newWeight));
        }
    }

    private void showConnectionDetails() {
        if (selectedPlaces.size() != 2) {
            showAlert("Two places must be selected!");
            return;
        }
    
        T fromPlace = selectedPlaces.get(0);
        T toPlace = selectedPlaces.get(1);
    
        Optional<Edge<T>> connection = connections.stream().filter(edge -> 
            edge.getFrom().equals(fromPlace) && edge.getDestination().equals(toPlace) ||
            edge.getFrom().equals(toPlace) && edge.getDestination().equals(fromPlace)
        ).findFirst();
    
        if (!connection.isPresent()) {
            showAlert("No connection exists between the selected places");
            return;
        }
    
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Connection");
        dialog.setHeaderText("Connection from " + fromPlace.getName() + " to " + toPlace.getName());
    
        TextField nameField = new TextField(connection.get().getName());
        nameField.setEditable(false);
        TextField weightField = new TextField(String.valueOf(connection.get().getWeight()));
        weightField.setEditable(false);
    
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(weightField, 1, 1);
        dialog.getDialogPane().setContent(grid);
    
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        dialog.showAndWait();
    }

    private void findPath() {
        if (!hasSelectedTwoPlaces()) {
            showAlert("Two places must be selected to find a path");
            return;
        }
    
        T startPlace = selectedPlaces.get(0);
        T endPlace = selectedPlaces.get(1);
    
        List<Edge<T>> path = graph.getPath(startPlace, endPlace);
        if (path == null || path.isEmpty()) {
            showAlert("No path found between the selected places");
            return;
        }
    
        String pathDetails = buildPathDetails(path);
        showInfoAlert("The Path from " + startPlace.getName() + " to " + endPlace.getName() + ":", pathDetails);
    }
    
    private boolean hasSelectedTwoPlaces() {
        return selectedPlaces.size() == 2;
    }
    
    private String buildPathDetails(List<Edge<T>> path) {
        StringBuilder pathDetails = new StringBuilder();
        int totalTime = 0;
    
        for (Edge<T> edge : path) {
            pathDetails.append("to ")
                        .append(edge.getDestination().getName())
                        .append(" by ")
                        .append(edge.getName())
                        .append(" takes ")
                        .append(edge.getWeight())
                        .append("\n");
    
            totalTime += edge.getWeight();
        }
        pathDetails.append("\nTotal ").append(totalTime);
        return pathDetails.toString();
    }
    
    private void showInfoAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }       

    public static void main(String[] args) {
        launch(args);
    }
}