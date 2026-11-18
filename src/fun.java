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

public class fun extends Application {
    
	/*double score = 0;
	double scorePerSecond = 0;*/
	Boolean talking = true;
	Boolean mouthState = false;
	
	
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
    		
    		
    		Rectangle dialogueBox = new Rectangle(50, 50, 220, 150);
            dialogueBox.setFill(Color.LIGHTGRAY);
            dialogueBox.setStroke(Color.BLACK);
            dialogueBox.setWidth(450);
            dialogueBox.setHeight(250);
            dialogueBox.setStrokeWidth(2);
            dialogueBox.setArcWidth(10);  
            dialogueBox.setArcHeight(10);
        
        Label dialogue = new Label();
            dialogue.setLayoutX(60);
            dialogue.setLayoutY(60);
            dialogue.setMaxWidth(450); // Keeps text inside the 220px box
            dialogue.setWrapText(true);
    	
    		
        
        
        
        
        Pane roots = new Pane();
        roots.setStyle("-fx-background-color: white;");
        roots.getChildren().addAll(stickView, dialogueBox, dialogue, mouthOpenV, mouthCloseV);
        
        Scene scene = new Scene(roots, 800, 500);
        primaryStage.setTitle("Markov Chat Bot");
        primaryStage.setScene(scene);
        Timeline sPS = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(150), e -> {
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
        primaryStage.show();
    }

    
    
    
    public static void main(String[] args) {
        launch(args);
    }
}
