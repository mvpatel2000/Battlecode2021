package fdr;

import battlecode.common.*;
import java.util.*;

public class Muckraker extends Unit {

    public final static int INITIAL_COOLDOWN = 10;
    
    public Muckraker(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        updateDestinationForExploration(false);

        // Search for nearest slanderer. If one exists, kill it or move towards it.
        if (rc.isReady()) {
            if (!denyNeutralEC()) {
                huntSlanderersOrToDestination();
            }
        }
    }

    /**
     * Dilute enemy politicians when they try to capture neutral EC.
     * Returns true if it took an action.
     * @throws GameActionException
     */
    boolean denyNeutralEC() throws GameActionException {
        RobotInfo[] nearbyNeutrals = rc.senseNearbyRobots(RobotType.MUCKRAKER.sensorRadiusSquared, neutralTeam);
        // No nearby neutral units
        if (nearbyNeutrals.length == 0) {
            return false;
        }
        RobotInfo nearestNeutralEC = null;
        int nearestNeutralECDistance = 100000;
        for (RobotInfo robot : nearbyNeutrals) {
            int distance = robot.location.distanceSquaredTo(myLocation);
            if (distance < nearestNeutralECDistance) {
                nearestNeutralECDistance = distance;
                nearestNeutralEC = robot;
            }
        }
        // No nearby neutral ECs
        if (nearestNeutralEC == null) {
            return false;
        }
        for (RobotInfo robot : nearbyAllies) {
            // Another muckraker has EC covered
            if (robot.type == RobotType.MUCKRAKER && robot.location.distanceSquaredTo(nearestNeutralEC.location) <= 2) {
                return false;
            }
            // Ally policitian attempts to take EC
            if (robot.type == RobotType.POLITICIAN && robot.location.distanceSquaredTo(nearestNeutralEC.location) <= 2) {
                return false;
            }
        }
        // Nearest enemy politician
        RobotInfo nearestEnemyPolitician = null;
        int nearestEnemyPoliticianDistance = 100000;
        for (RobotInfo robot : nearbyEnemies) {
            int distance = robot.location.distanceSquaredTo(nearestNeutralEC.location);
            if (robot.type == RobotType.POLITICIAN && distance < nearestEnemyPoliticianDistance) {
                nearestEnemyPoliticianDistance = distance;
                nearestEnemyPolitician = robot;
            }
        }
        // No enemy politicians
        if (nearestEnemyPolitician == null) {
            // Don't move, already adjacent
            if (nearestNeutralECDistance > 2) {
                fuzzyMove(nearestNeutralEC.location);
            }
        } 
        else {
            // Redefine as unchanging variable for compiler
            MapLocation nearestEnemyPoliticianLocation = nearestEnemyPolitician.location;
            int nearestEnemyPoliticianDistanceFinal = nearestEnemyPoliticianDistance;
            MapLocation nearestNeutralECLocation = nearestNeutralEC.location;
            Arrays.sort(allDirections, new Comparator<Direction>() {
                public int compare(Direction d1, Direction d2) {
                    MapLocation m1 = myLocation.add(d1);
                    MapLocation m2 = myLocation.add(d2);
                    int cost1 = m1.distanceSquaredTo(nearestEnemyPoliticianLocation) <= nearestEnemyPoliticianDistanceFinal ? 0 : 100000; 
                    int cost2 = m2.distanceSquaredTo(nearestEnemyPoliticianLocation) <= nearestEnemyPoliticianDistanceFinal ? 0 : 100000;
                    cost1 += m1.distanceSquaredTo(nearestNeutralECLocation); 
                    cost2 += m2.distanceSquaredTo(nearestNeutralECLocation); 
                    return cost1 - cost2;
                }
            });
            for (Direction dir : allDirections) {
                // Stay put
                if (dir == Direction.CENTER) {
                    break;
                }
                else if (rc.canMove(dir)) {
                    rc.move(dir);
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Search for nearest slanderer. If one exists, kill it or move towards it.
     * @throws GameActionException
     */
    void huntSlanderersOrToDestination() throws GameActionException {
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
                fuzzyMove(nearestSlanderer.location);
            }
        } else {
            // Continue towards destination
            // Consider using weightedFuzzyMove
            fuzzyMove(destination);
        }
    }
}