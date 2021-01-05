package sprintplayer;

import battlecode.common.*;

public abstract class Unit extends Robot {
    // Base EnlightmentCenter information
    MapLocation baseLocation;
    int baseID;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        // Add base information
        RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, allyTeam);
        for (RobotInfo robot : adjacentRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                baseLocation = robot.location;
                baseID = robot.ID;
            }
        }
    }

    @Override
    public void run() throws GameActionException {
        super.run();
    }

    // Moves towards destination, in the optimal direction or diagonal offsets based on which is
    // cheaper to move through. Assumes rc.isReady() == true, or otherwise wastes bytecode on
    // unnecessary computation.
    void fuzzyMove(MapLocation destination) throws GameActionException {
        // TODO: This is not optimal! Sometimes taking a slower move is better if its diagonal.
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight()};
        double cost = -1;
        Direction optimalDir = null;
        for (Direction dir : dirs) {
            if (rc.canMove(dir)) {
                double newCost = rc.sensePassability(myLocation.add(dir));
                if (newCost > cost) {
                    cost = newCost;
                    optimalDir = dir;
                }
            }
        }
        if (optimalDir != null) {
            rc.move(optimalDir);
        }
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}