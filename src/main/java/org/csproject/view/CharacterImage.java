package org.csproject.view;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.csproject.model.Constants;
import org.csproject.model.general.Direction;
import org.csproject.service.BattleFactory;

import java.util.Random;

/**
 * Created by Brett on 10/8/2015.
 */
public class CharacterImage extends ImageView {
    private static final int BLOCK_SIZE_X = (int) Constants.TILE_SIZE * 3;
    private static final int BLOCK_SIZE_Y = (int) Constants.TILE_SIZE * 4;



    private int actorImageBlockX, actorImageBlockY;
    private Image actorImage;
    private int animPhase; // 0, 1 or 2
    private Direction faceDirection;
    private Thread animationThread;
    private boolean walking;
    private boolean enemyEncounter;

    /**
     *  set on true if you want the battle to appear
    */
    private boolean startbattle = true;

    private BattleFactory battlefactory = new BattleFactory();

    //For the battle GUI - No threads, viewport is vital.
    public CharacterImage(int blockX, int blockY, String imageURL, Direction direction) {
        super();

        faceDirection = direction;
        actorImageBlockX = blockX;
        actorImageBlockY = blockY;

        actorImage = new Image(imageURL);
        setImage(actorImage);

        setViewport(getCharacterViewport(actorImageBlockX, actorImageBlockY, faceDirection, animPhase));
    }

    /**
     * Maike Keune-Staab
     * returns the viewport of a character image to display a character portrait matching the given parameters.
     * @param actorImageBlockX
     * @param actorImageBlockY
     * @param faceDirection
     * @param animPhase: 0= left foot, 1 = stand still, 2 = right foot
     * @return
     */
    public static Rectangle2D getCharacterViewport(int actorImageBlockX, int actorImageBlockY, Direction faceDirection,
                                                   int animPhase) {
        return new Rectangle2D(
                actorImageBlockX * BLOCK_SIZE_X + animPhase * Constants.TILE_SIZE,
                actorImageBlockY * BLOCK_SIZE_Y + faceDirection.ordinal() * Constants.TILE_SIZE,
                Constants.TILE_SIZE,
                Constants.TILE_SIZE);
    }

    public CharacterImage(int blockX, int blockY, double posX, double posY, String imageUrl) {
        super();

        walking = false;

        setX(posX);
        setY(posY);

        faceDirection = Direction.DOWN;

        actorImageBlockX = blockX;
        actorImageBlockY = blockY;

        actorImage = new Image(imageUrl);

        setImage(actorImage);

        updateAnimation();

        animationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        if(walking) {
                            setAnimationPhase(0);
                            Thread.sleep((long) (Constants.WALK_TIME_PER_TILE * 1000 / 2));
                            setAnimationPhase(2);
                        }
                        Thread.sleep((long) (Constants.WALK_TIME_PER_TILE * 1000 / 2));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        animationThread.setDaemon(true);
        animationThread.start();
    }

    /**
     * Maike Keune-Staab
     * updates the animation face of the players avatar.
     */
    public void updateAnimation() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                setViewport(getCharacterViewport(actorImageBlockX, actorImageBlockY, faceDirection, animPhase));
            }
        });
    }

    /**
     * Maren Sandner
     * calculating and then checking if the battle occured
     * @return if a battle start on the tile
     */
    public boolean didBattleStart() {
        calcEnemyEncounter();
        if (getEnemyEncounter() && startbattle) {
            startbattle = false;
            battlefactory.startBattle();
            setWalking(false);
            startbattle = true;

            return true;
        }
        return false;
    }

    /**
     * Maike Keune-Staab
     * lets the avatar face the given direction
     * @param direction
     */
    public void face(Direction direction)
    {
        faceDirection = direction;
        updateAnimation();
    }

    /**
     * Maike Keune-Staab
     * sets the animationPhase (0=left step, 1=stand still, 2=right step)
     * @param animationPhase
     */
    public void setAnimationPhase(int animationPhase) {
        this.animPhase = animationPhase;
        updateAnimation();
    }

    /**
     * Maike Keune-Staab
     * signalises the animation thread to alterate animation phases or not.
     * @param walking
     */
    public void setWalking(boolean walking) {
        this.walking = walking;
        if(!walking) {
            setAnimationPhase(1);
        }
    }

    /**
     * Maren Sandner
     * calculates the enemy encounter chance when moving the character
     */
    private void calcEnemyEncounter() {
        enemyEncounter = false;
        Random rand = new Random();
        int chance = rand.nextInt(50);
        if(chance < Constants.ENEMY_ENCOUNTER_PERCENTAGE) {
            enemyEncounter = true;
        }
    }

    /**
     * Maren Sandner
     * @return if a battle will be encountered
     */
    public boolean getEnemyEncounter() {
        return enemyEncounter;
    }

    public Direction getFaceDirection() {
        return faceDirection;
    }
}
