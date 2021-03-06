package org.csproject.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.csproject.model.field.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.csproject.model.Constants.*;

/**
 * Fieldfactory is for randomly generating fields such as dungeons and future random fields
 *
 * @author Maike Keune-Staab on 31.10.2015.
 */
@Component
public class FieldFactory {
    public static final int NORM_FACTOR = 2;

    @Autowired
    private WorldService worldService;

    /**
     * Maike Keune-Staab
     * generates a random dungeon according to the given DungeonDetails and returns the first floor.
     *
     * @param dungeonDetails
     * @return
     */
    public Field generateDungeon(DungeonDetails dungeonDetails) {
        // Binary Space Partitioning

        Field entryField = null;

        List<String> floorNames = new ArrayList<>();

        for (int i = 0; i < dungeonDetails.getFloors(); i++) {
            TileInfo[][] bsp = getEmptyBsp(dungeonDetails.getHeight(), dungeonDetails.getWidth());
            createRandomBsp(bsp, true, true, true, dungeonDetails);
            String tempFieldName = UUID.randomUUID().toString();
            Field currentField = createField(bsp, dungeonDetails, tempFieldName);
            currentField.setGroundImage(dungeonDetails.getDefaultGroundImage());
            currentField.setDecoImage(dungeonDetails.getDefaultDecoImage());
            worldService.setTempField(currentField, tempFieldName);
            floorNames.add(tempFieldName);
            if (i == 0) {
                entryField = currentField;
            }
        }

        Field startField = worldService.getField(floorNames.get(0));
        Collection<TeleportPoint> startTeleporter
                = startField.getTeleporter(DungeonHelper.RANDOM_START);
        setTarget(startTeleporter, dungeonDetails.getSourceMap(), dungeonDetails.getSourcePoint());

        if (floorNames.size() > 2) {
            for (int i = 1; i < floorNames.size() - 1; i++) {
                Field currentField = worldService.getField(floorNames.get(i));

                // set source teleporter
                Collection<TeleportPoint> sourceTeleporter = currentField.getTeleporter(DungeonHelper.RANDOM_START);
                setTarget(sourceTeleporter, floorNames.get(i - 1), DungeonHelper.RANDOM_END);
                // set target teleporter
                Collection<TeleportPoint> targetTeleporter = currentField.getTeleporter(DungeonHelper.RANDOM_END);
                setTarget(targetTeleporter, floorNames.get(i + 1), DungeonHelper.RANDOM_START);
            }
        }

        Field endField = worldService.getField(floorNames.get(floorNames.size() - 1));
        Collection<TeleportPoint> endTeleporter = endField.getTeleporter(DungeonHelper.RANDOM_END);
        setTarget(endTeleporter, dungeonDetails.getTargetMap(), dungeonDetails.getTargetPoint());

        if (floorNames.size() > 1) {
            Collection<TeleportPoint> startEndTeleporter = startField.getTeleporter(DungeonHelper.RANDOM_END);
            setTarget(startEndTeleporter, floorNames.get(1), DungeonHelper.RANDOM_START);
            Collection<TeleportPoint> endStartTeleporter = endField.getTeleporter(DungeonHelper.RANDOM_START);
            setTarget(endStartTeleporter, floorNames.get(floorNames.size() - 2), DungeonHelper.RANDOM_END);
        }
        return entryField;
    }

