package scoutplayer;

import battlecode.common.*;

public abstract class Unit extends Robot {
    
    // Base EnlightmentCenter information
    MapLocation baseLocation;
    int baseID;

    RobotInfo[] nearbyRobots;
    Direction moveThisTurn;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        moveThisTurn = Direction.CENTER;
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
        moveThisTurn = Direction.CENTER;
    }

    public void parseVision() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
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
            moveThisTurn = dir;
            return true;
        }
        moveThisTurn = Direction.CENTER;
        return false;
    }
}