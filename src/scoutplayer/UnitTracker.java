package scoutplayer;

import battlecode.common.*;

/**
 * Used by ECs to track units roaming around.
 */
public class UnitTracker {
    
    int robotID;
    MapLocation currLoc;
    EnlightmentCenter ec;
    int turnCount;
    boolean alive;
    RobotType type;
    Direction lastMove;
    int flagInt;
    
    public UnitTracker(EnlightmentCenter ec, RobotType type, int idToTrack, MapLocation spawnLoc) {
        robotID = idToTrack;
        this.ec = ec;
        currLoc = spawnLoc;
        turnCount = 1;
        alive = true;
        this.type = type;
        lastMove = Direction.CENTER;
        flagInt = 0;
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * Returns:
     * * if the robot is dead/inactive, -1
     * * otherwise, the received flag schema
     */
    public int update() throws GameActionException {
        turnCount++;
        if (!alive || !ec.rc.canGetFlag(robotID)) {
            alive = false;
            return -1;
        }
        flagInt = ec.rc.getFlag(robotID);
        lastMove = getLastMoveFromFlag(flagInt);
        currLoc = currLoc.add(lastMove);
        return Flag.getSchema(flagInt);
    }

    public Direction getLastMoveFromFlag(int flag) {
        switch (Flag.getSchema(flag)) {
            case Flag.MAP_TERRAIN_SCHEMA:
                MapTerrainFlag mtf = new MapTerrainFlag(flag);
                return mtf.readLastMove();
            case Flag.EC_SIGHTING_SCHEMA:
                ECSightingFlag ecsf = new ECSightingFlag(flag);
                return ecsf.readLastMove();
            default:
                UnitFlag uf = new UnitFlag(flag);
                return uf.readLastMove();
        }
    }
}
