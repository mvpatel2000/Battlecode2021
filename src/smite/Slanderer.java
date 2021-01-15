package smite;

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

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
        // if no destination was provided from parent EC, set one:
        if (spawnedSilently) {
            Direction away = myLocation.directionTo(baseLocation).opposite();
            destination = myLocation.add(away).add(away).add(away);
        }
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // If you turn into politician, suicide for now
        // TODO: Listen to spawn messages from EC to learn new dest
        if (rc.getType() == RobotType.POLITICIAN) {
            if (rc.canEmpower(1)) {
                rc.empower(1);
            }
        }

        // Run away from nearest Muckraker.
        if (rc.isReady()) {
            RobotInfo nearestMuckraker = null;
            int nearestMuckrakerDistSquared = 1000;
            RobotInfo nearestEnemy = null;
            int nearestEnemyDistSquared = 1000;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type == RobotType.MUCKRAKER && robotDistSquared < nearestMuckrakerDistSquared) {
                    nearestMuckraker = robot;
                    nearestMuckrakerDistSquared = robotDistSquared;
                }
                if (robotDistSquared < nearestEnemyDistSquared) {
                    nearestEnemy = robot;
                    nearestEnemyDistSquared = robotDistSquared;
                }
            }
            if (nearestMuckraker != null) {
                // Flee from nearest Muckraker.
                int diffX = myLocation.x - nearestMuckraker.location.x;
                int diffY = myLocation.y - nearestMuckraker.location.y;
                destination = myLocation.translate(diffX, diffY);
            } else if (nearestEnemy != null) {
                int diffX = myLocation.x - nearestEnemy.location.x;
                int diffY = myLocation.y - nearestEnemy.location.y;
                destination = myLocation.translate(diffX, diffY);
            } else {
                RobotInfo nearestSignalRobot = getNearestEnemyFromAllies();
                if (nearestSignalRobot != null) {
                    int diffX = myLocation.x - nearestSignalRobot.location.x;
                    int diffY = myLocation.y - nearestSignalRobot.location.y;
                    destination = myLocation.translate(diffX, diffY);
                }
            }
            wideFuzzyMove(destination);
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