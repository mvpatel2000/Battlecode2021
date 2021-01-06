package scoutplayer;

import battlecode.common.*;

public class EnlightmentCenter extends Robot {
    // EC to EC communication
    boolean setInitialFlag;
    int initialFlagRound;
    int numFoundECs;
    int numAllyECs;
    MapLocation[] allyECLocs;
    int scanIndex = 10000;
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

        // Initialize EC to EC communication variables
        setInitialFlag = false;
        initialFlagRound = 1;
        numFoundECs = 0;
        // Note: rc.getRobotCount() returns number of ally units on map. If there's another EC,
        // it might spawn a unit, which would increase this. We would then overestimate the
        // number of ECs, leading us to scan all ranges. This is OK -- we only use this as an
        // early termination method that sometimes helps.
        numAllyECs = Math.min(3, rc.getRobotCount() - 1);
        allyECLocs = new MapLocation[2];
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
        // Already done finding all ally ECs
        if (numFoundECs == numAllyECs || scanIndex > 14096) {
            return;
        }
    
        // Setup initial flag
        if (!setInitialFlag) {
            int code = getSecretCode(rc.getID());
            if (rc.canSetFlag(code)) {
                rc.setFlag(code);
                setInitialFlag = true;
                System.out.println("I set my initial flag to: " + code + " and expect " + numAllyECs + " allies.");
                initialFlagRound = rc.getRoundNum();
            } else {
                System.out.println("MAJOR ERROR: EQ CODE IS LIKELY WRONG: " + code);
            }
        }

        // Scan over possible IDs across multiple rounds until ally ECs found.
        if (setInitialFlag && rc.getRoundNum() > initialFlagRound) {
            while (Clock.getBytecodesLeft() > 200 && scanIndex < 14096) {
                for (; scanIndex < scanIndex + 10; scanIndex++) {
                    if (rc.canGetFlag(scanIndex) && getSecretCode(scanIndex) == rc.getFlag(scanIndex) && scanIndex != rc.getID()) {
                        numFoundECs += 1;
                        System.out.println("Found an ally! Yay " + scanIndex + " I now have: " + numFoundECs + " allies out of " + numAllyECs + " expected.");
                        if (numFoundECs == numAllyECs) {
                            break;
                        }
                    }
                }
                if (numFoundECs == numAllyECs) {
                    break;
                }
            }
            System.out.println("Done searching for allies. FINAL TALLY: " + numFoundECs + " allies out of " + numAllyECs + " expected.");
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
