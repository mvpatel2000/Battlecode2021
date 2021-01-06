package scoutplayer;

import battlecode.common.*;

public class EnlightmentCenter extends Robot {
    boolean setInitialFlag;
    int initialFlagRound = 1;
    boolean foundAllyECs;
    int numAllyECs;
    int initialCheck;
    MapLocation[] allyECLocs;
    // Change these two numbers before uploading to competition
    final int CRYPTO_KEY = 92747502; // A random large number
    final int MODULUS = 36904; // A random number smaller than CRYPTO KEY and 2^21 = 2,097,152

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    public EnlightmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        setInitialFlag = false;
        initialFlagRound = 1;
        foundAllyECs = false;
        allyECLocs = new MapLocation[2];
        numAllyECs = 0;
        initialCheck = 10000;
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        initialFlagsAndAllies();

        RobotType toBuild = RobotType.MUCKRAKER;
        int influence = 1;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }

    /*
     * Sets an initial flag to let other ECs know I exist.
     * Then, go through IDs 10000 to 14096 to check for my allies.
     * Flags are set and verified using getSecretCode() below.
     * Currently, the check from 10k-14k takes ~6-7 rounds.
     * =========================================================
     * TODO: Use the FindAllyFlag class instead of this one-off flag!
     * TODO: Communicate my location + a code instead of just a code! Very important!
     * Reduce code to 9 bits, so I will have a 3 Bit header + 9 Bit code + 12 bit location.
     * THIS REQUIRES A WAY TO ENCODE LOCATIONS IN 12 BITS BEFORE I KNOW THE OFFSET!
     * Once the above ^ is figured out, the above TODOs should be trivial! FindAllyFlag is already written.
     */
    void initialFlagsAndAllies() throws GameActionException {
        if (!setInitialFlag) {
            int code = getSecretCode(rc.getID());
            if (rc.canSetFlag(code)) {
                rc.setFlag(code);
                System.out.println("I set my initial flag to: " + code);
                setInitialFlag = true;
                initialFlagRound = rc.getRoundNum();
            } else {
                System.out.println("MAJOR ERROR: CODE IS LIKELY WRONG: " + code);
            }
        }

        if (!foundAllyECs && setInitialFlag && rc.getRoundNum() > initialFlagRound) {
            for (int i=10000; i<14096; i++) {
                if (rc.canGetFlag(i)) {
                    if (getSecretCode(i) == rc.getFlag(i)) {
                        numAllyECs += 1;
                        System.out.println("Found an ally! Yay " + i + " I now have: " + numAllyECs + " allies.");
                    }
                }
            }
            foundAllyECs = true;
            System.out.println("Done searching for allies. FINAL TALLY: " + numAllyECs + " allies.");
        }
    }

    /*
     * Returns a secret code used to verify that a robot is indeed an ally EC.
     * Sent out at the beginning of the match to alert other ECs.
     */
    int getSecretCode(int robotID) {
        return (Math.abs(CRYPTO_KEY*robotID + getTeamNum(rc.getTeam()))) % MODULUS;
    }
    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }
}
