package taft;

import battlecode.common.*;

public class Slanderer extends Unit {

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
}
