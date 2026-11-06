import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class fun extends Application {
    
	double score = 0;
	double scorePerSecond = 0;
	
	
	
    @Override
    public void start(Stage primaryStage) {
    	
    		
    		
    	
    		Label scoreLabel = new Label("0");
    		scoreLabel.setText(String.format("$%.2f", score));
    		scoreLabel.setStyle("-fx-border-color: black; " +     
                            "-fx-border-width: 2px; " +       
                            "-fx-border-radius: 5px; " +      
                            "-fx-background-color: lightgray; " +
                            "-fx-padding: 0px 0px;" +
                            "-fx-font-size: 75px;" + 
                            "-fx-alignment: center;");
    		scoreLabel.setLayoutX(175);
    		scoreLabel.setLayoutY(25);
    		scoreLabel.setPrefWidth(400);
    		scoreLabel.setPrefHeight(130);
    		Label sPSLabel = new Label(0 + "$/s");
    		sPSLabel.setText(scorePerSecond + "$/s");
    		sPSLabel.setStyle("-fx-font-size: 15px;" + 
                    			"-fx-alignment: center;");
    		sPSLabel.setLayoutX(361);
    		sPSLabel.setLayoutY(127);
    	
    	
        Button button1 = new Button("  $0.50\n+$0.01/s");
        String defaultStyle = 
                "-fx-border-color: black; " +     
                "-fx-border-width: 2px; " +       
                "-fx-border-radius: 5px; " +      
                "-fx-background-color: lightgray; " +
                "-fx-padding: 0px 0px;" +
                "-fx-font-size: 20px;";
        button1.setStyle(defaultStyle);
        button1.setOnMousePressed(e -> button1.setStyle(defaultStyle.replace("lightgray", "darkgray")));
        button1.setOnMouseReleased(e -> button1.setStyle(defaultStyle));
        button1.setOnAction(e -> {
        		if (score >= 0.50) {
        			score = score - 0.50;
        		
        		scorePerSecond = scorePerSecond + 0.01;
        		sPSLabel.setText(scorePerSecond + "$/s");
        		scoreLabel.setText(String.format("$%.2f", score));
        		}
        });
        button1.setLayoutX(25);
        button1.setLayoutY(25);
        button1.setPrefWidth(125);
        button1.setPrefHeight(95);
        
        Button button2 = new Button("  $50\n+$1/s");
        button2.setStyle(defaultStyle);
        button2.setOnMousePressed(e -> button2.setStyle(defaultStyle.replace("lightgray", "darkgray")));
        button2.setOnMouseReleased(e -> button2.setStyle(defaultStyle));
        button2.setOnAction(e -> {
        		if (score >= 50) {
        			score = score - 50;
        		
        		scorePerSecond = scorePerSecond + 1;
        		sPSLabel.setText(scorePerSecond + "$/s");
        		scoreLabel.setText(String.format("$%.2f", score));
        		}
        });
        button2.setLayoutX(25);
        button2.setLayoutY(130);
        button2.setPrefWidth(125);
        button2.setPrefHeight(95);
        
        Button button3 = new Button("  $750\n+$15/s");
        button3.setStyle(defaultStyle);
        button3.setOnMousePressed(e -> button3.setStyle(defaultStyle.replace("lightgray", "darkgray")));
        button3.setOnMouseReleased(e -> button3.setStyle(defaultStyle));
        button3.setOnAction(e -> {
        		if (score >= 750) {
        			score = score - 750;
        		
        		scorePerSecond = scorePerSecond + 15;
        		sPSLabel.setText(scorePerSecond + "$/s");
        		scoreLabel.setText(String.format("$%.2f", score));
        		}
        });
        button3.setLayoutX(25);
        button3.setLayoutY(235);
        button3.setPrefWidth(125);
        button3.setPrefHeight(95);
        
        Button button4 = new Button("Work Job");
        button4.setStyle(defaultStyle);
        button4.setOnMousePressed(e -> button4.setStyle(defaultStyle.replace("lightgray", "darkgray")));
        button4.setOnMouseReleased(e -> button4.setStyle(defaultStyle));
        button4.setOnAction(e -> {
        		score = score + 0.01;
        		scoreLabel.setText(String.format("$%.2f", score));
        		
        });
        button4.setLayoutX(175);
        button4.setLayoutY(165);
        button4.setPrefWidth(400);
        button4.setPrefHeight(225);
        
        
        Pane roots = new Pane();
        roots.getChildren().addAll(button1, button2, button3, button4, scoreLabel, sPSLabel);
        
        Scene scene = new Scene(roots, 600, 400);
        primaryStage.setTitle("Job Simulator");
        primaryStage.setScene(scene);
        Timeline sPS = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(10), e -> {
        		score = score + scorePerSecond / 100;
        		scoreLabel.setText(String.format("$%.2f", score));
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