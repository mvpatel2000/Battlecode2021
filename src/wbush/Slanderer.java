package wbush;

import battlecode.common.*;

public class Slanderer extends Unit {

    final static int[][] SLANDERER_SPIRAL_ORDER = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{0,3},{3,0},{0,-3},{-3,0},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4},{2,5},{5,2},{5,-2},{2,-5},{-2,-5},{-5,-2},{-5,2},{-2,5},{1,6},{6,1},{6,-1},{1,-6},{-1,-6},{-6,-1},{-6,1},{-1,6},{4,5},{5,4},{5,-4},{4,-5},{-4,-5},{-5,-4},{-5,4},{-4,5},{3,6},{6,3},{6,-3},{3,-6},{-3,-6},{-6,-3},{-6,3},{-3,6},{0,7},{7,0},{0,-7},{-7,0},{2,7},{7,2},{7,-2},{2,-7},{-2,-7},{-7,-2},{-7,2},{-2,7},{5,6},{6,5},{6,-5},{5,-6},{-5,-6},{-6,-5},{-6,5},{-5,6},{1,8},{4,7},{7,4},{8,1},{8,-1},{7,-4},{4,-7},{1,-8},{-1,-8},{-4,-7},{-7,-4},{-8,-1},{-8,1},{-7,4},{-4,7},{-1,8},{3,8},{8,3},{8,-3},{3,-8},{-3,-8},{-8,-3},{-8,3},{-3,8},{0,9},{9,0},{0,-9},{-9,0},{2,9},{6,7},{7,6},{9,2},{9,-2},{7,-6},{6,-7},{2,-9},{-2,-9},{-6,-7},{-7,-6},{-9,-2},{-9,2},{-7,6},{-6,7},{-2,9},{5,8},{8,5},{8,-5},{5,-8},{-5,-8},{-8,-5},{-8,5},{-5,8}};

    int indexInSpiralOrder;
    int returnToPositionCooldown;

    public final static int INITIAL_COOLDOWN = 0;

    // Tracks nearest enemy so we don't constantly update destination from same unit
    public MapLocation lastNearestLocation;

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
        // IGNORE EC DESTINATION FOR NOW!
        // if no destination was provided from parent EC, set one:
        // if (spawnedSilently) {
        //     Direction away = myLocation.directionTo(baseLocation).opposite();
        //     destination = myLocation.add(away).add(away).add(away);
        // }
        destination = baseLocation;
        lastNearestLocation = myLocation;
        indexInSpiralOrder = 0;
        returnToPositionCooldown = 0;
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // If you turn into politician, convert into politician.
        if (rc.getType() == RobotType.POLITICIAN) {
            isSlandererConvertedToPolitician = true;
            return;
        }

        // Skip spiral first turn to avoid TLE
        if (turnCount > 1) {
            scanSpiral();
        }
        returnToPositionCooldown--;

        // Run away from nearest Muckraker.
        if (rc.isReady()) {
            RobotInfo nearestMuckraker = getNearestMuckraker();
            // System.out.println("Nearest muck: " + nearestMuckraker);
            // Flee from nearest Muckraker.
            if (nearestMuckraker != null) {
                fleeDestination(nearestMuckraker.location);
            } else {
                RobotInfo nearestSignalRobot = getNearestEnemyFromAllies();
                // System.out.println("Flee: " + nearestSignalRobot);
                if (nearestSignalRobot != null && nearestSignalRobot.type == RobotType.MUCKRAKER
                    && nearestSignalRobot.location.distanceSquaredTo(myLocation) <= 64) {
                    fleeDestination(nearestSignalRobot.location);
                }
            }
            slandererMove();
        }
    }

    /**
     * Gets nearest muckraker in vision radius
     * @return
     * @throws GameActionException
     */
    public RobotInfo getNearestMuckraker() throws GameActionException {
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
        return nearestMuckraker;
    }

    /**
     * Scans spiral locations looking for a spot to fit in
     * @throws GameActionException
     */
    public void scanSpiral() throws GameActionException {
        // System.out.println("Scan Start: " + indexInSpiralOrder);
        while (scanSpiralHelper()) {
            indexInSpiralOrder++;
        }
        // System.out.println("Scan End: " + indexInSpiralOrder);
    }

    /**
     * Checks current index to see if its valid. Returns true if needs to continue scanning.
     * @throws GameActionException
     */
    public boolean scanSpiralHelper() throws GameActionException {
        int dx = SLANDERER_SPIRAL_ORDER[indexInSpiralOrder][0];
        int dy = SLANDERER_SPIRAL_ORDER[indexInSpiralOrder][1];
        MapLocation spiralPlace = baseLocation.translate(dx, dy);
        int distanceToSpiralPlace = myLocation.distanceSquaredTo(spiralPlace);
        // At destination, stay still
        if (distanceToSpiralPlace == 0) {
            return false;
        }
        // System.out.println("Position: " + spiralPlace + " distance: " + distanceToSpiralPlace);
        // Can sense location
        if (distanceToSpiralPlace <= RobotType.SLANDERER.sensorRadiusSquared) {
            // Not on map
            if (!rc.onTheMap(spiralPlace)) {
                return true;
            }
            // System.out.println("Occ: " + rc.isLocationOccupied(spiralPlace));
            // If another slanderer occupies a slot, move on
            if (rc.isLocationOccupied(spiralPlace)) {
                RobotInfo robot = rc.senseRobotAtLocation(spiralPlace);
                // System.out.println("Robot: " + robot);
                // Slanderers cant distinguish slanderers and politicians
                if (robot.type == RobotType.POLITICIAN) {
                    return true;
                }
            }
            // In sensing radius and on map / unoccupied, so we can move towards it
            return false;
        }
        MapLocation nearSpiralPlace = myLocation;
        for (int i = 0; i < 3; i++) {
            Direction toDest = nearSpiralPlace.directionTo(spiralPlace);
            nearSpiralPlace = nearSpiralPlace.add(toDest);
        }
        // System.out.println("Near: " + nearSpiralPlace + " " + rc.onTheMap(nearSpiralPlace));
        // Place near destination is off map so destination is off map
        if (!rc.onTheMap(nearSpiralPlace)) {
            return true;
        }
        // Can't sense it yet, keep moving!
        return false;
    }

    /**
     * Update destination to flee from enemy
     * @param flee
     * @throws GameActionException
     */
    public void fleeDestination(MapLocation flee) throws GameActionException {
        // System.out.println("FLEEING! " + flee + " " + lastNearestLocation);
        if (lastNearestLocation.equals(flee)) {
            return;
        }
        int diffX = Math.max(Math.min((myLocation.x - flee.x)*2, 5), -5);
        int diffY = Math.max(Math.min((myLocation.y - flee.y)*2, 5), -5);
        destination = myLocation.translate(diffX, diffY);
        lastNearestLocation = flee;
        returnToPositionCooldown = 30;
        // System.out.println("STARTED FLEEING! Destination: " + destination + " Cooldown: " + returnToPositionCooldown);
    }

    /**
     * Moves to destination. Readjusts destination if destination is off the map to avoid slandererFuzzyMove
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
        // System.out.println("Destination: " + destination + " Cooldown: " + returnToPositionCooldown);

        // Fleeing, run away to destination
        if (returnToPositionCooldown >= 1) {
            slandererFuzzyMove(destination);
        } else {
            int dx = SLANDERER_SPIRAL_ORDER[indexInSpiralOrder][0];
            int dy = SLANDERER_SPIRAL_ORDER[indexInSpiralOrder][1];
            MapLocation spiralPlace = baseLocation.translate(dx, dy);
            // System.out.println("Position Move: " + spiralPlace);
            slandererFuzzyMove(spiralPlace);
        }
    }

    /**
     * Moves towards destination, in the optimal direction or diagonal offsets based on which is
     * cheaper to move through. Assumes rc.isReady() == true, or otherwise wastes bytecode on
     * unnecessary computation. Allows orthogonal moves to unlodge. Penalizes moving around EC.
     */
    void slandererFuzzyMove(MapLocation destination) throws GameActionException {
        // TODO: This is not optimal! Sometimes taking a slower move is better if its diagonal.
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};
        double cost = -1;
        Direction optimalDir = null;
        for (int i = 0; i < dirs.length; i++) {
            // Prefer forward moving steps over horizontal shifts
            if (i > 2 && cost > 0) {
                break;
            }
            Direction dir = dirs[i];
            if (rc.canMove(dir)) {
                double newCost = rc.sensePassability(myLocation.add(dir));
                // add epsilon boost to forward direction
                if (dir == toDest) {
                    newCost += 0.001;
                }
                if (myLocation.add(dir).distanceSquaredTo(baseLocation) <= 2) {
                    newCost -= 0.5;
                }
                if (newCost > cost) {
                    cost = newCost;
                    optimalDir = dir;
                }
            }
        }
        if (optimalDir != null) {
            move(optimalDir);
        }
    }
}
