package sprintplayer;

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
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    // System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        fuzzyMove(destination);
    }
}