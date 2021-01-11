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
        // System.out.println("I am a UnitTracker tracking " + type.toString() + " at " + spawnLoc.toString() + " with ID " + idToTrack);
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * Returns -1 if the robot is dead/inactive, and otherwise
     * returns the schema of the received flag.
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
        // System.out.println("Robot with ID " + robotID + " is now at " + currLoc.toString());
        // System.out.println("Last move: " + lastMove.toString());
        // System.out.println("Flag seen: " + flagInt);

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
            case Flag.UNIT_UPDATE_SCHEMA:
                UnitFlag uf = new UnitFlag(flag);
                return uf.readLastMove();
            default:
                return Direction.CENTER;
        }
    }
}
