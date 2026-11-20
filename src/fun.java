// Blake, Caden, Josh
// 11/20/2026
// Markov Based Chat Bot

//imports...
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import java.io.*;
import javafx.application.Platform;

public class fun extends Application {
	
	//initial variables
    Boolean talking = true;
    Boolean mouthState = false;
    Boolean blinkState = false;
    String output = "Greetings, I am John Markov.";
    VBox dialogueContent = new VBox(10);
    ScrollPane dialogueScroll = new ScrollPane(dialogueContent);
    Timeline typewriter;
    PrintWriter toBot;

    @Override
    public void start(Stage primaryStage) {
    		//image for John's body
        Image stickGuy = new Image(getClass().getResourceAsStream("assets/stick.png"));
        ImageView stickView = new ImageView(stickGuy);
        stickView.setScaleX(0.8);
        stickView.setScaleY(0.8);
        stickView.setPreserveRatio(true);
        stickView.setLayoutX(500);
        stickView.setLayoutY(-15);
        //image for John's open mouth
        Image mouthOpen = new Image(getClass().getResourceAsStream("assets/stickMouthOpen.png"));
        ImageView mouthOpenV = new ImageView(mouthOpen);
        mouthOpenV.setScaleX(1.6);
        mouthOpenV.setScaleY(1.2);
        mouthOpenV.setPreserveRatio(true);
        mouthOpenV.setLayoutX(640);
        mouthOpenV.setLayoutY(140);
        mouthOpenV.setVisible(mouthState);
        //image for John's closed mouth
        Image mouthClose = new Image(getClass().getResourceAsStream("assets/stickMouthClose.png"));
        ImageView mouthCloseV = new ImageView(mouthClose);
        mouthCloseV.setScaleX(1.6);
        mouthCloseV.setScaleY(1.2);
        mouthCloseV.setPreserveRatio(true);
        mouthCloseV.setLayoutX(640);
        mouthCloseV.setLayoutY(145);
        mouthCloseV.setVisible(!mouthState);
        //image for John's first Eye
        Image eyeball1 = new Image(getClass().getResourceAsStream("assets/eyeball.png"));
        ImageView eyeball1V = new ImageView(eyeball1);
        eyeball1V.setFitWidth(90);
        eyeball1V.setPreserveRatio(true);
        eyeball1V.setLayoutX(605);
        eyeball1V.setLayoutY(87);
        eyeball1V.setVisible(!blinkState);
        //image for John's second Eye
        Image eyeball2 = new Image(getClass().getResourceAsStream("assets/eyeball.png"));
        ImageView eyeball2V = new ImageView(eyeball2);
        eyeball2V.setFitWidth(90);
        eyeball2V.setPreserveRatio(true);
        eyeball2V.setLayoutX(650);
        eyeball2V.setLayoutY(87);
        eyeball2V.setVisible(!blinkState);
        //image for John's first eyelid
        Image blink1 = new Image(getClass().getResourceAsStream("assets/blink.png"));
        ImageView blink1V = new ImageView(blink1);
        blink1V.setScaleX(0.05);
        blink1V.setScaleY(0.1);
        blink1V.setPreserveRatio(true);
        blink1V.setLayoutX(250);
        blink1V.setLayoutY(-61);
        blink1V.setVisible(blinkState);
        //image for John's second eyelid
        Image blink2 = new Image(getClass().getResourceAsStream("assets/blink.png"));
        ImageView blink2V = new ImageView(blink2);
        blink2V.setScaleX(0.05);
        blink2V.setScaleY(0.1);
        blink2V.setPreserveRatio(true);
        blink2V.setLayoutX(300);
        blink2V.setLayoutY(-61);
        blink2V.setVisible(blinkState);
        //redundant dialogue box that I refuse to remove
        Rectangle dialogueBox = new Rectangle(50, 50, 450, 250);
        dialogueBox.setFill(Color.LIGHTGRAY);
        dialogueBox.setStroke(Color.BLACK);
        dialogueBox.setStrokeWidth(2);
        dialogueBox.setArcWidth(10);
        dialogueBox.setArcHeight(10);
        
        // the content inside the output box
        dialogueContent.setPadding(new Insets(10));
        dialogueContent.heightProperty().addListener((obs, oldVal, newVal) -> dialogueScroll.setVvalue(1.0));
        
       // the box for the ouput
        dialogueScroll.setLayoutX(50);
        dialogueScroll.setLayoutY(50);
        dialogueScroll.setPrefWidth(450);
        dialogueScroll.setPrefHeight(250);
        dialogueScroll.setFitToWidth(true);
        dialogueScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dialogueScroll.setStyle(
            "-fx-background: lightgray; " +
            "-fx-background-color: lightgray; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        
        // John Markov Label
        Label nameTag = new Label("John Markov");
        nameTag.setLayoutX(628);
        nameTag.setLayoutY(10);
        nameTag.setStyle("-fx-font-size: 15px;");
        
        //Output box
        TextArea inputBox = new TextArea();
        inputBox.setLayoutX(50);
        inputBox.setLayoutY(325);
        inputBox.setPrefWidth(450);
        inputBox.setPrefHeight(150);
        inputBox.setWrapText(true);
        inputBox.setPromptText("Type a message and press Enter...");
        inputBox.setStyle(
            "-fx-control-inner-background: lightgray; " +
            "-fx-prompt-text-fill: dimgray; " +
            "-fx-border-color: black; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px; " +
            "-fx-font-size: 18px; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        // Sends key presses into input and sends input to the bot on enter
        inputBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                String userText = inputBox.getText().trim();

                if (!userText.isEmpty()) {
                    inputBox.clear();
                    addUserMessage(userText);
                    if (toBot != null) {
                        toBot.println(userText);
                    }
                }
            }
        });
        
