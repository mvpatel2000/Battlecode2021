package smite;

import battlecode.common.*;

public class Muckraker extends Unit {

    public final static int INITIAL_COOLDOWN = 10;
    
    public Muckraker(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        super.run();

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
                    weightedFuzzyMove(nearestSlanderer.location);
                }
            } else {
                // Continue towards destination
                weightedFuzzyMove(destination);
            }
        }
        if (!flagSetThisRound) {
            setFlag((new UnitFlag(moveThisTurn)).flag);
        }
    }

    /**
     * Moves towards destination, prefering high passability terrain. Repels from nearby muckrakers
     * to avoid clustering after initial few turns.
     */
    void weightedFuzzyMove(MapLocation destination) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(), 
            toDest.rotateRight().rotateRight(), toDest.opposite().rotateLeft(), toDest.opposite().rotateRight(), toDest.opposite()};
        double[] costs = new double[8];
        // Ignore repel factor in beginning and when close to target
        boolean shouldRepel = turnCount > 50 && myLocation.distanceSquaredTo(destination) > 40;
        for (int i = 0; i < dirs.length; i++) {
            MapLocation newLocation = myLocation.add(dirs[i]);
            // Movement invalid, set higher cost than starting value
            if (!rc.onTheMap(newLocation)) {
                costs[i] = -999999;
                continue;
            }
            double cost = (rc.sensePassability(newLocation) - 1) * 60;
            // Preference tier for moving towards target
            if (i >= 3) {
                cost -= 60;
            }
            if (shouldRepel) {
                for (RobotInfo robot : nearbyAllies) {
                    cost -= 40 - newLocation.distanceSquaredTo(robot.location);
                }
            }
            costs[i] = cost;
        }
        double cost = -99999;
        Direction optimalDir = null;
        for (int i = 0; i < dirs.length; i++) {
            Direction dir = dirs[i];
            // //System.out.println\(dir + " " + rc.canMove(dir) + " " + costs[i]);
            if (rc.canMove(dir)) {
                double newCost = costs[i];
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