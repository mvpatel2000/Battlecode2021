package scoutplayer;

import battlecode.common.*;
import java.util.*;

public class Politician extends Unit {

    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTH = {{1,4},{-1,4},{2,4},{-2,4},{0,5},{3,4},{4,3},{5,0},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHEAST = {{0,4},{4,0},{1,4},{4,1},{3,3},{2,4},{4,2},{4,-2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_EAST = {{4,1},{4,-1},{4,2},{4,-2},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5}};
    final static int[][] NEW_SENSED_LOCS_SOUTHEAST = {{4,0},{0,-4},{4,-1},{1,-4},{3,-3},{4,2},{4,-2},{2,-4},{-2,-4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4}};
    final static int[][] NEW_SENSED_LOCS_SOUTH = {{1,-4},{-1,-4},{2,-4},{-2,-4},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0}};
    final static int[][] NEW_SENSED_LOCS_SOUTHWEST = {{0,-4},{-4,0},{-1,-4},{-4,-1},{-3,-3},{2,-4},{-2,-4},{-4,-2},{-4,2},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3}};
    final static int[][] NEW_SENSED_LOCS_WEST = {{-4,-1},{-4,1},{-4,-2},{-4,2},{0,5},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHWEST = {{0,4},{-4,0},{-4,1},{-1,4},{-3,3},{2,4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_CENTER = {};

    MapTerrainQueue mtq;

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
        mtq = new MapTerrainQueue();
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        if (considerAttack()) {
            System.out.println("Attacking!");
        } else if (mtq.hasRoom() && tryMove(randomDirection())) { // move if queue isn't full
            System.out.println("I moved "+moveThisTurn.toString()+" to "+rc.getLocation().toString());
        } else if (!mtq.hasRoom()) {
            System.out.println("MapTerrainQueue full; not moving this round.");
        }
        mtq.step(rc, moveThisTurn, rc.getLocation());
        MapTerrainFlag mtf = new MapTerrainFlag();
        mtf.setLastMove(moveThisTurn);
        for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
            if (mtq.isEmpty()) break;
            MapTerrain terrain = mtq.pop();
            mtf.addPassability(terrain.pa);
            System.out.println("Added to flag: " + terrain.loc.toString() + " has passability " + terrain.pa);
        }
        rc.setFlag(mtf.getFlag());
    }

    /*
     * Returns newly sensable locations relative to the current location
     * after a move in direction lastMove.
     */
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

    /*
     * Analyzes if politician should attack. Returns true if it attacked. Sorts nearbyRobots and
     * considers various ranges of empowerment to optimize kills.
     */
    public boolean considerAttack() throws GameActionException {
        if (!rc.isReady()) {
            return false;
        }
        Arrays.sort(nearbyRobots, new Comparator<RobotInfo>() {
            public int compare(RobotInfo r1, RobotInfo r2) {
                // Intentional: Reverse order for this demo
                int d1 = currLocation.distanceSquaredTo(r1.location);
                int d2 = currLocation.distanceSquaredTo(r2.location);
                return d1 - d2;
            }
        });
        int[] distanceSquareds = new int[nearbyRobots.length];
        for (int i = 0; i < nearbyRobots.length; i++) {
            distanceSquareds[i] = currLocation.distanceSquaredTo(nearbyRobots[i].location);
        }
        int optimalNumEnemiesKilled = 0;
        int optimalDist = -1;
        double totalDamage = rc.getConviction() * rc.getEmpowerFactor(allyTeam, 0) - 10;
        int maxDist = rc.getType().actionRadiusSquared;
        // Loop over subsets of nearbyRobots
        for (int i = 1; i <= nearbyRobots.length; i++) {
            // Skip over subsets which don't actually change range
            if (i != nearbyRobots.length && distanceSquareds[i-1] == distanceSquareds[i]) {
                continue;
            }
            // Remaining units are out of range, break.
            if (distanceSquareds[i-1] >= maxDist) {
                break;
            }
            // Count number of units to kill
            int perUnitDamage = (int)(totalDamage / i);
            int numEnemiesKilled = 0;
            for (int j = 0; j < i; j++) {
                RobotInfo robot = nearbyRobots[j];
                if (robot.team == enemyTeam && perUnitDamage > robot.conviction) {
                    numEnemiesKilled++;
                }
            }
            if (numEnemiesKilled > optimalNumEnemiesKilled) {
                optimalNumEnemiesKilled = numEnemiesKilled;
                optimalDist = distanceSquareds[i-1];
            }
        }
        if (optimalNumEnemiesKilled > 0 && rc.canEmpower(optimalDist)) {
            rc.empower(optimalDist);
        }
        return false;
    }
}