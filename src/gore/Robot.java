package gore;

import battlecode.common.*;

public abstract class Robot {

    RobotController rc;
    int currentRound;
    int turnCount;
    int myID;
    Team allyTeam;
    Team enemyTeam;
    Team neutralTeam;
    MapLocation startLocation;
    MapLocation myLocation;
    boolean flagSetThisRound;
    int flagDataSetThisRound;
    public boolean isSlandererConvertedToPolitician;

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
    
    public static final RobotType[] robotTypes = {
        RobotType.ENLIGHTENMENT_CENTER,
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER
    };

    public Robot(RobotController robotController) throws GameActionException {
        rc = robotController;
        currentRound = rc.getRoundNum();
        turnCount = 0;
        allyTeam = rc.getTeam();
        enemyTeam = allyTeam.opponent();
        neutralTeam = Team.NEUTRAL;
        myID = rc.getID();
        startLocation = rc.getLocation();
        myLocation = startLocation;
        flagSetThisRound = false;
        flagDataSetThisRound = 0;
        isSlandererConvertedToPolitician = false;
    }

    public void run() throws GameActionException {
        turnCount++;
        flagSetThisRound = false;
        flagDataSetThisRound = 0;
        currentRound = rc.getRoundNum();
    }

    public void setFlag(int flag) throws GameActionException {
        rc.setFlag(flag);
        flagSetThisRound = true;
        flagDataSetThisRound = flag;
    }

    public int[] getRelativeLocFromModuloLoc(int xMod, int yMod, MapLocation thisLoc) {
        int xRel = xMod - (thisLoc.x & 127);
        if (xRel > 64) {
            xRel -= 128;
        } else if (xRel < -64) {
            xRel += 128;
        }
        int yRel = yMod - (thisLoc.y & 127);
        if (yRel > 64) {
            yRel -= 128;
        } else if (yRel < -64) {
            yRel += 128;
        }
        return new int[]{ xRel, yRel };
    }

    /**
     * Converts Team enum to number.
     * 0 for Team A, 1 for team B.
     */
    public int getTeamNum(Team t) {
        switch (t) {
            case A:
                return 0;
            case B:
                return 1;
            case NEUTRAL:
                return 2;
            default:
                return -1;
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

    /**
     * Convert RobotType to integer.
     */
    public static int typeToInt(RobotType type) {
        switch(type) {
            case POLITICIAN:
                return 1;
            case SLANDERER:
                return 2;
            case MUCKRAKER:
                return 3;
            default:
                return 0;
        }
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
