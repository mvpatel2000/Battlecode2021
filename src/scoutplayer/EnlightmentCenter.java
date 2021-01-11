package scoutplayer;

import battlecode.common.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class EnlightmentCenter extends Robot {
    // EC to EC communication
    boolean scannedAllIDs;
    int searchRound;
    int numPotentialAllyECs;
    int[] potentialAllyECIDs;
    MapLocation[] potentialAllyECLocs; // absolute locations

    int numVerifiedAllyECs;
    int[] verifiedAllyECIDs;
    MapLocation[] verifiedAllyECLocs;

    Set<Integer> foundAllyECLocations;
    FindAllyFlag initialFaf;    // initial flags used to communicate my existence and location
    int[] searchBounds;
    ArrayList<Integer> firstRoundIDsToConsider;
    // Change these two numbers before uploading to competition
    final int CRYPTO_KEY = 92747502; // A random large number
    final int MODULUS = 987; // A random number strictly smaller than CRYPTO KEY and 2^10 = 1024

    // Flags to initialize whenever a unit is spawned, and then set
    // at the earliest available flag slot.
    SpawnUnitFlag latestSpawnFlag;
    LocationFlag latestSpawnDestinationFlag;
    int latestSpawnRound;

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
        scannedAllIDs = false;
        searchRound = 0;
        numPotentialAllyECs = 0;
        potentialAllyECIDs = new int[]{0, 0, 0, 0, 0};
        potentialAllyECLocs = new MapLocation[5];
        numVerifiedAllyECs = 0;
        verifiedAllyECIDs = new int[]{0, 0};
        verifiedAllyECLocs = new MapLocation[2];

        foundAllyECLocations = new HashSet<Integer>();
        // Underweight the first turn of searching since we initialize arrays on that turn.
        searchBounds = new int[]{10000, 11072, 12584, 14096};
        initialFaf = new FindAllyFlag();
        initialFaf.writeCode(generateSecretCode(myID));
        initialFaf.writeLocation(myLocation.x & 127, myLocation.y & 127); // modulo 128
        firstRoundIDsToConsider = new ArrayList<Integer>();

        latestSpawnRound = -1;
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (currentRound == 100) rc.resign(); // TODO: remove; just for debugging

        // Do not add any code in the run() function before this line.
        // initialFlagsAndAllies must run here to fit properly with bytecode.
        // This function will return with ~2700 bytecode left for rounds 1 - 3.
        // TODO: Come up with initialization scheme for neutral ECs we take over.
        if (currentRound < searchBounds.length) {
            initialFlagsAndAllies();
        }

        setSpawnOrDirectionFlag(); // this needs to be run before spawning any units
        if (turnCount > searchBounds.length) {
            spawnOrUpdateScout();
        }
        spawnAttacker();
    }

    /**
     * Set the robot's flag to a SpawnUnitFlag on the same round as spawning
     * a new unit, or a SpawnDestinationFlag on the subsequent round.
     */
    void setSpawnOrDirectionFlag() throws GameActionException {
        if (latestSpawnRound > 0 && currentRound == latestSpawnRound + 1) {
            if (flagSetThisRound) { // TODO: delete, for debugging
                System.out.println("MAJOR ERROR: Trying to send multiple flags in one round!");
            }
            System.out.println("Setting SpawnUnitFlag: " + latestSpawnFlag.flag);
            setFlag(latestSpawnFlag.flag);
        } else if (latestSpawnRound > 0 && currentRound == latestSpawnRound + 2) {
            if (flagSetThisRound) { // TODO: delete, for debugging
                System.out.println("MAJOR ERROR: Trying to send multiple flags in one round!");
            }
            System.out.println("Setting SpawnDestinationFlag: " + latestSpawnDestinationFlag.flag);
            setFlag(latestSpawnDestinationFlag.flag);
        }
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
                st = new ScoutTracker(this, RobotType.POLITICIAN, latestSpawnFlag.readID(), myLocation.add(Direction.EAST)); // TODO: cache id inside SpawnUnitFlag?
            }
        } else {
            //System.out.println("Before st.update(): " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
            st.update(); // check on existing scout
            //System.out.println("After st.update(): " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
        }
    }

    boolean spawnAttacker() throws GameActionException {
        // if (rc.isReady()) {
        //     RobotType toBuild = allyTeam == Team.A ? RobotType.MUCKRAKER : RobotType.POLITICIAN;
        //     int influence = allyTeam == Team.A ? 1 : 50;
        //     if (rc.getInfluence() < influence) {
        //         return false;
        //     }
        //     for (Direction dir : directions) {
        //         if (spawnRobot(toBuild, dir, influence, myLocation, SpawnDestinationFlag.INSTR_ATTACK)) {
        //             return true;
        //         }
        //     }
        // }
        return false;
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
     * @param silent Whether to suppress spawn flags.
     * @return Whether the robot was successfully spawned.
     * @throws GameActionException
     */
    boolean spawnRobot(RobotType type, Direction direction, int influence, MapLocation destination, int instruction, boolean silent) throws GameActionException {
        if (!rc.canBuildRobot(type, direction, influence)) {
            return false;
        }
        rc.buildRobot(type, direction, influence);
        MapLocation spawnLoc = myLocation.add(Direction.EAST);
        System.out.println("Built " + type.toString() + " at " + spawnLoc.toString());
        if (!silent) {
            int newBotID = rc.senseRobotAtLocation(spawnLoc).ID;
            latestSpawnRound = currentRound;
            latestSpawnFlag = new SpawnUnitFlag(type, direction, newBotID);
            latestSpawnDestinationFlag = new SpawnDestinationFlag(destination, instruction);
        }
        return true;
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
        return spawnRobot(type, direction, influence, destination, instruction, false);
    }

    /**
     * Sets an initial flag to let other ECs know I exist.
     * Then, go through IDs 10000 to 14096 to check for my allies.
     * Flags are set and verified using getSecretCode() below.
     * Currently, the check from 10k-14k takes ~50,000 bytecode across 3 rounds (Rounds 1-3).
     * Ends on round 3. You can reduce the number of bytecodes
     * used per round (but extend the number of rounds) by modifying the searchBounds array
     * and making the differences in numbers smaller. Please ensure this function finishes
     * before or on to round 3, or else, you should change STOP_SENDING_LOCATION_ROUND.
     */
    void initialFlagsAndAllies() throws GameActionException {
        // Set flag on first turn
        if (currentRound == 1) {
            if (rc.canSetFlag(initialFaf.flag)) {
                setFlag(initialFaf.flag);
                System.out.println("I set my initial flag to: " + initialFaf.flag);
            } else {
                System.out.println("MAJOR ERROR: FindAllyFlag IS LIKELY WRONG: " + initialFaf.flag);
            }
        }

        // Check first turn IDs for which we could get flag, then clear ArrayList
        if (currentRound == 2 && firstRoundIDsToConsider.size() > 0) {
            for (int i : firstRoundIDsToConsider) {
                FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(i));
                // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                if (generateSecretCode(i) == newFaf.readCode())  {
                    // add RobotID to potentialAlly list and decoded location to map.
                    potentialAllyECIDs[numPotentialAllyECs] = i;
                    numPotentialAllyECs += 1;
                    int[] moduloLocs = newFaf.readLocation();
                    int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                    map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                    MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                    System.out.println("Found a POTENTIAL ally! ID: " + i + ". I now have: " + numPotentialAllyECs + " allies.");
                    System.out.println("Adding POTENTIAL ally " + i + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                }
            }
            firstRoundIDsToConsider.clear();
        }

        // Continue scanning for friendly ECs
        if (!scannedAllIDs) {
            int startPoint = searchBounds[searchRound];
            int endPoint = searchBounds[searchRound+1];
            System.out.println("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
            // We partially unroll this loop to optimize bytecode. Without unrolling, we get 14.1
            // bytecode per iteration, and with unrolling it's 12.2. This lets us do scanning in 3 turns.
            for (int i=startPoint; i<endPoint; i+=16) {
                if (rc.canGetFlag(i)) {
                    int j=i;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(i) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+1)) {
                    int j = i+1;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(i) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+2)) {
                    int j = i+2;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+3)) {
                    int j = i+3;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+4)) {
                    int j = i+4;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+5)) {
                    int j = i+5;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+6)) {
                    int j = i+6;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+7)) {
                    int j = i+7;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+8)) {
                    int j = i+8;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+9)) {
                    int j = i+9;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+10)) {
                    int j = i+10;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+11)) {
                    int j = i+11;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+12)) {
                    int j = i+12;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+13)) {
                    int j = i+13;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+14)) {
                    int j = i+14;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+15)) {
                    int j = i+15;
                    if (myID == j) { continue; }
                    FindAllyFlag newFaf = new FindAllyFlag(rc.getFlag(j));
                    // Check to see if code matches. If so, the Robot with ID i is a potential ally.
                    if (generateSecretCode(j) == newFaf.readCode())  {
                        // add RobotID to potentialAlly list and decoded location to map.
                        potentialAllyECIDs[numPotentialAllyECs] = j;
                        numPotentialAllyECs += 1;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        MapLocation potentialAllyECLocs = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numPotentialAllyECs + " allies.");
                        System.out.println("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
            }
            System.out.println("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
            searchRound += 1;
            if (searchRound == searchBounds.length-1) {
                scannedAllIDs = true;
                System.out.println("Done finding allies.");
            }
            return;
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