    private Field createField(TileInfo[][] bsp, DungeonDetails dungeonDetails, String fieldName) {
        Map<TileInfoFilter, FieldSelection> tileMapping = dungeonDetails.getTileMapping();
        List<TileInfoFilter> filterOrder = dungeonDetails.getFilterOrder();
        if (filterOrder == null) {
            filterOrder = new ArrayList<>(tileMapping.keySet());
        }

        int width = bsp[0].length;
        int height = bsp.length;
        Tile[][] ground = new Tile[height][width];
        Tile[][] deco = new Tile[height][width];
        for (int i = 0; i < height; i++) {
            ground[i] = new Tile[width];
            deco[i] = new Tile[width];
        }
        Collection<StartPoint> startPoints = new ArrayList<>();
        Collection<TeleportPoint> teleportPoints = new ArrayList<>();

        for (TileInfoFilter filter : filterOrder) {
            for (int row = 0; row < bsp.length; row++) {
                for (int col = 0; col < bsp[row].length; col++) {
                    FieldSelection fieldSelection = tileMapping.get(filter);
                    if (filter.matches(bsp, col, row)) {
                        boolean singletonField = fieldSelection.getSingletonFields().size() > 0 && Math.random() < 0.2;
                        List<Field> fields = singletonField ?
                                fieldSelection.getSingletonFields()
                                : fieldSelection.getFields();

                        int randomPosition = (int) ((Math.random() * (fields.size())));
                        Field field = fields.get(randomPosition);
                        if (singletonField) {
                            fields.remove(randomPosition);
                        }

                        boolean takeOverGround = filter.overwriteGround(ground, row, col);
                        boolean takeOverDeco = filter.overwriteDeco(deco, row, col);

                        if (takeOverGround) {
                            setTiles(ground, row, col, field.getGroundTiles());
                        }
                        if (takeOverDeco) {
                            setTiles(deco, row, col, field.getDecoTiles());
                        }
                        if (takeOverGround && takeOverDeco) {
                            for (StartPoint startPoint : field.getStartPoints()) {
                                startPoints.add(new StartPoint(col + startPoint.getX(), row + startPoint.getY(),
                                        startPoint.getName()));
                            }
                            for (TeleportPoint teleportPoint : field.getTeleportPoints()) {

                                if (!DungeonHelper.RANDOM_END.equals(teleportPoint.getName())
                                        && !DungeonHelper.RANDOM_START.equals(teleportPoint.getName())) {
                                    worldService.setTempField(worldService.getField(teleportPoint.getTargetField()),
                                            teleportPoint.getTargetField());

                                    for (TeleportPoint entryTeleporter : worldService.getField(teleportPoint.getTargetField()).getTeleporter("entry")) {
                                        entryTeleporter.setTargetField(fieldName);
                                    }
                                }

                                teleportPoints.add(new TeleportPoint(false, col + teleportPoint.getX(),
                                        row + teleportPoint.getY(), teleportPoint.getName(), teleportPoint.getTargetField(),
                                        teleportPoint.getTargetPoint(), teleportPoint.getSourceField(),
                                        teleportPoint.getSourcePoint(), teleportPoint.getRandomHeight(),
                                        teleportPoint.getRandomWidth(), teleportPoint.getRandomFloors(),
                                        teleportPoint.getRandomType()));
                            }
                        }
                    }
                }
            }
        }

        Field field = new Field(ground, deco);
        field.getStartPoints().addAll(startPoints);
        field.getTeleportPoints().addAll(teleportPoints);
        return field;
    }

    /**
     * Maike Keune-Staab
     * iterates over a collection of TeleportPoints and and sets their target
     *
     * @param startTeleporter
     * @param targetMap
     * @param targetPoint
     */
    private void setTarget(Collection<TeleportPoint> startTeleporter, String targetMap, String targetPoint) {
        if (startTeleporter != null) {
            for (TeleportPoint teleportPoint : startTeleporter) {
                teleportPoint.setTargetField(targetMap);
                teleportPoint.setTargetPoint(targetPoint);
            }
        }
    }

    /**
     * Maike Keune-Staab
     * puts the source matrix into the target matrix at the location given by rowStart and colStart.
     *
     * @param targetMatrix
     * @param rowStart
     * @param colStart
     * @param sourceMatrix
     * @return
     */
    private boolean setTiles(Tile[][] targetMatrix, int rowStart, int colStart, Tile[][] sourceMatrix) {

        if (sourceMatrix == null || sourceMatrix[0] == null) {
            return false;
        }

        for (int row = rowStart; row < rowStart + sourceMatrix.length; row++) {
            for (int col = colStart; col < colStart + sourceMatrix[0].length; col++) {
                if (targetMatrix.length > row && targetMatrix[row].length > col) {
                    targetMatrix[row][col] = sourceMatrix[row - rowStart][col - colStart];
                }
            }
        }
        return false;
    }

