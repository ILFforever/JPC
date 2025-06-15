package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import server.GameServer;
import client.GameClient;
import java.util.Random;

public class Main extends Application {

    private static SharedState sharedState = new SharedState(); // Create a SharedState instance
    private static TextArea logArea = new TextArea();
    private static TextArea typeArea = new TextArea();
    private static Button sendButton = new Button("Send");
    private static Button connectButton = new Button("Connect");
    private static Button hostServerButton = new Button("Host a Server");
    private static Button connectClientButton = new Button("Connect as Client");
    private static Button cancelButton = new Button("Cancel");
    private static Button disconnectButton = new Button("Disconnect");
    private static TextField nameField = new TextField();
    private int serverPort;
    private static boolean isGameWindow = false;
    private static int State = 0; //0 Idle, 1 Server, 2 Client;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Java Peer Connect V1.2");
        
        serverPort = new Random().nextInt(64512) + 1024;
        
        Label label = new Label("Choose Function!");
        nameField.setPromptText("Enter your name");

        hostServerButton.setOnAction(event -> {
            broadcastServer();
            GameServer.startServer(sharedState, logArea,serverPort); // Start the server for message handling
        });
        
        connectClientButton.setOnAction(event -> connectClient());

        cancelButton.setOnAction(event -> cancelBroadcast());
        cancelButton.setDisable(true);
        
        disconnectButton.setDisable(true); // Initially disabled
        disconnectButton.setPrefWidth(140);
        disconnectButton.setOnAction(event -> {
            System.out.println("Disconnect button pressed. Resetting state and closing sockets.");
            cancelBroadcast();
            disconnectButton.setDisable(true);
        });

        sendButton.setPrefWidth(65);
        connectButton.setPrefWidth(65);


        VBox controls = new VBox();
        controls.setSpacing(20);
        controls.setAlignment(Pos.TOP_CENTER); // Align controls to top-center
        controls.getChildren().addAll(label, nameField, hostServerButton, connectClientButton, cancelButton);
        controls.setPadding(new Insets(10));

        logArea.setEditable(false);
        logArea.setPrefWidth(600);
        logArea.setPrefHeight(350);

        typeArea.setPrefHeight(50);
        typeArea.setPrefWidth(500);
        

        sendButton.setDisable(true);
        connectButton.setDisable(true);
        sendButton.setOnAction(event -> {
            if (sharedState.isClient()) {
                GameClient.sendMessage(typeArea.getText(), logArea);
            } else {
                GameServer.sendMessageToClients(typeArea.getText(), logArea);
            }
            typeArea.clear();
        });

        connectButton.setOnAction(event -> {
            String message = typeArea.getText();
            try {
                int serverIndex = Integer.parseInt(message.trim()) - 1;
                boolean success = GameClient.connectToServer(serverIndex, logArea, getPlayerName());
                if (success) {
                    disconnectButton.setDisable(false); // Enable disconnect button on successful connection
                    sendButton.setDisable(false);
                    connectButton.setDisable(true);
                    typeArea.clear();
                    typeArea.setPromptText("Type your message here...");
                }
            } catch (NumberFormatException e) {
                log("Invalid input. Please enter a number.");
            }
        });
        
        HBox SCbuttons = new HBox();
        SCbuttons.setSpacing(10);
        SCbuttons.getChildren().addAll(sendButton, connectButton);
        
        VBox buttonStack = new VBox();
        buttonStack.setSpacing(5);
        buttonStack.getChildren().addAll(SCbuttons, disconnectButton);
        
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.getChildren().addAll(typeArea, buttonStack);

        VBox Rbox = new VBox();
        Rbox.setSpacing(8);
        Rbox.setPadding(new Insets(5));
        Rbox.getChildren().addAll(logArea, messageBox);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10)); // add Padding for the entire root pane

        root.setLeft(controls);
        BorderPane.setMargin(controls, new Insets(0, 20, 0, 0));

        root.setCenter(Rbox);
        Label developedByLabel = new Label("Developed by ILFforever");
        HBox bottomBox = new HBox(developedByLabel);
        bottomBox.setAlignment(Pos.BOTTOM_LEFT);
        BorderPane.setMargin(bottomBox, new Insets(10, 0, 0, 0));
        root.setBottom(bottomBox);


        Scene scene = new Scene(root, 900, 450);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> handleWindowClose(event));
        primaryStage.show();
    }

    public void broadcastServer() {
        if (!sharedState.isBroadcast()) {
            log("Starting broadcast...");
            State = 1;
            logArea.clear();
            sendButton.setDisable(false);
            connectButton.setDisable(true);
            hostServerButton.setDisable(true);
            connectClientButton.setDisable(true);
            cancelButton.setDisable(false);
            nameField.setDisable(true);
            typeArea.setPromptText("Send massage to connected clients...");
            sharedState.setBroadcast(true);
            disconnectButton.setDisable(false); // Enable disconnect button
            GameServer.startBroadcasting(sharedState, logArea, getServerName(), serverPort);        
            }
    }

    public void connectClient() {
        if (!sharedState.isClient()) {
            log("Connecting as client...");
            State = 2;
            logArea.clear();
            hostServerButton.setDisable(true);
            connectClientButton.setDisable(true);
            cancelButton.setDisable(false);
            nameField.setDisable(true); // Disable nameField
            connectButton.setDisable(false);
            typeArea.setPromptText("Enter server number to connect first...");
            sharedState.setClient(true);
            sharedState.setBroadcast(false);
            GameClient.startClient(sharedState, logArea);
        }
    }

    public void cancelBroadcast() {
        log("Stopping ...");
        State = 0;
        cancelButton.setDisable(true);
        nameField.setDisable(false); // Re-enable nameField
        typeArea.setPromptText("");
        
        if (sharedState.isBroadcast()) {
            sharedState.setBroadcast(false);
            GameServer.stopServer();
        }
        
        if (sharedState.isClient()) {
            sharedState.setClient(false);
            sendButton.setDisable(true);
            connectButton.setDisable(true);
            GameClient.stopClient(logArea); // Pass logArea to stopClient
        }
        hostServerButton.setDisable(false);
        connectClientButton.setDisable(false);
        logArea.clear();
        disconnectButton.setDisable(true);
    }

    private void handleWindowClose(WindowEvent event) {
        log("Window is closing...");
        sharedState.setBroadcast(false);
        sharedState.setClient(false);
        GameServer.stopServer();
        GameClient.stopClient(logArea);
        System.out.println("Window is closing...");
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }
    
    public static void switchbuttonDisable()
    {
        if (sendButton.isDisable() && !connectButton.isDisable()) {
            sendButton.setDisable(false);
            connectButton.setDisable(true);
        }
        else if (!sendButton.isDisable() && connectButton.isDisable()) {
            sendButton.setDisable(true);
            connectButton.setDisable(false);
        }
    }
    
    public static String getPlayerName() {
        return nameField.getText().isEmpty() ? "Player" : nameField.getText();
    }
    
    public static String getServerName() {
        return nameField.getText().isEmpty() ? "Host" : nameField.getText();
    }
    
    public static void setDisconnectButtonDisable(boolean set) {
        disconnectButton.setDisable(set);
    }
    
    public static boolean isGameWindow() {
        return isGameWindow;
    }
    
    public static int getState() {
        return State;
    }

}