        // writes everything to the screen
        Pane roots = new Pane();
        roots.setStyle("-fx-background-color: white;");
        roots.getChildren().addAll(stickView, dialogueBox, dialogueScroll, mouthOpenV, mouthCloseV, nameTag, inputBox, eyeball1V, eyeball2V, blink1V, blink2V);

        Scene scene = new Scene(roots, 800, 500);
        primaryStage.setTitle("Markov Chat Bot");
        primaryStage.setScene(scene);
        
        // this keyFrame makes his mouth move on a loop when talking == true
        Timeline mouth = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(100), e -> {
            if (talking) {
                mouthState = !mouthState;
                mouthCloseV.setVisible(!mouthState);
                mouthOpenV.setVisible(mouthState);
            } else {
                mouthState = false;
                mouthCloseV.setVisible(!mouthState);
                mouthOpenV.setVisible(mouthState);
            }
        });
        scheduleBlink(eyeball1V, eyeball2V, blink1V, blink2V);
        mouth.getKeyFrames().add(keyFrame);
        mouth.setCycleCount(Timeline.INDEFINITE);
        mouth.play();
        
        //Make him say the initial message
        say(output);
        //starts the bot
        startBot();
        //shows things on screen
        primaryStage.show();
    }

    // adds your message to the output box
    public void addUserMessage(String userText) {
        if (typewriter != null) {
            typewriter.stop();
            talking = false;
        }

        Label userLabel = new Label("You: " + userText);
        userLabel.setWrapText(true);
        userLabel.setMaxWidth(Double.MAX_VALUE);
        userLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: dimgray;");
        dialogueContent.getChildren().add(userLabel);
    }

    // this is the function which makes him say things and makes it come out in a typewriter fashion.
    public void say(String newText) {
        if (newText == null || newText.isEmpty()) return;

        if (typewriter != null) {
            typewriter.stop();
        }

        Label botLabel = new Label("");
        botLabel.setWrapText(true);
        botLabel.setMaxWidth(Double.MAX_VALUE);
        botLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: black;");
        dialogueContent.getChildren().add(botLabel);

        talking = true;
        final int[] index = {0};

        typewriter = new Timeline(new KeyFrame(Duration.millis(60), e -> {
            botLabel.setText(botLabel.getText() + newText.charAt(index[0]));
            index[0]++;
        }));

        typewriter.setCycleCount(newText.length());
        typewriter.setOnFinished(e -> talking = false);
        typewriter.play();
    }

    // blinks the eyes on a random interval between 2 and 4 seconds
    public void scheduleBlink(ImageView eyeball1V, ImageView eyeball2V, ImageView blink1V, ImageView blink2V) {
        double randomDelay = 2000 + (Math.random() * 2000);

        PauseTransition wait = new PauseTransition(Duration.millis(randomDelay));
        wait.setOnFinished(e -> {
            blinkState = true;
            eyeball1V.setVisible(false);
            eyeball2V.setVisible(false);
            blink1V.setVisible(true);
            blink2V.setVisible(true);

            PauseTransition closeTime = new PauseTransition(Duration.millis(200));
            closeTime.setOnFinished(event -> {
                blinkState = false;
                eyeball1V.setVisible(true);
                eyeball2V.setVisible(true);
                blink1V.setVisible(false);
                blink2V.setVisible(false);

                scheduleBlink(eyeball1V, eyeball2V, blink1V, blink2V);
            });
            closeTime.play();
        });
        wait.play();
    }
    
    //Sends out input to receive output from the CLI
    public void startBot() {
        try {
            // sends input into the CLI
            PipedOutputStream outToBot = new PipedOutputStream();
            PipedInputStream botIn = new PipedInputStream(outToBot);
            toBot = new PrintWriter(outToBot, true);
            System.setIn(botIn);

            // Read's the CLI's Output
            PipedOutputStream botOut = new PipedOutputStream();
            PipedInputStream inFromBot = new PipedInputStream(botOut);
            System.setOut(new PrintStream(botOut, true));

            Thread botThread = new Thread(() -> {
                try {
                    CLI.main(new String[0]);
                } catch (Exception ex) {
                    Platform.runLater(() -> say("Error loading bot: " + ex.getMessage()));
                }
            });
            botThread.setDaemon(true);
            botThread.start();

            // Sends John's lines to the say() function
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inFromBot))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int botIndex = line.indexOf("Bot: ");
                        if (botIndex != -1) {
                            String reply = line.substring(botIndex + 5);
                            Platform.runLater(() -> say(reply));
                        }
                    }
                } catch (IOException ignored) {}
            });
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (IOException ex) {
            say("Couldn't connect to bot: " + ex.getMessage());
        }
    }
    
    //Launches the program
    public static void main(String[] args) {
        launch(args);
    }
}