    /**
     * Maike Keune-Staab
     * creates an empty bsp where all points are blocking
     *
     * @param rowNum
     * @param colNum
     * @return
     */
    public TileInfo[][] getEmptyBsp(int rowNum, int colNum) {
        TileInfo[][] bsp = new TileInfo[rowNum][colNum];
        for (int row = 0; row < bsp.length; row++) {
            bsp[row] = new TileInfo[colNum];
            for (int col = 0; col < bsp[row].length; col++) {
                bsp[row][col] = TileInfo.BLOCKING;
            }
        }
        return bsp;
    }

    /**
     * Maike Keune-Staab
     * creates a random bsp recursively by splitting the given bsp into two blocks, altering between horizontal and
     * vertical splits. This is the preparation for the paths which will connect each split block pair.
     * If the given bsp block is too small to be split any further, depending on the parameters isStart or isEnd, a room
     * tile info will be set into the middle.
     *
     * @param bsp
     * @param splitHorizontally
     * @param isStart
     * @param isEnd
     * @param dungeonDetails
     * @return
     */
    private TileInfo[][] createRandomBsp(TileInfo[][] bsp, boolean splitHorizontally, boolean isStart, boolean isEnd,
                                         DungeonDetails dungeonDetails) {
        int width = bsp[0].length;
        int height = bsp.length;

        int blockMinRows = dungeonDetails.getBlockMinRows();
        int blockMinCols = dungeonDetails.getBlockMinCols();
        int pathWidth = dungeonDetails.getPathWidth();

        if (height < blockMinRows * 2 || width < blockMinCols * 2) {
            if (width > 0 && height > 0) {
                if (isStart) {
                    bsp[height / 2][width / 2] = TileInfo.START_POS;
                } else if (isEnd) {
                    bsp[height / 2][width / 2] = TileInfo.END_POS;
                } else {
                    bsp[height / 2][width / 2] = TileInfo.ROOM_POS;
                }
            }
            return bsp;
        }

        TileInfo[][] block1;
        TileInfo[][] block2;

        // calc the random factor
        double rndSum = 0.0;
        for (int i = 0; i < NORM_FACTOR; i++) {
            rndSum += Math.random();
        }
        double rnd = rndSum / NORM_FACTOR;
        int split;

        if (splitHorizontally) {
            split = (int) (width * rnd);
            split = split < blockMinCols ? blockMinCols : split;
            split = split > width - blockMinCols ? width - blockMinCols : split;
            block1 = getEmptyBsp(height, split);
            block2 = getEmptyBsp(height, width - split);
        } else {
            split = (int) (height * rnd);
            split = split < blockMinRows ? blockMinRows : split;
            split = split > height - blockMinRows ? height - blockMinRows : split;
            block1 = getEmptyBsp(split, width);
            block2 = getEmptyBsp(height - split, width);
        }

        block1 = createRandomBsp(block1, !splitHorizontally, isStart, false, dungeonDetails);
        block2 = createRandomBsp(block2, !splitHorizontally, false, isEnd, dungeonDetails);

        if (splitHorizontally) {
            for (int row = 0; row < height; ++row) {
                for (int col = 0; col < width; ++col) {
                    //if the current coordinate lies on the path between the first and the second block, its walkable (bsp =true)
                    boolean notAbovePath = row >= (height / 2) - pathWidth / 2;
                    boolean notBelowPath = row < (height / 2) + pathWidth / 2;
                    boolean leftOfPath = col < split / 2;
                    boolean rightOfPath = col >= width - (width - split) / 2;

                    // if before split and what was found in block 1 is a room
                    if (col < split && isRoom(block1[row][col])) {
                        // take over room of block 1
                        bsp[row][col] = block1[row][col];
                    }
                    // else if after split and what was found in block 2 is a room
                    else if (col >= split && isRoom(block2[row][col - split])) {
                        // take over room of block 2
                        bsp[row][col] = block2[row][col - split];
                    }
                    // else if position is right on a path between the two blocks
                    else if (notAbovePath && notBelowPath && !leftOfPath && !rightOfPath) {
                        // draw the way
                        bsp[row][col] = TileInfo.WALKABLE;
                    }
                    // if before split
                    else if (col < split) {
                        // take over tile info of block 1
                        bsp[row][col] = block1[row][col];
                    }
                    // if after split
                    else {
                        // take over tile info of block 2
                        bsp[row][col] = block2[row][col - split];
                    }
                }
            }
        } else {
            for (int row = 0; row < height; ++row) {
                for (int col = 0; col < width; ++col) {
                    //if the current coordinate lies on the path between the first and the second block, its walkable (bsp =true)
                    boolean notLeftOfPath = col >= (width / 2) - pathWidth / 2;
                    boolean notRightOfPath = col < (width / 2) + pathWidth / 2;
                    boolean abovePath = row < split / 2;
                    boolean belowPath = row >= height - (height - split) / 2;

                    if (row < split && isRoom(block1[row][col])) {
                        bsp[row][col] = block1[row][col];
                    } else if (row >= split && isRoom(block2[row - split][col])) {
                        bsp[row][col] = block2[row - split][col];
                    } else if (notLeftOfPath && notRightOfPath && !abovePath && !belowPath) {
                        bsp[row][col] = TileInfo.WALKABLE;
                    } else if (row < split) {
                        bsp[row][col] = block1[row][col];
                    } else {
                        bsp[row][col] = block2[row - split][col];
                    }
                }
            }
        }
        return bsp;
    }

