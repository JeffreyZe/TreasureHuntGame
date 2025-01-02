import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClient extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private TextArea gameStateArea;
    private TextField commandField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();

        gameStateArea = new TextArea();
        gameStateArea.setEditable(false);
        gameStateArea.setPrefHeight(400);

        commandField = new TextField();
        commandField.setPromptText("Enter command (UP, DOWN, LEFT, RIGHT, DIG) to move");
        commandField.setOnAction(event -> sendCommand(commandField.getText()));

        root.getChildren().addAll(new Label("Game State:"), gameStateArea, commandField);

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setTitle("Treasure Hunt Game Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the client-side connection to the server
        new Thread(this::connectToServer).start();
    }

    // Connect to the server
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read server messages in a separate thread
            String message;
            while ((message = in.readLine()) != null) {
                // Update the game state area when new messages are received
                updateGameState(message);
            }
        } catch (IOException e) {
            // Replace with more robust logging
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    // Send commands to the server
    private void sendCommand(String command) {
        if (command != null && !command.isEmpty() && out != null) {
            out.println(command);
            commandField.clear();
        }
    }

    // Update the game state area with the latest game information
    private void updateGameState(String message) {
        // Append the new message to the text area
        gameStateArea.appendText(message + "\n");
    }

    @Override
    public void stop() {
        // Close resources gracefully
        System.out.println("Stopping client...");
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }
}
