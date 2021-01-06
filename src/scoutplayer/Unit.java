package scoutplayer;

import battlecode.common.*;

public abstract class Unit extends Robot {
    
    // Base EnlightmentCenter information
    MapLocation baseLocation;
    int baseID;

    RobotInfo[] nearbyRobots;
    Direction lastMove;

    FlagHandler[] flagHandlers;

    int[][] SENSE_SPIRAL_ORDER;
    int[][] NEW_SENSED_LOCS_NORTH;
    int[][] NEW_SENSED_LOCS_NORTHEAST;
    int[][] NEW_SENSED_LOCS_EAST;
    int[][] NEW_SENSED_LOCS_SOUTHEAST;
    int[][] NEW_SENSED_LOCS_SOUTH;
    int[][] NEW_SENSED_LOCS_SOUTHWEST;
    int[][] NEW_SENSED_LOCS_WEST;
    int[][] NEW_SENSED_LOCS_NORTHWEST;
    final int[][] NEW_SENSED_LOCS_CENTER = {};

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        lastMove = Direction.CENTER;
        // Add base information
        RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, allyTeam);
        for (RobotInfo robot : adjacentRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                baseLocation = robot.location;
                baseID = robot.ID;
            }
        }
    }

    @Override
    public void run() throws GameActionException {
        super.run();
    }

    public void parseVision() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
    }

    public void updateFlagHandlers(boolean beforeMove) throws GameActionException {
        for (FlagHandler flagHandler : flagHandlers) {
            flagHandler.update(beforeMove);
        }
    }

    // Returns newly sensable locations relative to the current location
    // after a move in direction lastMove.
    public int[][] newSensedLocationsRelative(Direction lastMove) throws GameActionException {
        switch (lastMove) {
            case NORTH:
                return NEW_SENSED_LOCS_NORTH;
            case NORTHEAST:
                return NEW_SENSED_LOCS_NORTHEAST;
            case EAST:
                return NEW_SENSED_LOCS_EAST;
            case SOUTHEAST:
                return NEW_SENSED_LOCS_SOUTHEAST;
            case SOUTH:
                return NEW_SENSED_LOCS_SOUTH;
            case SOUTHWEST:
                return NEW_SENSED_LOCS_SOUTHWEST;
            case WEST:
                return NEW_SENSED_LOCS_WEST;
            case NORTHWEST:
                return NEW_SENSED_LOCS_NORTHWEST;
            default:
                return NEW_SENSED_LOCS_CENTER;
        }
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            lastMove = dir;
            return true;
        }
        lastMove = Direction.CENTER;
        return false;
    }
}