package scoutplayer;

import battlecode.common.*;

public class Muckraker extends Unit {
    MapLocation destination;

    public Muckraker(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Delete! Hard coded destination for testing
        if (allyTeam == Team.A) {
            destination = baseLocation.translate(40, 40);
        } else {
            destination = baseLocation.translate(-40, -40);
        }
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        // Search for nearest slanderer. If one exists, kill it or move towards it.
        if (rc.isReady()) {
            RobotInfo nearestSlanderer = null;
            int nearestSlandererDistSquared = 100;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = currLocation.distanceSquaredTo(robot.location);
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
                fuzzyMove(destination);
            }
        }
    }
}