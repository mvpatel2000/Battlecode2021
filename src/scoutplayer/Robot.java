package scoutplayer;

import battlecode.common.*;

public abstract class Robot {

    RobotController rc;
    int currentRound;
    int turnCount;
    int myID;
    Team allyTeam;
    Team enemyTeam;
    MapLocation startLocation;
    MapLocation myLocation;
    boolean flagSetThisRound;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };

    static final Direction[] allDirections = { // includes CENTER
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER
    };

    public Robot(RobotController robotController) throws GameActionException {
        rc = robotController;
        currentRound = 0;
        turnCount = 0;
        allyTeam = rc.getTeam();
        enemyTeam = allyTeam.opponent();
        myID = rc.getID();
        startLocation = rc.getLocation();
        myLocation = startLocation;
        flagSetThisRound = false;
    }

    public void run() throws GameActionException {
        turnCount++;
        flagSetThisRound = false;
        currentRound = rc.getRoundNum();
    }

    public void setFlag(int flag) throws GameActionException {
        rc.setFlag(flag);
        flagSetThisRound = true;
    }

    /**
     * Converts Team enum to number.
     * 0 for Team A, 1 for team B.
     */
    public int getTeamNum(Team t) {
        if (t == Team.A) {
            return 0;
        } else {
            return 1;
        }
    }
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    public static int directionToInt(Direction d) {
        switch (d) {
            case NORTH:
                return 0;
            case NORTHEAST:
                return 1;
            case EAST:
                return 2;
            case SOUTHEAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTHWEST:
                return 5;
            case WEST:
                return 6;
            case NORTHWEST:
                return 7;
            case CENTER:
                return 8;
            default:
                return -1;
        }
    }
}
