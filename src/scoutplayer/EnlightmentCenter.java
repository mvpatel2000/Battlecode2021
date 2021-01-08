package scoutplayer;

import battlecode.common.*;
import java.util.Set;
import java.util.HashSet;

public class EnlightmentCenter extends Robot {
    // EC to EC communication
    boolean setInitialFlag;
    boolean foundAllyECs;
    int initialFlagRound;
    int searchRound;
    int numAllyECs;
    int allyECsUpperBound;
    int[] allyECIds;
    MapLocation[] allyECLocs; // absolute locations
    Set<Integer> foundAllyECLocations;
    FindAllyFlag initialFaf;    // initial flags used to communicate that I exist
    LocationFlag initialLof;    // and my location to ally ECs.
    int[] searchBounds;
    // Change these two numbers before uploading to competition
    final int CRYPTO_KEY = 92747502; // A random large number
    final int MODULUS = 1453481; // A random number strictly smaller than CRYPTO KEY and 2^21 = 2,097,152
    final int STOP_SENDING_LOCATION_ROUND = 10;

    // Flags to initialize whenever a unit is spawned, and then set
    // at the earliest available flag slot.
    SpawnUnitFlag latestSpawnFlag;
    LocationFlag latestSpawnDestinationFlag;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    RelativeMap map;
    ScoutTracker st; // TODO: array of ScoutTrackers

    public EnlightmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        map = new RelativeMap(rc.getLocation());
        st = null; // ScoutTracker

        // Initialize EC to EC communication variables
        setInitialFlag = false;
        foundAllyECs = false;
        initialFlagRound = 1;
        searchRound = 0;
        numAllyECs = 0;
        allyECIds = new int[]{0, 0};
        allyECLocs = new MapLocation[2];
        foundAllyECLocations = new HashSet<Integer>();
        searchBounds = new int[]{10000, 10820, 11640, 12460, 13280, 14096};
        initialFaf = new FindAllyFlag();
        initialFaf.writeCode(generateSecretCode(myID));
        initialLof = new LocationFlag();
        initialLof.writeLocation(myLocation);
        // Note: rc.getRobotCount() returns number of ally units on map. If there's another EC,
        // it might spawn a unit, which would increase this. We would then overestimate the
        // number of ECs, leading us to scan all ranges. This is OK -- we only use this as an
        // early termination method that sometimes helps.
        allyECsUpperBound = Math.min(3, rc.getRobotCount() - 1);
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (turnCount == 100) rc.resign(); // TODO: remove; just for debugging

        // Add all new code after this line.
        // Do not add any code in the run() function before this line.
        // initialFlagsAndAllies must run here to fit properly with bytecode.
        // This function will return with ~500 bytecode left for rounds 2 - 6,
        // and will use very minimal bytecode during other rounds to return immediately.
        // Consider adding an rc.getRoundNum() > 10 check before running late-game code
        // so we do not run out of bytecode.
        initialFlagsAndAllies();
        setInitialLocationFlag();

        spawnOrUpdateScout();
        spawnAttacker();
        