    /**
     * Maike Keune-Staab
     *
     * @param tileInfo
     * @return
     */
    private boolean isRoom(TileInfo tileInfo) {
        return TileInfo.START_POS.equals(tileInfo) || TileInfo.END_POS.equals(tileInfo)
                || TileInfo.ROOM_POS.equals(tileInfo);
    }

    /**
     * Maike Keune-Staab
     * creates a 3x2 grid of worldmap fields (.json files) randomly, that will be used as the games worldmap.
     *
     * @return
     */
    public Field generateWorldMap() {
        Tile[][] groundTiles = new Tile[60][80];

        for (int row = 0; row < groundTiles.length; row++) {
            for (int col = 0; col < groundTiles[0].length; col++) {
                groundTiles[row][col] = new Tile(0, 0, true, false, CS_OUTSIDE_1);
            }
        }

        Tile[][] decoTiles = new Tile[60][80];

        for (int row = 0; row < decoTiles.length; row++) {
            for (int col = 0; col < decoTiles[0].length; col++) {
                decoTiles[row][col] = new Tile(7, 3, false, true, CS_OUTSIDE_1);
            }
        }
        // load six world map chunks of which each must have the size 20*20
        List<String> mapChunkNames = new ArrayList<>(Arrays.asList(
                "forest_with_camp", "forest_with_lake", "desert_camp", "iceland_and_graveyard", "icy_land", "stone_road"));

        Collection<StartPoint> startPoints = new ArrayList<>();
        Collection<TeleportPoint> teleportPoints = new ArrayList<>();

        int row = 10;
        int col = 10;

        do {
            int randomNumber = (int) (Math.random() * mapChunkNames.size());
            String mapName = mapChunkNames.get(randomNumber);
            mapChunkNames.remove(randomNumber);
            Field field = worldService.getField(mapName);

            setTiles(groundTiles, row, col, field.getGroundTiles());
            setTiles(decoTiles, row, col, field.getDecoTiles());

            for (StartPoint startPoint : field.getStartPoints()) {
                startPoints.add(new StartPoint(col + startPoint.getX(), row + startPoint.getY(),
                        startPoint.getName()));
            }
            for (TeleportPoint teleportPoint : field.getTeleportPoints()) {
                teleportPoints.add(new TeleportPoint(teleportPoint.isRandom(), col + teleportPoint.getX(),
                        row + teleportPoint.getY(), teleportPoint.getName(), teleportPoint.getTargetField(),
                        teleportPoint.getTargetPoint(), teleportPoint.getSourceField(),
                        teleportPoint.getSourcePoint(), teleportPoint.getRandomHeight(),
                        teleportPoint.getRandomWidth(), teleportPoint.getRandomFloors(),
                        teleportPoint.getRandomType()));
            }
            if (col < 50) {
                col += 20;
            } else {
                col = 10;
                row += 20;
            }
        } while (mapChunkNames.size() > 0);


        Field field = new Field(groundTiles, decoTiles);
        field.getStartPoints().addAll(startPoints);
        field.getTeleportPoints().addAll(teleportPoints);
        return field;
    }
}
