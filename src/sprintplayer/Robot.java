package sprintplayer;

import battlecode.common.*;

public abstract class Robot {
    RobotController rc;
    int turnCount = 0;
    Team allyTeam;
    Team enemyTeam;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    public Robot(RobotController robotController) throws GameActionException {
        rc = robotController;
        allyTeam = rc.getTeam();
        enemyTeam = allyTeam.opponent();
    }

    public void run() throws GameActionException {
        turnCount++;
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
}