        setSpawnOrDirectionFlag();
    }

    /**
     * 
     */
    void setSpawnOrDirectionFlag() throws GameActionException {

    }

    /**
     * If no scout has been made, spawn a scout. Otherwise, run the
     * ScoutTracker update loop.
     * 
     * TODO: figure out direction to send scout in.
     */
    void spawnOrUpdateScout() throws GameActionException {
        if (st == null) { // no scout has been spawned yet
            if (spawnRobot(RobotType.POLITICIAN, Direction.EAST, 1, myLocation, SpawnDestinationFlag.INSTR_SCOUT)) { // attempt to spawn scout
                st = new ScoutTracker(rc, latestSpawnFlag.readID(), myLocation.add(Direction.EAST), map); // TODO: cache id inside SpawnUnitFlag?
            }
        } else {
            //System.out.println("Before st.update(): " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
            st.update(); // check on existing scout
            //System.out.println("After st.update(): " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
        }
    }

    void spawnAttacker() throws GameActionException {
        RobotType toBuild = allyTeam == Team.A ? RobotType.MUCKRAKER : RobotType.POLITICIAN;
        int influence = allyTeam == Team.A ? 1 : 50;
        for (Direction dir : directions) {
            spawnRobot(toBuild, dir, influence, myLocation, SpawnDestinationFlag.INSTR_ATTACK);
        }
    }
    
    /**
     * Spawn a robot and prepare its SpawnUnitFlag and the LocationFlag
     * telling it where its destination will be. These flags will not
     * actually be set by the EC here; that happens in another function,
     * setSpawnOrDirectionFlag().
     * @param type Type of the robot to build.
     * @param direction Direction to build the robot in.
     * @param influence Influence to allocate to the new robot.
     * @param destination Destination of the new robot.
     * @return Whether the robot was successfully spawned.
     * @throws GameActionException
     */
    boolean spawnRobot(RobotType type, Direction direction, int influence, MapLocation destination, int instruction) throws GameActionException {
        if (!rc.canBuildRobot(type, direction, influence)) {
            return false;
        }
        rc.buildRobot(type, direction, influence);
        MapLocation spawnLoc = myLocation.add(Direction.EAST);
        System.out.println("Built " + type.toString() + " at " + spawnLoc.toString());
        int newBotID = rc.senseRobotAtLocation(spawnLoc).ID;
        latestSpawnFlag = new SpawnUnitFlag(type, direction, newBotID);
        latestSpawnDestinationFlag = new SpawnDestinationFlag(destination, instruction);
        return true;
    }

    /**
     * Sets an initial flag to let other ECs know I exist.
     * Then, go through IDs 10000 to 14096 to check for my allies.
     * Flags are set and verified using getSecretCode() below.
     * Currently, the check from 10k-14k takes ~11,600 bytecodes on each round
     * for 5 rounds (Rounds 2-6). Ends on round 6. You can reduce the number of bytecodes
     * used per round (but extend the number of rounds) by modifying the searchBounds array
     * and making the differences in numbers smaller. Please ensure this function finishes
     * before or on to round 8, or else, you should change STOP_SENDING_LOCATION_ROUND.
     */
    void initialFlagsAndAllies() throws GameActionException {
        if (!setInitialFlag) {
             if (rc.canSetFlag(initialFaf.flag)) {
                 setFlag(initialFaf.flag);
                 System.out.println("I set my initial flag to: " + initialFaf.flag);
                 setInitialFlag = true;
                 initialFlagRound = rc.getRoundNum();
             } else {
                 System.out.println("MAJOR ERROR: FindAllyFlag IS LIKELY WRONG: " + initialFaf.flag);
             }
         }

         if (!foundAllyECs && setInitialFlag && rc.getRoundNum() > initialFlagRound) {
             int startPoint = searchBounds[searchRound];
             int endPoint = searchBounds[searchRound+1];
             //System.out.println(startPoint + " to " + endPoint);
             System.out.println("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
             for (int i=startPoint; i<endPoint; i++) {
                 if (rc.canGetFlag(i)) {
                     if (myID == i) { continue; }
                     // hack to isolate 21-bit code of a FindAllyFlag without the expensive cost of initializing an object.
                     // Please do not copy such bad style, the objects are there for a reason.
                     if (generateSecretCode(i) == (rc.getFlag(i) << 11) >>> 11) {
                         allyECIds[numAllyECs] = i;
                         numAllyECs += 1;
                         System.out.println("Found an ally! ID: " + i + ". I now have: " + numAllyECs + " allies.");
                     }
                 }
             }
             System.out.println("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
             searchRound += 1;
             if (searchRound == searchBounds.length-1) {
                 foundAllyECs = true;
                 System.out.println("Done finding allies.");
             }
             return;
         }
    }

    /**
     * After completing initialFlagsAndAllies, this should run on the next round (currently 7)
     * until round STOP_SENDING_LOCATION_ROUND.
     * We tell our other ECs our location and check for their flags saying the same.
     */
    void setInitialLocationFlag() throws GameActionException {
        if (rc.getRoundNum() <= STOP_SENDING_LOCATION_ROUND && setInitialFlag && foundAllyECs && numAllyECs > 0) {
            // skip round 6, just after completing initialFlagsAndAllies.
            // We don't want to change our flag from a FindAllyFlag to a LocFlag while
            // our allies are still looking for our FindAllyFlag.
            if (searchRound == searchBounds.length - 1) {
                searchRound += 1;
                return;
            }
            // we have already found all the Ally EC locations.
            if (foundAllyECLocations.size() == numAllyECs) {
                return;
            }
            if (rc.canSetFlag(initialLof.flag)) {
                System.out.println("Setting location flag to: " + initialLof.flag);
                setFlag(initialLof.flag);
            } else {
                System.out.println("MAJOR ERROR: LocationFlag IS LIKELY WRONG: " + initialLof.flag);
            }
            // loop through the list of Ally EC ID's found in initialFlagsAndAllies
            // and check if they are displaying LocationFlags. If so, read and parse the data
            // and add it to the RelativeMap.
            for (int i=0; i<numAllyECs; i++) {
                if(rc.canGetFlag(allyECIds[i])) {
                    if (foundAllyECLocations.contains(i)) {
                        continue;
                    }
                    int data = rc.getFlag(allyECIds[i]);
                    Flag ff = new Flag(data);
                    if (ff.getSchema() == Flag.LOCATION_SCHEMA) {
                        LocationFlag new_lf = new LocationFlag(data);
                        int[] locs = new_lf.readRelativeLocationFrom(myLocation); // relative locs
                        System.out.println("Adding ally " + i + " at RELATIVE location (" + locs[0] + ", " + locs[1] + ")");
                        map.set(locs[0], locs[1], RelativeMap.ALLY_EC);
                        MapLocation allyECLoc = map.getAbsoluteLocation(locs[0], locs[1]);
                        System.out.println("Adding ally " + i + " at location " + allyECLoc.toString());
                        allyECLocs[i] = allyECLoc;
                        foundAllyECLocations.add(i);
                    }
                }
            }
        }
    }

    /**
     * Returns a secret code used to verify that a robot is indeed an ally EC.
     * Sent out at the beginning of the match to alert other ECs.
     */
    int generateSecretCode(int robotID) {
        return (Math.abs(CRYPTO_KEY*robotID + getTeamNum(allyTeam))) % MODULUS;
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
