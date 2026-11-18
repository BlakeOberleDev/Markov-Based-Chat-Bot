import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

public class fun extends Application {
    
	/*double score = 0;
	double scorePerSecond = 0;*/
	Boolean talking = true;
	Boolean mouthState = false;
	String output = "Hello There! I am Tod Henderson the best guy around!";
	TextArea dialogue = new TextArea();
	Timeline typewriter;
	
    @Override
    public void start(Stage primaryStage) {
    		Image stickGuy = new Image(getClass().getResourceAsStream("assets/stick.png"));
    		ImageView stickView = new ImageView(stickGuy);
    		stickView.setScaleX(0.8);   
    		stickView.setScaleY(0.8);  
    		stickView.setPreserveRatio(true);
    		stickView.setLayoutX(500);
    		stickView.setLayoutY(-15);
    		
    		Image mouthOpen = new Image(getClass().getResourceAsStream("assets/stickMouthOpen.png"));
    		ImageView mouthOpenV = new ImageView(mouthOpen);
    		mouthOpenV.setScaleX(1.6);   
    		mouthOpenV.setScaleY(1.2);  
    		mouthOpenV.setPreserveRatio(true);
    		mouthOpenV.setLayoutX(640);
    		mouthOpenV.setLayoutY(140);
    		mouthOpenV.setVisible(mouthState);
    		
    		Image mouthClose = new Image(getClass().getResourceAsStream("assets/stickMouthClose.png"));
    		ImageView mouthCloseV = new ImageView(mouthClose);
    		mouthCloseV.setScaleX(1.6);   
    		mouthCloseV.setScaleY(1.2);  
    		mouthCloseV.setPreserveRatio(true);
    		mouthCloseV.setLayoutX(640);
    		mouthCloseV.setLayoutY(145);
    		mouthCloseV.setVisible(!mouthState);
    		
    		try {
    		    java.io.File folder = new java.io.File(getClass().getResource("assets/stick.png").toURI()).getParentFile();
    		    System.out.println("Looking in: " + folder.getAbsolutePath());
    		    for (String f : folder.list()) System.out.println(" - " + f);
    		} catch (Exception ex) { ex.printStackTrace(); }
    		
    		Image eyeball1 = new Image(getClass().getResourceAsStream("assets/eyeball.png"));
    		ImageView eyeball1V = new ImageView(eyeball1);
    		eyeball1V.setScaleX(0.3);   
    		eyeball1V.setScaleY(0.3);  
    		eyeball1V.setPreserveRatio(true);
    		eyeball1V.setLayoutX(640);
    		eyeball1V.setLayoutY(145);
    		//eyeball1V.setVisible();
    		
    		
    		Rectangle dialogueBox = new Rectangle(50, 50, 450, 250);
    		dialogueBox.setFill(Color.LIGHTGRAY);
    		dialogueBox.setStroke(Color.BLACK);
    		dialogueBox.setStrokeWidth(2);
    		dialogueBox.setArcWidth(10);  
    		dialogueBox.setArcHeight(10);
        
    		dialogue.setLayoutX(50);
    		dialogue.setLayoutY(50);
    		dialogue.setPrefWidth(450);
    		dialogue.setPrefHeight(250);
    		dialogue.setWrapText(true);
    		dialogue.setEditable(false);
    		dialogue.setStyle(
    		    "-fx-control-inner-background: lightgray; " +
    		    "-fx-border-color: black; " +
    		    "-fx-border-width: 2px; " +
    		    "-fx-border-radius: 5px; " +
    		    "-fx-background-radius: 5px; " +
    		    "-fx-font-size: 22px;"
    		);
    			
    		Label nameTag = new Label("John Markov");
    		nameTag.setLayoutX(628);
    		nameTag.setLayoutY(10);
    		nameTag.setStyle("-fx-font-size: 15px;");
    		
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
    			    "-fx-font-size: 18px;"
    			);
    		inputBox.setOnKeyPressed(e -> {
    		    if (e.getCode() == KeyCode.ENTER) {
    		        e.consume();
    		        String userText = inputBox.getText().trim();
    		        
    		        if (!userText.isEmpty()) {
    		            inputBox.clear();
    		            // Use userText variable as input here to make him say using the say() function what is then outputted.
    		            say("I didn't like how you said: " + userText); // << this is a placeholder, feel free to change it.
    		        }
    		    }
    		});
        
        
        
        Pane roots = new Pane();
        roots.setStyle("-fx-background-color: white;");
        roots.getChildren().addAll(stickView, dialogueBox, dialogue, mouthOpenV, mouthCloseV, nameTag, inputBox, eyeball1V);
        
        Scene scene = new Scene(roots, 800, 500);
        primaryStage.setTitle("Markov Chat Bot");
        primaryStage.setScene(scene);
        Timeline sPS = new Timeline();
        
        //this keyFrame makes his mouth move on a loop when talking == true
        KeyFrame keyFrame = new KeyFrame(Duration.millis(100), e -> {
        		if (talking) {
        			mouthState = !mouthState;
        			mouthCloseV.setVisible(!mouthState);
        			mouthOpenV.setVisible(mouthState);
        		} else if (talking == false) {
        			mouthState = false;
        			mouthCloseV.setVisible(!mouthState);
        			mouthOpenV.setVisible(mouthState);
        		}
        });
        
        sPS.getKeyFrames().add(keyFrame);
        sPS.setCycleCount(Timeline.INDEFINITE);
        sPS.play();
        say(output);
        primaryStage.show();
    }

    //this is the function which makes him say things
    public void say(String newText) {
        if (newText == null || newText.isEmpty()) return;

        if (typewriter != null) {
            typewriter.stop();
        }

        
        if (!dialogue.getText().isEmpty()) {
            dialogue.appendText("\n\n"); 
        }

        talking = true;
        final int[] index = {0};

        typewriter = new Timeline(new KeyFrame(Duration.millis(60), e -> {
            dialogue.appendText(String.valueOf(newText.charAt(index[0])));
            index[0]++;
        }));

        typewriter.setCycleCount(newText.length());
        typewriter.setOnFinished(e -> talking = false);
        typewriter.play();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
