package sprintplayer;

import battlecode.common.*;

public class Slanderer extends Unit {
    MapLocation destination;

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Delete! Hard coded destination for testing
        if (allyTeam == Team.A) {
            destination = baseLocation.translate(0, 0);
        } else {
            destination = baseLocation.translate(0, 0);
        }
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        // Run away from nearest Muckraker.
        if (rc.isReady()) {
            MapLocation myLocation = rc.getLocation();
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemyTeam);
            RobotInfo nearestMuckraker = null;
            int nearestMuckrakerDistSquared = 100;
            for (RobotInfo robot : nearbyRobots) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type == RobotType.MUCKRAKER && robotDistSquared < nearestMuckrakerDistSquared) {
                    nearestMuckraker = robot;
                    nearestMuckrakerDistSquared = robotDistSquared;
                }
            }
            if (nearestMuckraker != null) {
                // Flee from nearest Muckraker.
                MapLocation fleeLocation = myLocation.add(myLocation.directionTo(nearestMuckraker.location).opposite());
                fuzzyMove(fleeLocation);
            } else {
                // Continue towards destination
                fuzzyMove(destination);
            }
        }
    }
}