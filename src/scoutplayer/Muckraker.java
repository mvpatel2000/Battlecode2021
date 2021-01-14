package scoutplayer;

import battlecode.common.*;

public class Muckraker extends Unit {

    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4},{1,5},{5,1},{5,-1},{1,-5},{-1,-5},{-5,-1},{-5,1},{-1,5},{2,5},{5,2},{5,-2},{2,-5},{-2,-5},{-5,-2},{-5,2},{-2,5}};
    final static int[][] NEW_SENSED_LOCS_NORTH = {{0,5},{3,4},{4,3},{-4,3},{-3,4},{1,5},{-1,5},{2,5},{5,2},{-5,2},{-2,5}};
    final static int[][] NEW_SENSED_LOCS_NORTHEAST = {{3,3},{2,4},{4,2},{0,5},{3,4},{4,3},{5,0},{1,5},{5,1},{5,-1},{-1,5},{2,5},{5,2},{5,-2},{-2,5}};
    final static int[][] NEW_SENSED_LOCS_EAST = {{3,4},{4,3},{5,0},{4,-3},{3,-4},{5,1},{5,-1},{2,5},{5,2},{5,-2},{2,-5}};
    final static int[][] NEW_SENSED_LOCS_SOUTHEAST = {{3,-3},{4,-2},{2,-4},{5,0},{4,-3},{3,-4},{0,-5},{5,1},{5,-1},{1,-5},{-1,-5},{5,2},{5,-2},{2,-5},{-2,-5}};
    final static int[][] NEW_SENSED_LOCS_SOUTH = {{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{1,-5},{-1,-5},{5,-2},{2,-5},{-2,-5},{-5,-2}};
    final static int[][] NEW_SENSED_LOCS_SOUTHWEST = {{-3,-3},{-2,-4},{-4,-2},{0,-5},{-3,-4},{-4,-3},{-5,0},{1,-5},{-1,-5},{-5,-1},{-5,1},{2,-5},{-2,-5},{-5,-2},{-5,2}};
    final static int[][] NEW_SENSED_LOCS_WEST = {{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4},{-5,-1},{-5,1},{-2,-5},{-5,-2},{-5,2},{-2,5}};
    final static int[][] NEW_SENSED_LOCS_NORTHWEST = {{-3,3},{-4,2},{-2,4},{0,5},{-5,0},{-4,3},{-3,4},{1,5},{-5,-1},{-5,1},{-1,5},{2,5},{-5,-2},{-5,2},{-2,5}};

    public final static int INITIAL_COOLDOWN = 10;
    
    public Muckraker(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        updateDestinationForExploration();

        // Search for nearest slanderer. If one exists, kill it or move towards it.
        if (rc.isReady()) {
            RobotInfo nearestSlanderer = null;
            int nearestSlandererDistSquared = 100;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type == RobotType.SLANDERER && robotDistSquared < nearestSlandererDistSquared) {
                    nearestSlanderer = robot;
                    nearestSlandererDistSquared = robotDistSquared;
                }
            }
            if (nearestSlanderer != null) {
                // Nearest slanderer exists -- try to kill it or move towards it.
                if (nearestSlandererDistSquared < rc.getType().actionRadiusSquared) {
                    rc.expose(nearestSlanderer.location);
                } else {
                    // Consider using weightedFuzzyMove
                    fuzzyMove(nearestSlanderer.location);
                }
            } else {
                // Continue towards destination
                // Consider using weightedFuzzyMove
                fuzzyMove(destination);
            }
        }
    }
}