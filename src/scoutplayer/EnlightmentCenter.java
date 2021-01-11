package scoutplayer;

import battlecode.common.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class EnlightmentCenter extends Robot {
    // Symmetries - horizontal, vertical, rotational, true until ruled out.
    boolean[] symmetries;

    // Initial EC to EC communication
    boolean scannedAllIDs;
    int searchRound;
    Set<Integer> foundAllyECLocations;
    FindAllyFlag initialFaf;    // initial flag used to communicate my existence and location
    int[] searchBounds;
    ArrayList<Integer> firstRoundIDsToConsider;
    // Generate Secret Code. Change these two numbers before uploading to competition
    final int CRYPTO_KEY = 92747502; // A random large number
    final int MODULUS = 987; // A random number strictly smaller than CRYPTO KEY and 2^10 = 1024

    // Ally ECs: Unverified and verified.
    // We are treating unverified allies as ground truth for now.
    // For better security, eventually implement and use the verified variables
    int numAllyECs;
    int[] allyECIDs;
    MapLocation[] allyECLocs; // absolute locations

    int numVerifiedAllyECs;
    int[] verifiedAllyECIDs;
    MapLocation[] verifiedAllyECLocs;

    // Environment and enemy
    int numFoundEnemyECs;
    Set<MapLocation> enemyECLocs;

    int numFoundNeutralECs;
    Set<MapLocation> neutralECLocs;

    // Flags to initialize whenever a unit is spawned, and then set
    // at the earliest available flag slot.
    SpawnUnitFlag latestSpawnFlag;
    LocationFlag latestSpawnDestinationFlag;
    int latestSpawnRound;

    // Troop Counts
    int numSlanderers;
    int numMuckrakers;
    int numPoliticians;
    int numScouts; // scouts don't count in troop counts

    // UnitTrackers
    List<UnitTracker> unitTrackerList;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER
    };

    RelativeMap map;
    ScoutTracker st; // TODO: array of ScoutTrackers

    public EnlightmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        map = new RelativeMap(rc.getLocation());

        symmetries = new boolean[]{true, true, true};

        // Initialize EC to EC communication variables
        scannedAllIDs = false;
        searchRound = 0;
        numAllyECs = 0;
        allyECIDs = new int[]{0, 0, 0, 0, 0};  // technically, there could be more than 5 secret code matches, but this chance is astronomical.
        allyECLocs = new MapLocation[5];
        numVerifiedAllyECs = 0;
        verifiedAllyECIDs = new int[]{0, 0};
        verifiedAllyECLocs = new MapLocation[2];
        foundAllyECLocations = new HashSet<Integer>();
        searchBounds = new int[]{10000, 11072, 12584, 14096};   // Underweight the first turn of searching since we initialize arrays on that turn.
        initialFaf = new FindAllyFlag();
        initialFaf.writeCode(generateSecretCode(myID));
        initialFaf.writeLocation(myLocation.x & 127, myLocation.y & 127); // modulo 128
        firstRoundIDsToConsider = new ArrayList<Integer>();

        // Initialize environment and enemy tracking variables
        numFoundEnemyECs = 0;
        enemyECLocs = new HashSet<MapLocation>();
        numFoundNeutralECs = 0;
        neutralECLocs = new HashSet<MapLocation>();

        latestSpawnRound = -1;

        // Troop counts
        numSlanderers = 0;
        numMuckrakers = 0;
        numPoliticians = 0;
        numScouts = 0;

        // List of all unit trackers
        unitTrackerList = new List<UnitTracker>();
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (currentRound == 150) rc.resign(); // TODO: remove; just for debugging

        // Do not add any code in the run() function before this line.
        // initialFlagsAndAllies must run here to fit properly with bytecode.
        // This function will return with ~2700 bytecode left for rounds 1 - 3.
        // TODO: Come up with initialization scheme for neutral ECs we take over.
        if (currentRound < searchBounds.length) {
            initialFlagsAndAllies();
        }

        if (turnCount >= searchBounds.length) {
            readAllyECUpdates(); // read EC updates before building units/prod logic.
            // spawnOrUpdateScout();
        }

        setSpawnOrDirectionFlag(); // this needs to be run before spawning any units
        updateUnitTrackers();
        buildUnit();

        if (currentRound >= searchBounds.length && !flagSetThisRound) {
            setFlag(0);
        }
    }

    /**
     * Wrapper function for spawnRobot. Determines build order. Spawns an initial silent slanderer
     * and subsequently builds a mix of all units based on ratios. Destination is based on the 
     * nearest enemy EC or a random exploration destination.
     * @throws GameActionException
     */
    void buildUnit() throws GameActionException {
        // Turn 1 spawn silent slanderer
        if (turnCount == 1) {
            Direction optimalDir = findOptimalSpawnDir();
            if (optimalDir != null) {
                spawnRobotSilentlyWithTracker(RobotType.SLANDERER, optimalDir, 140);
            }
        } 
        // Otherwise, do normal build order
        else {
            Direction optimalDir = findOptimalSpawnDir();
            if (optimalDir != null) {
                // Determine destination by taking closest enemyECLoc or random exploration
                // destination if no ECs are found.
                MapLocation enemyLocation = null;
                int enemyLocationDistance = 999999999;
                if (enemyECLocs.size() > 0) {
                    for (MapLocation enemyECLoc : enemyECLocs) {
                        int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                        if (enemyLocationDistance < enemyECLocDestination) {
                            enemyLocation = enemyECLoc;
                            enemyLocationDistance = enemyECLocDestination;
                        }
                    }
                } else if (neutralECLocs.size() > 0) {
                    for (MapLocation neutralECLoc : neutralECLocs) {
                        int neutralECLocDestination = myLocation.distanceSquaredTo(neutralECLoc);
                        if (enemyLocationDistance < neutralECLocDestination) {
                            enemyLocation = neutralECLoc;
                            enemyLocationDistance = neutralECLocDestination;
                        }
                    }
                } else {
                    // TODO: Come up with better exploration heuristic. Use map bounds we calculate
                    // in scouting to better guess locations.
                    int signx = Math.random() < .5 ? -1 : 1;
                    int signy = Math.random() < .5 ? -1 : 1;
                    int dx = (int)(Math.random()*20 + 20) * signx;
                    int dy = (int)(Math.random()*20 + 20) * signy;
                    enemyLocation = myLocation.translate(dx, dy);
                }
                if (rc.getInfluence() > 145 && (numSlanderers - 3) * 2 < numMuckrakers + numPoliticians) {
                    int maxInfluence = Math.min(949, rc.getInfluence() - 5);
                    spawnRobotWithTracker(RobotType.SLANDERER, optimalDir, maxInfluence, myLocation.add(optimalDir).add(optimalDir).add(optimalDir), 0);
                    numSlanderers++;
                } else if (false && numPoliticians * 3 > numMuckrakers) {
                    spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, 0);
                    numMuckrakers++;
                } else {
                    if (rc.getInfluence() > 1000) {
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 1000, enemyLocation, 0);
                    } else {
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 14, enemyLocation, 0);
                    }
                    numPoliticians++;
                }
            }
        }
    }

    /**
     * Reads flags from Ally ECs.
     * Handles logic: Given a flag from an Ally EC, what does this EC do?
     */
    void readAllyECUpdates() throws GameActionException {
        for (int i=0; i<numAllyECs; i++) {
            if(rc.canGetFlag(allyECIDs[i])) { // should always be true
                int flagInt = rc.getFlag(allyECIDs[i]);
                int flagSchema = Flag.getSchema(flagInt);
                switch (flagSchema) {
                    case Flag.NO_SCHEMA:
                        break;
                    case Flag.EC_SIGHTING_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                    case Flag.MAP_TERRAIN_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                    case Flag.LOCATION_SCHEMA:
                        break;
                    case Flag.SPAWN_UNIT_SCHEMA:
                        // Track new unit our ally EC produced.
                        SpawnUnitFlag suf = new SpawnUnitFlag(flagInt);
                        RobotType spawnType = suf.readUnitType();
                        int spawnID = suf.readID();
                        Direction spawnDir = suf.readSpawnDir();
                        unitTrackerList.add(new UnitTracker(this, spawnType, spawnID, allyECLocs[i].add(spawnDir)));
                        System.out.println("Ally " + allyECLocs[i] + "told me about new " + spawnType + " at " + allyECLocs[i].add(spawnDir));
                        break;
                    case Flag.SPAWN_DESTINATION_SCHEMA:
                        // TODO: @Vinjai: handle the case where scouts are spawned
                        // and we want to set up a ScoutTracker instead of UnitTracker.
                        break;
                    case Flag.UNIT_UPDATE_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                }
            }
        }
    }

    /**
     * Reads flags from all tracked units (not ally ECs).
     * Handles logic: Given a flag from a unit we're tracking,
     * what does this EC do?
     * TODO: @Vinjai: Handle the other cases. For example, MapTerrainFlag.
     */
    void updateUnitTrackers() throws GameActionException {
        unitTrackerList.resetIter();
        while(unitTrackerList.hasNext()) {
            UnitTracker ut = unitTrackerList.next();
            int unitUpdate = ut.update();
            switch(unitUpdate) {
                case -1:
                    break;
                case Flag.NO_SCHEMA:
                    break;
                case Flag.EC_SIGHTING_SCHEMA:
                    ECSightingFlag ecsf = new ECSightingFlag(ut.flagInt);
                    MapLocation ecLoc = ecsf.readAbsoluteLocation(myLocation);
                    if (ecsf.readECType() == ECSightingFlag.NEUTRAL_EC) {
                        if (!neutralECLocs.contains(ecLoc)) {
                            neutralECLocs.add(ecLoc);
                        }
                        System.out.println("Informed about NEUTRAL EC at " + ecLoc);
                    } else {
                        if (!enemyECLocs.contains(ecLoc)) {
                            enemyECLocs.add(ecLoc);
                            // This EC has been converted from neutral to enemy since we last saw it.
                            if(neutralECLocs.contains(ecLoc)) {
                                neutralECLocs.remove(ecLoc);
                            }
                        }
                        System.out.println("Informed about ENEMY EC at " + ecLoc);
                    }
                    break;
                case Flag.MAP_TERRAIN_SCHEMA:
                    break;
                case Flag.LOCATION_SCHEMA:
                    break;
                case Flag.SPAWN_UNIT_SCHEMA:
                    break;
                case Flag.SPAWN_DESTINATION_SCHEMA:
                    break;
                case Flag.UNIT_UPDATE_SCHEMA:
                    break;
            }
        }
    }

    /**
     * Spawn robot silently (without flags) and add a generic unit tracker.
     * @param type Type of the robot to build.
     * @param direction Direction to build the robot in.
     * @param influence Influence to allocate to the new robot.
     * @return Whether the robot was successfully spawned.
     */
    boolean spawnRobotSilentlyWithTracker(RobotType type, Direction direction, int influence) throws GameActionException {
        if (spawnRobotSilently(type, direction, influence)) {
            int id = rc.senseRobotAtLocation(myLocation.add(direction)).ID;
            unitTrackerList.add(new UnitTracker(this, type, id, myLocation.add(direction)));
            return true;
        }
        return false;
    }

    /**
     * Spawn robot non-silently (with flags) and add a generic unit tracker.
     * @param type Type of the robot to build.
     * @param direction Direction to build the robot in.
     * @param influence Influence to allocate to the new robot.
     * @param destination Destination of the new robot.
     * @param instruction Instruction to send to the new robot; the list
     * of available instructions is in SpawnDestinationFlag.
     * @return Whether the robot was successfully spawned.
     */
    boolean spawnRobotWithTracker(RobotType type, Direction direction, int influence, MapLocation destination, int instruction) throws GameActionException {
        if (spawnRobot(type, direction, influence, destination, instruction)) {
            int id = rc.senseRobotAtLocation(myLocation.add(direction)).ID;
            unitTrackerList.add(new UnitTracker(this, type, id, myLocation.add(direction)));
            return true;
        }
        return false;
    }

    /**
     * Finds best terrain that a unit can spawn on. Returns null if no valid direction exists.
     * @throws GameActionException
     */
    Direction findOptimalSpawnDir() throws GameActionException {
        Direction optimalDir = null;
        double optimalPassability = -1;
        for (Direction dir : directions) {
            // Dummy robot build check for valid direction
            if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                double passabilty = rc.sensePassability(myLocation.add(dir));
                if (passabilty > optimalPassability) {
                    optimalDir = dir;
                    optimalPassability = passabilty;
                }
            }
        }
        return optimalDir;
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
    boolean spawnScout() throws GameActionException {
        if (numScouts < 1) { // no scout has been spawned yet
            if (spawnRobot(RobotType.POLITICIAN, Direction.EAST, 1, myLocation, SpawnDestinationFlag.INSTR_SCOUT)) { // attempt to spawn scout
                unitTrackerList.add(new ScoutTracker(this, RobotType.POLITICIAN, latestSpawnFlag.readID(), myLocation.add(Direction.EAST)));
                numScouts++;
                return true;
            }
        }
        return false;
    }

    boolean spawnAttacker() throws GameActionException {
        if (rc.isReady()) {
            RobotType toBuild = allyTeam == Team.A ? RobotType.MUCKRAKER : RobotType.POLITICIAN;
            int influence = allyTeam == Team.A ? 1 : 50;
            if (rc.getInfluence() < influence) {
                return false;
            }
            for (Direction dir : directions) {
                if (spawnRobot(toBuild, dir, influence, myLocation, SpawnDestinationFlag.INSTR_ATTACK)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Spawn a robot without sending it or the other ECs any flags.
     * @param type Type of the robot to build.
     * @param direction Direction to build the robot in.
     * @param influence Influence to allocate to the new robot.
     * @return Whether the robot was successfully spawned.
     * @throws GameActionException
     */
    boolean spawnRobotSilently(RobotType type, Direction direction, int influence) throws GameActionException {
        if (!rc.canBuildRobot(type, direction, influence)) {
            return false;
        }
        rc.buildRobot(type, direction, influence);
        MapLocation spawnLoc = myLocation.add(direction);
        System.out.println("Built " + type.toString() + " silently at " + spawnLoc.toString());
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
     * @param instruction Instruction to send to the new robot; the list
     * of available instructions is in SpawnDestinationFlag.
     * @return Whether the robot was successfully spawned.
     * @throws GameActionException
     */
    boolean spawnRobot(RobotType type, Direction direction, int influence, MapLocation destination, int instruction) throws GameActionException {
        if (!rc.canBuildRobot(type, direction, influence)) {
            return false;
        }
        rc.buildRobot(type, direction, influence);
        MapLocation spawnLoc = myLocation.add(direction);
        System.out.println("Built " + type.toString() + " at " + spawnLoc.toString());
        int newBotID = rc.senseRobotAtLocation(spawnLoc).ID;
        latestSpawnRound = currentRound;
        latestSpawnFlag = new SpawnUnitFlag(type, direction, newBotID);
        latestSpawnDestinationFlag = new SpawnDestinationFlag(destination, instruction);
        return true;
    }


    /**
     * Begin by only comparing ally EC locations to rule out symmetry.
     * Call at the end of initialFlagsAndAllies.
     * ADVANCED: Use enemy EC lcoations to rule out symmetry axes. Also, use neutral ECs.
     * To implement this, consider calling after case Flag.EC_SIGHTING_SCHEMA in updateUnitTrackers(), but be careful:
     * Enemy ECs could have started out as neutral ECs or ally ECs.
     */
    void updateSymmetry() {
        return;
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
                    allyECIDs[numAllyECs] = i;
                    int[] moduloLocs = newFaf.readLocation();
                    int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                    map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                    allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                    numAllyECs += 1;
                    System.out.println("Found a POTENTIAL ally! ID: " + i + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
                        allyECIDs[numAllyECs] = j;
                        int[] moduloLocs = newFaf.readLocation();
                        int[] relLocs = getRelativeLocFromModuloLoc(moduloLocs[0], moduloLocs[1], myLocation);
                        map.set(relLocs[0], relLocs[1], RelativeMap.ALLY_EC);
                        allyECLocs[numAllyECs] = map.getAbsoluteLocation(relLocs[0], relLocs[1]);
                        numAllyECs += 1;
                        System.out.println("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
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
