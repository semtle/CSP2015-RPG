package org.csproject.view;/**
 * Created by Brett on 9/21/2015.
 */

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.csproject.configuration.SpringConfiguration;
import org.csproject.model.Constants;
import org.csproject.model.actors.Monster;
import org.csproject.model.actors.PlayerActor;
import org.csproject.model.actors.PlayerParty;
import org.csproject.service.ScreensController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class MasterController extends Application {

    public static final String START_MENU_ID = "startMenu";
    public static final String START_MENU_FILE = "StartMenu.fxml";
    public static final String NEW_GAME_ID = "newGame";
    public static final String NEW_GAME_FILE = "NewGame.fxml";
    public static final String GAME_SCREEN = "gameScreen";
    public static final String TOWN_SCREEN = "townScreen";
    public static final String BATTLE_SCREEN_ID = "battleScreen";
    public static final String BATTLE_SCREEN_FILE = "BattleScreen.fxml";

    //Switch these two statements to start the game faster/slower
    public static final boolean fastStart = true;
    //public static final boolean fastStart = false;

    public static ScreensController screensController;

    private static AnnotationConfigApplicationContext context;

    public static void main(String[] args) {

        context = new AnnotationConfigApplicationContext(SpringConfiguration.class);

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {


        primaryStage.setWidth(Constants.SCREEN_WIDTH);
        primaryStage.setHeight(Constants.SCREEN_HEIGHT);

        screensController = context.getBean(ScreensController.class);

        if (!fastStart) {
            screensController.loadScreen(START_MENU_ID, START_MENU_FILE);
            screensController.loadScreen(NEW_GAME_ID, NEW_GAME_FILE);
            screensController.loadScreen(BATTLE_SCREEN_ID, BATTLE_SCREEN_FILE);
            screensController.setScreen(START_MENU_ID);


            Group root = new Group();
            root.getChildren().addAll(screensController.getRoot());
            primaryStage.setScene(new Scene(root));
        } else {
            /* Skip the beginning scenes and start the game right away.
             * Change the values in the ScreensController class (setUpNewGame() function)
             * to view different scenes like the static map, dungeon, forest, etc
             */
            screensController.setUpNewGame();
            screensController.addScreen(GAME_SCREEN, screensController.getFieldScreen());
            screensController.setScreen(GAME_SCREEN);

            /*
                Example for creating a new battle. Comment out the three lines above and uncomment
                everything underneath this block comment. You have to call getBattleController from
                a screensController object to be able to operate any methods to alter the gui.
                In this example, all of the enemy images are still null, but I am able to start a new
                battle, put all of the player characters in it, and damage one of them. Note that no
                Monsters have been created at the time of this implementation.
             */
            screensController.loadScreen(BATTLE_SCREEN_ID, BATTLE_SCREEN_FILE);

            Group root = new Group();
            root.getChildren().addAll(screensController.getRoot());
            primaryStage.setScene(new Scene(root));

            /*have to use a static playerparty for faststart*/

            PlayerActor char1 = new PlayerActor("Bladerunner", Constants.CLASS_THIEF, 15, 25, 1, 1);
            PlayerActor char2 = new PlayerActor("Tim", Constants.CLASS_MAGE, 14, 25, 1, 1);
            PlayerActor char3 = new PlayerActor("Knightrider", Constants.CLASS_KNIGHT, 13, 25, 1, 1);

            PlayerParty party = new PlayerParty(char1, char2, char3, 0);
            screensController.setParty(party);
        }
        primaryStage.show();
    }

    public static void setScreen(String screenName) {
        screensController.setScreen(screenName);
    }

    public static BattleScreenController getBattleController() {
        return screensController.getBattleController();
    }

    public static ScreensController getScreensController() {
        return screensController;
    }
}
