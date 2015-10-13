package org.csproject.service;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.csproject.model.actors.Actor;
import org.csproject.model.actors.PlayerActor;
import org.csproject.model.bean.Field;
import org.csproject.model.bean.Tile;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * @author Maike Keune-Staab on 12.09.2015.
 */
public interface WorldService {
    Actor createActor(String name, String type);

    Field getNewWorld();

    PlayerActor getPlayerActor();

    List<PlayerActor> getAvailableClasses();

    void setAvailableClasses(List<PlayerActor> playerActors) throws FileNotFoundException;

    // diese methode muss nun für jedes tile ein imageview zur gruppe an der richtigen stelle hinzufügen
    public Group getNode(Tile[][] matrix);
}
