package jefferson;

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

    public final static int INITIAL_COOLDOWN = 0;

    // Tracks nearest enemy so we don't constantly update destination from same unit
    public MapLocation lastNearestLocation;

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
        // if no destination was provided from parent EC, set one:
        if (spawnedSilently) {
            Direction away = myLocation.directionTo(baseLocation).opposite();
            destination = myLocation.add(away).add(away).add(away);
        }
        lastNearestLocation = myLocation;
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // If you turn into politician, convert into politician.
        if (rc.getType() == RobotType.POLITICIAN) {
            isSlandererConvertedToPolitician = true;
            return;
        }

        // Run away from nearest Muckraker.
        if (rc.isReady()) {
            RobotInfo nearestMuckraker = null;
            int nearestMuckrakerDistSquared = 1000;
            // RobotInfo nearestEnemy = null;
            // int nearestEnemyDistSquared = 1000;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type == RobotType.MUCKRAKER && robotDistSquared < nearestMuckrakerDistSquared) {
                    nearestMuckraker = robot;
                    nearestMuckrakerDistSquared = robotDistSquared;
                }
                // if (robotDistSquared < nearestEnemyDistSquared) {
                //     nearestEnemy = robot;
                //     nearestEnemyDistSquared = robotDistSquared;
                // }
            }
            if (nearestMuckraker != null && !lastNearestLocation.equals(nearestMuckraker.location)) {
                // Flee from nearest Muckraker.
                fleeDestination(nearestMuckraker.location);
            } 
            // Move towards nearest non-muckraker enemy. 
            // Disabled because enemy politicians push us into enemy muckrakers.
            // else if (nearestEnemy != null) {
            //     int diffX = myLocation.x - nearestEnemy.location.x;
            //     int diffY = myLocation.y - nearestEnemy.location.y;
            //     destination = myLocation.translate(diffX*2, diffY*2);
            // } 
            else {
                //parseVision();
                RobotInfo nearestSignalRobot = getNearestEnemyFromAllies();
                if (nearestSignalRobot != null && nearestSignalRobot.type == RobotType.MUCKRAKER
                    && !lastNearestLocation.equals(nearestSignalRobot.location)) {
                    fleeDestination(nearestSignalRobot.location);
                }
            }
            slandererMove();
        }
    }

    /**
     * Update destination to flee from enemy
     * @param flee
     * @throws GameActionException
     */
    public void fleeDestination(MapLocation flee) throws GameActionException {
        int diffX = Math.max(Math.min((myLocation.x - flee.x)*2, 5), -5);
        int diffY = Math.max(Math.min((myLocation.y - flee.y)*2, 5), -5);
        destination = myLocation.translate(diffX, diffY);
        lastNearestLocation = flee;
    }

    /**
     * Moves to destination. Readjusts destination if destination is off the map to avoid fuzzyMove
     * leading to movement on map edge.
     * @throws GameActionException
     */
    public void slandererMove() throws GameActionException {
        // Turn off right now. Instead set max on translation
        // Direction toDest = myLocation.directionTo(destination);
        // MapLocation nearDestination = myLocation.add(toDest).add(toDest).add(toDest);
        // // Near destination off map, pick a closer one to stop at
        // if (!rc.onTheMap(nearDestination)) {
        //     MapLocation twoStep = myLocation.add(toDest).add(toDest);
        //     MapLocation oneStep = myLocation.add(toDest);
        //     if (rc.onTheMap(twoStep)) {
        //         destination = twoStep;
        //     } else if (rc.onTheMap(oneStep)) {
        //         destination = oneStep;
        //     } else {
        //         destination = myLocation;
        //     }
        // } 
        fuzzyMove(destination);
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
