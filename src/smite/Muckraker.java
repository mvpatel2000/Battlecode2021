package smite;

import battlecode.common.*;

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
}