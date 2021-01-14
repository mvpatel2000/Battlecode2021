package smite;

import battlecode.common.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class EnlightmentCenter extends Robot {
    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4},{1,5},{5,1},{5,-1},{1,-5},{-1,-5},{-5,-1},{-5,1},{-1,5},{2,5},{5,2},{5,-2},{2,-5},{-2,-5},{-5,-2},{-5,2},{-2,5},{4,4},{4,-4},{-4,-4},{-4,4},{3,5},{5,3},{5,-3},{3,-5},{-3,-5},{-5,-3},{-5,3},{-3,5},{0,6},{6,0},{0,-6},{-6,0},{1,6},{6,1},{6,-1},{1,-6},{-1,-6},{-6,-1},{-6,1},{-1,6},{2,6},{6,2},{6,-2},{2,-6},{-2,-6},{-6,-2},{-6,2},{-2,6}};
    // Symmetries - horizontal, vertical, rotational, true until ruled out.
    boolean[] symmetries;

    // Initial EC to EC communication
    boolean scannedAllIDs;
    int searchRound;
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
    int[] allyDistances;

    // Environment and enemy
    Set<MapLocation> enemyECLocs;
    Set<MapLocation> neutralECLocs;
    Set<MapLocation> capturedAllyECLocs;

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
    final int MAX_UNITS_TRACKED = 80;

    // Bidding information
    int previousInfluence = 0;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER
    };

    RelativeMap map;
    //ScoutTracker st; // TODO: array of ScoutTrackers

    public EnlightmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        map = new RelativeMap(rc.getLocation());

        symmetries = new boolean[]{true, true, true};

        // Initialize EC to EC communication variables
        scannedAllIDs = false;
        searchRound = 0;
        numAllyECs = 0;
        allyECIDs = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        allyECLocs = new MapLocation[11];
        allyDistances = new int[]{0, 0};
        searchBounds = new int[]{10000, 10960, 12528, 14096};   // Underweight the first turn of searching since we initialize arrays on that turn.
        initialFaf = new FindAllyFlag();
        initialFaf.writeCode(generateSecretCode(myID));
        initialFaf.writeLocation(myLocation.x & 127, myLocation.y & 127); // modulo 128
        firstRoundIDsToConsider = new ArrayList<Integer>();

        // Initialize environment and enemy tracking variables
        enemyECLocs = new HashSet<MapLocation>();
        neutralECLocs = new HashSet<MapLocation>();
        capturedAllyECLocs = new HashSet<MapLocation>();
        latestSpawnRound = -1;

        // Troop counts
        numSlanderers = 0;
        numMuckrakers = 0;
        numPoliticians = 0;
        numScouts = 0;

        // List of all unit trackers
        unitTrackerList = new List<UnitTracker>();

        previousInfluence = 0;
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        // if (currentRound == 500) rc.resign(); // TODO: remove; just for debugging

        considerBid();

        // Do not add any code in the run() function before this line.
        // initialFlagsAndAllies must run here to fit properly with bytecode.
        // This function will return with ~2700 bytecode left for rounds 1 - 3.
        // TODO: Come up with initialization scheme for neutral ECs we take over.
        if (currentRound < searchBounds.length) {
            initialFlagsAndAllies();
        }
        if (turnCount == searchBounds.length) {
            putVisionTilesOnMap();
            updateSymmetryFromAllies();
        }
        if (turnCount >= searchBounds.length) {
            readAllyECUpdates(); // read EC updates before building units/prod logic.
            setSpawnOrDirectionFlag(); // this needs to be run before spawning any unit
            allyDistances = furthestAllyDistances();
            // spawnOrUpdateScout();
        }
        // Be careful about bytecode usage on rounds < searchBounds.length, especially round 1.
        // We currently end round 1 with 10 bytecode left. Rounds 2 and 3, ~2000 left.
        //System.out.println\("I am tracking " + unitTrackerList.length + " units");
        //System.out.println\("Bytecodes used before UnitTrackers: " + Clock.getBytecodeNum());
        updateUnitTrackers();
        //System.out.println\("Bytecodes used after UnitTrackers: " + Clock.getBytecodeNum());
        buildUnit();


        if (currentRound >= searchBounds.length && !flagSetThisRound) {
            setFlag(0);
        }
    }

    /**
     * Bid for votes. Stop bidding after getting 751, always vote 1, otherwise vote % of income
     * scaling as game progresses.
     * @throws GameActionException
     */
    void considerBid() throws GameActionException {
        int currentVotes = rc.getTeamVotes();
        // We have more than half the votes, stop bidding
        if (currentVotes > 750) {
            return;
        }
        int currentInfluence = rc.getInfluence();
        int dInf = currentInfluence - previousInfluence;
        // Bid 1 for first 350 turns
        if (currentRound <= 350) {
            if (currentInfluence > 10 && rc.canBid(1)) {
                rc.bid(1);
            }
        } 
        // Bid 1/8th income for first 550 turns
        else if (currentRound <= 550) {
            int dInfOverEight = (int)(dInf / 8);
            if (currentInfluence > dInfOverEight && rc.canBid(dInfOverEight)) {
                rc.bid(dInfOverEight);
            }
        }
        // Bid scaling from 1/8 income at turn 550 to 2 * income at turn 1500
        else if (currentRound < 1499) {
            double step = (currentRound - 550) / 1500;
            double proportion = 1/8 + step * 15/8;
            int bidAmount = (int)(proportion * dInf);
            if (currentInfluence > bidAmount && rc.canBid(bidAmount)) {
                rc.bid(bidAmount);
            }
        } else {
            if (rc.canBid(currentInfluence)) {
                rc.bid(currentInfluence);
            }
        }
        previousInfluence = currentInfluence;
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
                if (rc.getInfluence() > 40 && (numSlanderers - 1) * 2 < numMuckrakers + numPoliticians) {
                    int maxInfluence = Math.min(949, rc.getInfluence() - 5);
                    MapLocation enemyLocation = optimalDestination(true, false);
                    MapLocation shiftedLocation = myLocation.translate(myLocation.x - enemyLocation.x, myLocation.y - enemyLocation.y);
                    //System.out.println\("SPAWN SLANDERER:  " + enemyLocation + " " + shiftedLocation);
                    spawnRobotWithTracker(RobotType.SLANDERER, optimalDir, maxInfluence, shiftedLocation, 0);
                    numSlanderers++;
                } else if (numPoliticians > numMuckrakers * 2) {
                    MapLocation enemyLocation = optimalDestination(false, false);
                    spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, 0);
                    numMuckrakers++;
                } else {
                    if (rc.getInfluence() > 1000) {
                        MapLocation enemyLocation = optimalDestination(true, false);
                        //System.out.println\("Spawning killer: " + enemyLocation);
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 1000, enemyLocation, 0);
                    } else {
                        MapLocation enemyLocation = optimalDestination(false, false);
                        //System.out.println\("Spawning killer: " + enemyLocation);
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
            if(rc.canGetFlag(allyECIDs[i])) {
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
                        // TODO: Perhaps also keep a tally of number of units per unit type.
                        SpawnUnitFlag suf = new SpawnUnitFlag(flagInt);
                        RobotType spawnType = suf.readUnitType();
                        int spawnID = suf.readID();
                        Direction spawnDir = suf.readSpawnDir();
                        if (unitTrackerList.length < MAX_UNITS_TRACKED) {
                            unitTrackerList.add(new UnitTracker(this, spawnType, spawnID, allyECLocs[i].add(spawnDir)));
                        }
                        //System.out.println\("Ally " + allyECLocs[i] + "told me about new " + spawnType + " at " + allyECLocs[i].add(spawnDir));
                        break;
                    case Flag.SPAWN_DESTINATION_SCHEMA:
                        // TODO: @Vinjai: handle the case where scouts are spawned
                        // and we want to set up a ScoutTracker instead of UnitTracker.
                        break;
                    case Flag.UNIT_UPDATE_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                }
            } else {
                // Oh no! ally has been captured
                // Delete from list by replacing elem i with last elem of List
                // and decrementing list length.
                if (!enemyECLocs.contains(allyECLocs[i])) {
                    enemyECLocs.add(allyECLocs[i]);
                }
                allyECIDs[i] = allyECIDs[numAllyECs-1];
                allyECLocs[i] = allyECLocs[numAllyECs-1];
                numAllyECs -= 1;
                i -= 1;
                //System.out.println\("Ally Lost! I now have " + numAllyECs + " allies.");
            }
        }
    }

    /**
     * Reads flags from all tracked units (not ally ECs).
     * Handles logic: Given a flag from a unit we're tracking,
     * what does this EC do?
     */
    void updateUnitTrackers() throws GameActionException {
        unitTrackerList.resetIter();
        while(unitTrackerList.currNotNull()) {
            UnitTracker ut = unitTrackerList.curr();
            int unitUpdate = ut.update();
            if (unitUpdate == -1) {
                unitTrackerList.popStep();
                continue;
            }
            switch(unitUpdate) {
                case Flag.NO_SCHEMA:
                    break;
                case Flag.EC_SIGHTING_SCHEMA:
                    ECSightingFlag ecsf = new ECSightingFlag(ut.flagInt);
                    MapLocation ecLoc = ecsf.readAbsoluteLocation(myLocation);
                    if (ecsf.readECType() == ECSightingFlag.NEUTRAL_EC) {
                        if (!neutralECLocs.contains(ecLoc)) {
                            neutralECLocs.add(ecLoc);
                            //System.out.println\("Informed about NEUTRAL EC at " + ecLoc);
                        }
                    } else if (ecsf.readECType() == ECSightingFlag.ENEMY_EC){
                        if (!enemyECLocs.contains(ecLoc)) {
                            enemyECLocs.add(ecLoc);
                            // This EC has been converted from neutral to enemy since we last saw it.
                            if(neutralECLocs.contains(ecLoc)) {
                                neutralECLocs.remove(ecLoc);
                            }
                            // This EC has been converted from a captured ally to an enemy since we last saw it.
                            if(capturedAllyECLocs.contains(ecLoc)) {
                                capturedAllyECLocs.remove(ecLoc);
                            }
                            //System.out.println\("Informed about ENEMY EC at " + ecLoc);
                        }
                    } else if (ecsf.readECType() == ECSightingFlag.ALLY_EC) {
                        if (!capturedAllyECLocs.contains(ecLoc)) {
                            capturedAllyECLocs.add(ecLoc);
                            if (enemyECLocs.contains(ecLoc)) {
                                enemyECLocs.remove(ecLoc);
                            }
                            if(neutralECLocs.contains(ecLoc)) {
                                neutralECLocs.remove(ecLoc);
                            }
                            //System.out.println\("Informed about new ALLY EC at " + ecLoc);
                        }
                    }
                    break;
                case Flag.MAP_TERRAIN_SCHEMA:
                    break;
                case Flag.LOCATION_SCHEMA:
                    break;
                case Flag.SPAWN_UNIT_SCHEMA:
                    // handled in readAllyECUpdates()
                    break;
                case Flag.SPAWN_DESTINATION_SCHEMA:
                    break;
                case Flag.UNIT_UPDATE_SCHEMA:
                    break;
            }
            unitTrackerList.step();
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
            if (unitTrackerList.length < MAX_UNITS_TRACKED) {
                unitTrackerList.add(new UnitTracker(this, type, id, myLocation.add(direction)));
            }
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
            if (unitTrackerList.length < MAX_UNITS_TRACKED) {
                unitTrackerList.add(new UnitTracker(this, type, id, myLocation.add(direction)));
            }
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
                //System.out.println\("MAJOR ERROR: Trying to send multiple flags in one round!");
            }
            ////System.out.println\("Setting SpawnUnitFlag: " + latestSpawnFlag.flag);
            setFlag(latestSpawnFlag.flag);
        } else if (latestSpawnRound > 0 && currentRound == latestSpawnRound + 2) {
            if (flagSetThisRound) { // TODO: delete, for debugging
                //System.out.println\("MAJOR ERROR: Trying to send multiple flags in one round!");
            }
            ////System.out.println\("Setting SpawnDestinationFlag: " + latestSpawnDestinationFlag.flag);
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
        //System.out.println\("Built " + type.toString() + " silently at " + spawnLoc.toString());
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
        //System.out.println\("Built " + type.toString() + " at " + spawnLoc.toString() + " to " + destination);
        int newBotID = rc.senseRobotAtLocation(spawnLoc).ID;
        latestSpawnRound = currentRound;
        latestSpawnFlag = new SpawnUnitFlag(type, direction, newBotID);
        latestSpawnDestinationFlag = new SpawnDestinationFlag(destination, instruction);
        return true;
    }

    /**
     * Determines destination to send robots. First choice is closest enemyECLoc.
     * If includeNeutral is false, then it does not consider neutralECLocs as potential destinations.
     * If we don't know about any ECs, goes to fallback:
     *      If randomFallback == False, sets destination based on symmetries ruled out and weighted by current info on map.
     *      If randomFallback == True, sets destination randomly.
     */
    MapLocation optimalDestination(boolean includeNeutral, boolean randomFallback) {
        if (rc.getRoundNum() < searchBounds.length) {
            int[] dArr = randomDestination();
            return myLocation.translate(dArr[0], dArr[1]);
        }
        MapLocation enemyLocation = null;
        int enemyLocationDistance = 999999999;
        if (enemyECLocs.size() > 0) {
            for (MapLocation enemyECLoc : enemyECLocs) {
                int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                if (enemyECLocDestination < enemyLocationDistance) {
                    enemyLocation = enemyECLoc;
                    enemyLocationDistance = enemyECLocDestination;
                }
            }
        } else if (includeNeutral && neutralECLocs.size() > 0) {
            for (MapLocation neutralECLoc : neutralECLocs) {
                int neutralECLocDestination = myLocation.distanceSquaredTo(neutralECLoc);
                if (neutralECLocDestination < enemyLocationDistance) {
                    enemyLocation = neutralECLoc;
                    enemyLocationDistance = neutralECLocDestination;
                }
            }
        } else {
            // FALLBACK: We don't know about any ECs.
            // TODO: Ensure this exploration heuristic produces good results.
            int[] dArr = new int[]{0, 0};
            if (!randomFallback) {
                // Use relativeMap's information on how far away opposite walls are to values for map size.
                int horizAbsSum = Math.abs(map.xLineAboveUpper) + Math.abs(map.xLineBelowLower);
                int horizSum = map.xLineAboveUpper + map.xLineBelowLower;
                Direction horizFurthestDirection = map.xLineAboveUpper > Math.abs(map.xLineBelowLower) ? Direction.EAST : Direction.WEST;
                int horizFurthestWall = Math.max(map.xLineAboveUpper, Math.abs(map.xLineBelowLower));

                int vertAbsSum = Math.abs(map.yLineAboveUpper) + Math.abs(map.yLineBelowLower);
                int vertSum = map.yLineAboveUpper + map.yLineBelowLower;
                Direction vertFurthestDirection = map.yLineAboveUpper > Math.abs(map.yLineBelowLower) ? Direction.NORTH : Direction.SOUTH;
                int vertFurthestWall = Math.max(map.yLineAboveUpper, Math.abs(map.yLineBelowLower));

                double threshold = (double)horizFurthestWall / (double)(vertFurthestWall + horizFurthestWall);
                if (symmetries[0] == true && symmetries[1] == true) {
                    //System.out.println\("Unknown symmetry. Horizontal and vertical both potential.");
                    if (numAllyECs != 0) {
                        // Send perpendicular to longest line between Allies.
                        int sendX = allyDistances[1];
                        int sendY = allyDistances[0];
                        int maxDelta = Math.max(sendX, sendY);
                        int stop = 1;
                        for (int i=1; i<10; i++) {
                            stop=i;
                            if (maxDelta >= 32) {
                                break;
                            }
                        }
                        dArr[0] = horizFurthestDirection == Direction.EAST ? stop*sendX : -stop*sendX;
                        dArr[1] = vertFurthestDirection == Direction.NORTH ? stop*sendY : -stop*sendY;
                    } else {
                        // Randomly launch vertically, horizontally, or at 45 degrees (45 deg TODO).
                        int[] dHoriz = optimalHorizontalDestination(horizAbsSum, horizSum, horizFurthestDirection, horizFurthestWall);
                        int[] dVert = optimalVerticalDestination(vertAbsSum, vertSum, vertFurthestDirection, vertFurthestWall);
                        double rand = Math.random();
                        if (rand < threshold) {
                            dArr = dVert;
                        } else {
                            dArr = dHoriz;
                        }
                    }
                } else if (symmetries[0] == true) {
                    //System.out.println\("Horizontal Symmetry."); // send units vertically
                    dArr = optimalVerticalDestination(vertAbsSum, vertSum, vertFurthestDirection, vertFurthestWall);
                } else if (symmetries[1] == true) {
                    //System.out.println\("Vertical Symmetry.");   // send units horizontally
                    dArr = optimalHorizontalDestination(horizAbsSum, horizSum, horizFurthestDirection, horizFurthestWall);
                } else {
                    // only rotational symmetry possible
                    //System.out.println\("Only rotational symmetry.");
                    //System.out.println\("Ally Distance Horiz: " + allyDistances[0] + " Vert: " + allyDistances[1]);
                    if (numAllyECs != 0) {
                        // Send perpendicular to longest line between Allies.
                        int sendX = allyDistances[1];
                        int sendY = allyDistances[0];
                        int maxDelta = Math.max(sendX, sendY);
                        int stop = 1;
                        for (int i=1; i<10; i++) {
                            stop=i;
                            if (maxDelta >= 32) {
                                break;
                            }
                        }
                        //System.out.println\("Sending: X: " + stop*sendX + " Y:" + stop*sendY);
                        dArr[0] = horizFurthestDirection == Direction.EAST ? stop*sendX : -stop*sendX;
                        dArr[1] = vertFurthestDirection == Direction.NORTH ? stop*sendY : -stop*sendY;
                        enemyLocation = myLocation.translate(dArr[0], dArr[1]);
                        //System.out.println\("Sending to enemyLoc: " + enemyLocation);
                    } else {
                        // Send at 45 degree angle cross-map
                        int[] dHoriz = optimalHorizontalDestination(horizAbsSum, horizSum, horizFurthestDirection, horizFurthestWall);
                        int[] dVert = optimalVerticalDestination(vertAbsSum, vertSum, vertFurthestDirection, vertFurthestWall);
                        dArr[0] = dHoriz[0];
                        dArr[1] = dVert[1];
                    }
                }
            } else {
                dArr = randomDestination();
            }
            enemyLocation = myLocation.translate(dArr[0], dArr[1]);
        }
        return enemyLocation;
    }

    /**
     * Helper function for optimalDestination().
     * Returns random destination.
     */
    int[] randomDestination() {
        int signx = Math.random() < .5 ? -1 : 1;
        int signy = Math.random() < .5 ? -1 : 1;
        int dx = (int)(Math.random()*20 + 20) * signx;
        int dy = (int)(Math.random()*20 + 20) * signy;
        return new int[]{dx, dy};
    }

    /**
     * Helper function for optimalDestination() to calculate the optimal horizontal destination.
     * Send units right or left?
     * Determines EAST or WEST based on how far we know the east and west walls are (via map).
     * If we think more of the map is on the east, we will send more units eastward.
     */
    int[] optimalHorizontalDestination(int horizAbsSum, int horizSum, Direction horizFurthestDirection, int horizFurthestWall) {
        int dx = 0;
        int dy = (int)(Math.random()*3);    // add small y-randomness
        // If one side clearly larger than the other, send all units in that direction.
        if (Math.abs(horizSum) >= 32) {
            if (horizFurthestDirection == Direction.EAST) {
                // optimal direction east
                dx = horizFurthestWall;
            } else {
                // optimal direction west
                dx = -horizFurthestWall;
            }
        } else {
            // Otherwise, send units in direction proportion to how far away the walls are.
            double k = Math.random();
            if (k < map.xLineAboveUpper/horizAbsSum) {
                // optimal direction east
                dx = map.xLineAboveUpper;
            } else {
                // optimal direction west
                dx = map.xLineBelowLower;
            }
        }
        return new int[]{dx, dy};
    }

    /**
     * Helper function for optimalDestination() to calculate the optimal horizontal destination.
     * Send units up or down?
     * Determines NORTH or SOUTH based on how far we know the north and south walls are (via map).
     * If we think more of the map is north of us, we will send more units northbound.
     */
    int[] optimalVerticalDestination(int vertAbsSum, int vertSum, Direction vertFurthestDirection, int vertFurthestWall) {
        int dx = (int)(Math.random()*3);    // add small x-randomness
        int dy = 0;
        // If one side clearly larger than the other, send all units in that direction.
        if (Math.abs(vertSum) >= 32) {
            if (vertFurthestDirection == Direction.NORTH) {
                dy = vertFurthestWall;
            } else {
                dy = -vertFurthestWall;
            }
        } else {
            // Otherwise, send units in direction proportion to how far away the walls are.
            double k = Math.random();
            if (k < map.yLineAboveUpper/vertAbsSum) {
                // generate north
                dy = map.yLineAboveUpper;
            } else {
                // generate south
                dy = map.yLineBelowLower;
            }
        }
        return new int[]{dx, dy};
    }

    /**
     * Record passability and wall information of locations in vision radius
     * Placed in map. Called once, on round 4, just after ending initial bytecode-intensive EC-EC comms.
     */
    void putVisionTilesOnMap() throws GameActionException {
        for (int[] tile : SENSE_SPIRAL_ORDER) {
            MapLocation newML = myLocation.translate(tile[0], tile[1]);
            if (rc.onTheMap(newML)) {
                double pa = rc.sensePassability(newML);
                ////System.out.println\("Setting {" + tile[0] + ", " + tile[1] + "} to " + pa);
                map.set(tile[0], tile[1], pa);
                // TODO: @Nikhil what if newML contains an EC?
            } else {
                map.set(tile[0], tile[1], 0);
            }
        }
    }

    /**
     * Return furthest [xDelta, yDelta] between two ally ECs.
     * Called once on round 4 after initial allies have been established.
     * Used for optimalDestination() as input to a heuristic.
     */
    int[] furthestAllyDistances() {
        if(numAllyECs == 0) {
            return new int[]{0, 0};
        } else if (numAllyECs == 1) {
            return new int[]{Math.abs(myLocation.x - allyECLocs[0].x), Math.abs(myLocation.y - allyECLocs[0].y)};
        } else if (numAllyECs == 2) {
            int maxX = Math.max(Math.abs(myLocation.x - allyECLocs[0].x), Math.max(Math.abs(myLocation.x - allyECLocs[1].x), Math.abs(allyECLocs[1].x - allyECLocs[0].x)));
            int maxY = Math.max(Math.abs(myLocation.y - allyECLocs[0].y), Math.max(Math.abs(myLocation.y - allyECLocs[1].y), Math.abs(allyECLocs[1].y - allyECLocs[0].y)));
            return new int[]{maxX, maxY};
        }
        return new int[]{0, 0};
    }

    /**
     * Compare ally EC locations to rule out symmetry.
     * Call just after after putVisionTilesOnTheMap().
     */
    void updateSymmetryFromAllies() {
        if (numAllyECs == 0) {
            return;
        }
        boolean canEliminateHorizontal = true;
        boolean canEliminateVertical = true;
        for (int i=0; i<numAllyECs; i++) {
            // ALLY_EC * * * * me
            if(allyECLocs[i].y == myLocation.y) {
                canEliminateHorizontal = false;
                break;
            }
        }
        for (int i=0; i<numAllyECs; i++) {
            if(allyECLocs[i].x == myLocation.x) {
                canEliminateVertical = false;
                break;
            }
        }

        symmetries[0] = !canEliminateHorizontal;
        symmetries[1] = !canEliminateVertical;
    }


    /**
     * Sets an initial flag to let other ECs know I exist.
     * Then, go through IDs 10000 to 14096 to check for my allies.
     * Flags are set and verified using getSecretCode() below.
     * Currently, the check from 10k-14k takes ~50,000 bytecode across 3 rounds (Rounds 1-3).
     * Ends on round 3. You can reduce the number of bytecodes
     * used per round (but extend the number of rounds) by modifying the searchBounds array
     * and making the differences in numbers smaller. Please ensure this function finishes
     * before or on to round 3, or else, ensure nothing breaks when you change when this ends (nothing should).
     */
    void initialFlagsAndAllies() throws GameActionException {
        // Set flag on first turn
        if (currentRound == 1) {
            if (rc.canSetFlag(initialFaf.flag)) {
                setFlag(initialFaf.flag);
                //System.out.println\("I set my initial flag to: " + initialFaf.flag);
            } else {
                //System.out.println\("MAJOR ERROR: FindAllyFlag IS LIKELY WRONG: " + initialFaf.flag);
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
                    //System.out.println\("Found a POTENTIAL ally! ID: " + i + ". I now have: " + numAllyECs + " allies.");
                    //System.out.println\("Adding POTENTIAL ally " + i + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                }
            }
            firstRoundIDsToConsider.clear();
        }

        // Continue scanning for friendly ECs
        if (!scannedAllIDs) {
            int startPoint = searchBounds[searchRound];
            int endPoint = searchBounds[searchRound+1];
            //System.out.println\("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
            // We partially unroll this loop to optimize bytecode. Without unrolling, we get 14.1
            // bytecode per iteration, and with unrolling it's 12.2. This lets us do scanning in 3 turns.
            for (int i=startPoint; i<endPoint; i+=32) {
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+16)) {
                    int j = i+16;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+17)) {
                    int j = i+17;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+18)) {
                    int j = i+18;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+19)) {
                    int j = i+19;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+20)) {
                    int j = i+20;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+21)) {
                    int j = i+21;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+22)) {
                    int j = i+22;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+23)) {
                    int j = i+23;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+24)) {
                    int j = i+24;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+25)) {
                    int j = i+25;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+26)) {
                    int j = i+26;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+27)) {
                    int j = i+27;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+28)) {
                    int j = i+28;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+29)) {
                    int j = i+29;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+30)) {
                    int j = i+30;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
                if (rc.canGetFlag(i+31)) {
                    int j = i+31;
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
                        ////System.out.println\("Found a POTENTIAL ally! ID: " + j + ". I now have: " + numAllyECs + " allies.");
                        //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
            }
            //System.out.println\("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
            searchRound += 1;
            if (searchRound == searchBounds.length-1) {
                scannedAllIDs = true;
                //System.out.println\("Done finding allies.");
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
