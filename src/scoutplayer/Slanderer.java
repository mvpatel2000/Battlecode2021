package scoutplayer;

import battlecode.common.*;

public class Slanderer extends Unit {

    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4}};
    final static int[][] NEW_SENSED_LOCS_NORTH = {{0,4},{1,4},{-1,4},{3,3},{-3,3},{2,4},{4,2},{-4,2},{-2,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHEAST = {{2,3},{3,2},{0,4},{4,0},{1,4},{4,1},{4,-1},{-1,4},{3,3},{2,4},{4,2},{4,-2},{-2,4}};
    final static int[][] NEW_SENSED_LOCS_EAST = {{4,0},{4,1},{4,-1},{3,3},{3,-3},{2,4},{4,2},{4,-2},{2,-4}};
    final static int[][] NEW_SENSED_LOCS_SOUTHEAST = {{3,-2},{2,-3},{4,0},{0,-4},{4,1},{4,-1},{1,-4},{-1,-4},{3,-3},{4,2},{4,-2},{2,-4},{-2,-4}};
    final static int[][] NEW_SENSED_LOCS_SOUTH = {{0,-4},{1,-4},{-1,-4},{3,-3},{-3,-3},{4,-2},{2,-4},{-2,-4},{-4,-2}};
    final static int[][] NEW_SENSED_LOCS_SOUTHWEST = {{-2,-3},{-3,-2},{0,-4},{-4,0},{1,-4},{-1,-4},{-4,-1},{-4,1},{-3,-3},{2,-4},{-2,-4},{-4,-2},{-4,2}};
    final static int[][] NEW_SENSED_LOCS_WEST = {{-4,0},{-4,-1},{-4,1},{-3,-3},{-3,3},{-2,-4},{-4,-2},{-4,2},{-2,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHWEST = {{-3,2},{-2,3},{0,4},{-4,0},{1,4},{-4,-1},{-4,1},{-1,4},{-3,3},{2,4},{-4,-2},{-4,2},{-2,4}};
    final static int[][] NEW_SENSED_LOCS_CENTER = {};

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        // Run away from nearest Muckraker.
        if (rc.isReady()) {
            RobotInfo nearestMuckraker = null;
            int nearestMuckrakerDistSquared = 100;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type == RobotType.MUCKRAKER && robotDistSquared < nearestMuckrakerDistSquared) {
                    nearestMuckraker = robot;
                    nearestMuckrakerDistSquared = robotDistSquared;
                }
            }
            if (nearestMuckraker != null) {
                // Flee from nearest Muckraker.
                MapLocation fleeLocation = myLocation.add(myLocation.directionTo(nearestMuckraker.location).opposite());
                fuzzyMove(fleeLocation);
            } else {
                // Continue towards destination
                fuzzyMove(destination);
            }
        }
    }

    // Returns newly sensable locations relative to the current location
    // after a move in direction lastMove.
    public static int[][] newSensedLocationsRelative(Direction lastMove) throws GameActionException {
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
}