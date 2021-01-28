package smite;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;

public abstract class Unit extends Robot {

    // Base EnlightmentCenter information
    MapLocation baseLocation;
    int baseID;

    RobotInfo[] nearbyAllies;
    RobotInfo[] nearbyEnemies;
    RobotInfo[] nearbyNeutral;
    RobotInfo[] nearbyRobots;
    Direction moveThisTurn;
    MapLocation destination;
    MapLocation latestBaseDestination;

    boolean[] foundEdges;
    int[] edgeLocations;

    int instruction;
    boolean spawnedSilently;
    boolean exploreMode;
    // variables to keep track of EC sightings
    // May only need sets instead of maps.
    // But we may want to use the IDs later, in which case we will need maps.
    int sawNewAllyLastTurn;
    MapLocation newAllyLocation;
    Map<MapLocation, Integer> enemyECLocsToIDs;
    Map<MapLocation, Integer> neutralECLocsToIDs;
    Map<MapLocation, Integer> allyECLocsToIDs;

    ArrayList<MapLocation> priorDestinations;

    RobotInfo nearestSignalRobotCache;
    boolean hasPopulatedNearestSignalRobot;

    int spawnRound;

    // Muck only, needs to be accessed here
    Direction momentumDir;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        moveThisTurn = Direction.CENTER;
                                // North, East, South, West
        foundEdges = new boolean[]{false, false, false, false};
        edgeLocations = new int[]{-1, -1, -1, -1};
        // Add base information. If no base is found, baseID will be 0.
        // If spawned silently, then baseLocation will be any base that
        // is adjacent to the unit when it spawns, and is not necessarily
        // specifically the one that spawned it.
        baseID = 0;
        spawnedSilently = true;
        exploreMode = true;
        RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, allyTeam);
        for (RobotInfo robot : adjacentRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (rc.canGetFlag(robot.ID)) {
                    SpawnUnitFlag suf = new SpawnUnitFlag(rc.getFlag(robot.ID));
                    if (suf.getSchema() == Flag.SPAWN_UNIT_SCHEMA && suf.readID() == rc.getID()) {
                        // //System.out.println\("Not spawned silently");
                        spawnedSilently = false;
                        baseLocation = robot.location;
                        baseID = robot.ID;
                    }
                }
            }
        }
        // Second pass if unit determines it was spawned silently
        for (RobotInfo robot : adjacentRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                baseLocation = robot.location;
                baseID = robot.ID;
            }
        }
        sawNewAllyLastTurn = 0;
        newAllyLocation = null;
        // Set destination as baseLocation until it's set by EC message
        destination = baseLocation;
        // //System.out.println\("Constructor: " + destination);
        latestBaseDestination = destination;
        // variables to keep track of EC sightings
        enemyECLocsToIDs = new HashMap<>();
        neutralECLocsToIDs = new HashMap<>();
        allyECLocsToIDs = new HashMap<>();
        allyECLocsToIDs.put(baseLocation, baseID);
        priorDestinations = new ArrayList<MapLocation>();

        instruction = -1;
        spawnRound = rc.getRoundNum();

        momentumDir = Direction.CENTER;
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        hasPopulatedNearestSignalRobot = false;
        moveThisTurn = Direction.CENTER;
        myLocation = rc.getLocation();
        parseVision();
        findMapEdges();
        readECInstructions();
        switchToLatestBaseDestination();
        // //System.out.println\("Destination: " + destination);
        if (destination != null) {
            //rc.setIndicatorDot(destination, 255, 255, 255);
        }

        // Call unit-specific run method
        runUnit();

        // Common wrap-up methods
        // //System.out.println\("11: " + Clock.getBytecodesLeft());
        if(sawNewAllyLastTurn == 1) {
            setMidGameAllyIDFlag(moveThisTurn);
            sawNewAllyLastTurn = 2;
            // //System.out.println\("11.1: " + Clock.getBytecodesLeft());
        } else if (sawNewAllyLastTurn == 2) {
            setMidGameAllyLocFlag(moveThisTurn);
            sawNewAllyLastTurn = 0;
            // //System.out.println\("11.2: " + Clock.getBytecodesLeft());
        } else {
            setECSightingFlag();
            // //System.out.println\("11.3: " + Clock.getBytecodesLeft());
        }
        if (rc.getType() == RobotType.MUCKRAKER) {
            parseVision();
        }
        setUnitUpdateFlag();
        // //System.out.println\("12: " + Clock.getBytecodesLeft());

        checkIfMapInfoFlagPersists();

    }

    /**
     * Function to be overridden by the unit classes. This is where
     * all unit-specific run stuff happens.
     */
    public void runUnit() throws GameActionException {
    }

    /**
     * If flag has not been set this turn, set a unit update flag by
     * looking around for the nearest enemy. If none is found, look
     * for nearby allies who might inform me about the nearest enemy.
     *
     * WARNING: Make sure parseVision() is called after the most recent
     * move before you call this function!
     */
    public void setUnitUpdateFlag() throws GameActionException {
        if (flagSetThisRound) {
            // //System.out.println\("Flag already set.");
            return;
        }
        // //System.out.println\("Flag not already set.");
        int minDist = 100000;
        MapLocation enemyLoc = null;
        RobotType enemyType = null;
        for (RobotInfo r : nearbyEnemies) {
            int dist = myLocation.distanceSquaredTo(r.location);
            if ((dist < minDist && enemyType != RobotType.SLANDERER && r.type != RobotType.SLANDERER)
                || (enemyType != RobotType.SLANDERER && r.type == RobotType.SLANDERER)
                || (dist < minDist && enemyType == RobotType.SLANDERER && r.type == RobotType.SLANDERER)) {
                minDist = dist;
                enemyLoc = r.location;
                enemyType = r.type;
                if (enemyType == RobotType.SLANDERER) {
                    //System.out.println\("TELLING EC ABOUT ENEMY SLANDERER");
                }
            }
        }
        // If enemyType is not muckraker (null is also not muckraker), use smoke signals
        if (turnCount != 1) {
            RobotInfo r = getNearestEnemyFromAllies();
            if (r != null && enemyType != RobotType.MUCKRAKER && r.type == RobotType.MUCKRAKER) {
                enemyLoc = r.location;
                enemyType = r.type;
            }
        }
        // Case where enemyLoc is still null is handled in UnitUpdateFlag constructor.
        UnitUpdateFlag uuf = new UnitUpdateFlag(rc.getType() == RobotType.SLANDERER, instruction == SpawnDestinationFlag.INSTR_DEFEND, enemyLoc, enemyType);
        ////System.out.println\("My nearest enemy is a " + enemyType.toString() + " at " + enemyLoc.toString());
        // //rc.setIndicatorDot(enemyLoc, 30, 255, 40);
        setFlag(uuf.flag);
    }

    /**
     * Get the nearest enemy to me by listening to the allies around me.
     * Returns a RobotInfo with known information about the enemy and 0
     * as its ID. Returns null if no nearby enemy is known.
     */
    public RobotInfo getNearestEnemyFromAllies() throws GameActionException {
        if (hasPopulatedNearestSignalRobot) {
            return nearestSignalRobotCache;
        }
        // //System.out.println\("nearest: " + Clock.getBytecodesLeft());
        int minDist = 10000;
        MapLocation enemyLoc = null;
        MapLocation allyLoc = null; // TODO: remove, for debugging purposes
        RobotType enemyType = null;
        for (int i = 0; i < Math.min(10, nearbyAllies.length); i++) {
            RobotInfo r = nearbyAllies[i];
            if (rc.canGetFlag(r.ID)) {
                int flag = rc.getFlag(r.ID);
                if (Flag.getSchema(flag) == Flag.UNIT_UPDATE_SCHEMA) {
                    UnitUpdateFlag uuf = new UnitUpdateFlag(flag);
                    MapLocation loc = uuf.readAbsoluteLocation(r.location);
                    // Only listen about enemies from units closer to it than you. This ensures it is a DAG
                    // and prevents echoing on dead units.
                    if (uuf.readHasNearbyEnemy() && r.location.distanceSquaredTo(loc) < myLocation.distanceSquaredTo(loc)) {
                        int dist = myLocation.distanceSquaredTo(loc);
                        enemyType = uuf.readEnemyType();
                        // Penalize non-muckrakers
                        if (enemyType != RobotType.MUCKRAKER) {
                            dist += 10000;
                        }
                        if (dist < minDist) {
                            minDist = dist;
                            enemyLoc = loc;
                            allyLoc = r.location;
                        }
                    }
                }
            }
        }
        // //System.out.println\("nearest: " + Clock.getBytecodesLeft());
        hasPopulatedNearestSignalRobot = true;
        if (enemyLoc != null) {
            //rc.setIndicatorLine(myLocation, allyLoc, 30, 255, 40);
            //rc.setIndicatorLine(myLocation, enemyLoc, 30, 30, 255);
            nearestSignalRobotCache = new RobotInfo(0, enemyTeam, enemyType, 0, 0, enemyLoc);
            return nearestSignalRobotCache;
        } else {
            nearestSignalRobotCache = null;
            return null;
        }
    }

    /**
     * Read my destination from the EC that spawned me the turn
     * after I was spawned.
     */
    public boolean readECInstructions() throws GameActionException {
        if (spawnedSilently) {
            return false;
        }
        if (turnCount == 2) { // expect SpawnDestinationFlag
            if (!rc.canGetFlag(baseID)) {
                //System.out.println\("MAJOR ERROR: I was expecting a flag from the EC, but can't see one!");
                return false;
            }
            SpawnDestinationFlag sdf = new SpawnDestinationFlag(rc.getFlag(baseID));
            if (sdf.getSchema() != Flag.SPAWN_DESTINATION_SCHEMA) {
                //System.out.println\("MAJOR ERROR: I was expecting a SpawnDestinationFlag from the EC, but didn't get one!");
                return false;
            }
            destination = sdf.readAbsoluteLocation(myLocation);
            // //System.out.println\("read instr: " + destination);
            instruction = sdf.readInstruction();
            exploreMode = sdf.readGuess();
            // //System.out.println\("I have my destination: " + destination.toString());
            // //System.out.println\("I have my instruction: " + instruction);
            // //System.out.println\("Explore Mode Status: " + exploreMode);
            return true;
        }
        return false;
    }

    /**
     * Update the visible bots around me. Costs 400 bytecode.
     */
    public void parseVision() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
        nearbyEnemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemyTeam);
        nearbyAllies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, allyTeam);
        nearbyNeutral = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, neutralTeam);
    }

    /**
     * Checks at the end of round if the MapInfoFlag with edge information
     * is actually going to be sent out. If it is, then we change a boolean
     * in the foundEdges array to not send out another flag for that direction.
     */
    public void checkIfMapInfoFlagPersists() {
        if (Flag.getSchema(flagDataSetThisRound) == Flag.MAP_INFO_SCHEMA) {
            MapInfoFlag mif = new MapInfoFlag(flagDataSetThisRound);
            Direction flagDir = mif.readEdgeDirection();
            switch(flagDir) {
                case NORTH:
                    foundEdges[0] = true;
                    break;
                case EAST:
                    foundEdges[1] = true;
                    break;
                case SOUTH:
                    foundEdges[2] = true;
                    break;
                case WEST:
                    foundEdges[3] = true;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Runs near the beginning of every turn. Starts at the four cardinal directions
     * of the sensor radius and looks for map edges it hasn't already communicated. Sets
     * a flag if it finds one.
     */
    public void findMapEdges() throws GameActionException {
        boolean allTrue = true;
        for (int i=0; i<4; i++) {
            if (foundEdges[i] == false) {
                allTrue = false;
            }
        }
        if (allTrue) {
            return;
        }
        int furthestSight = (int)Math.floor(Math.sqrt(rc.getType().sensorRadiusSquared));
        int distClosestOff = -1;
        for (int i=0; i<4; i++) {
            if (foundEdges[i] == true) {
                continue;
            } else {
                switch (i) {
                    case 0:
                        // Look North for Edge of map.
                        distClosestOff = -1;
                        for (int j=furthestSight; j>=0; j--) {
                            MapLocation testLoc = myLocation.translate(0, j);
                            if (!rc.onTheMap(testLoc)) {
                                distClosestOff = j;
                            } else if (rc.onTheMap(testLoc)) {
                                if (distClosestOff != -1) {
                                    edgeLocations[0] = testLoc.y-1;
                                    setMapEdgeFlag(Direction.NORTH, testLoc.translate(0, 1));
                                    return;
                                } else {
                                    break;  // no edge north.
                                }
                            }
                        }
                        break;
                    case 1:
                        // Look East for Edge of map.
                        distClosestOff = -1;
                        for (int j=furthestSight; j>=0; j--) {
                            MapLocation testLoc = myLocation.translate(j, 0);
                            if (!rc.onTheMap(testLoc)) {
                                distClosestOff = j;
                            } else if (rc.onTheMap(testLoc)) {
                                if (distClosestOff != -1) {
                                    edgeLocations[1] = testLoc.x-1;
                                    setMapEdgeFlag(Direction.EAST, testLoc.translate(1, 0));
                                    return;
                                } else {
                                    break;  // no edge east.
                                }
                            }
                        }
                        break;
                    case 2:
                        // Look South for Edge of map.
                        distClosestOff = -1 ;
                        for (int j=furthestSight; j>=0; j--) {
                            MapLocation testLoc = myLocation.translate(0, -j);
                            if (!rc.onTheMap(testLoc)) {
                                distClosestOff = j;
                            } else if (rc.onTheMap(testLoc)) {
                                if (distClosestOff != -1) {
                                    edgeLocations[2] = testLoc.y+1;
                                    setMapEdgeFlag(Direction.SOUTH, testLoc.translate(0, -1));
                                    return;
                                } else {
                                    break;  // no edge south.
                                }
                            }
                        }
                        break;
                    case 3:
                        // Look West for Edge of map.
                        distClosestOff = -1;
                        for (int j=furthestSight; j>=0; j--) {
                            MapLocation testLoc = myLocation.translate(-j, 0);
                            if (!rc.onTheMap(testLoc)) {
                                distClosestOff = j;
                            } else if (rc.onTheMap(testLoc)) {
                                if (distClosestOff != -1) {
                                    edgeLocations[3] = testLoc.x+1;
                                    setMapEdgeFlag(Direction.WEST, testLoc.translate(-1, 0));
                                    return;
                                } else {
                                    break;  // no edge west.
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    /**
     * Helper function for findMapEdges() which actually sets the map flag.
     */
    public void setMapEdgeFlag(Direction dirToSet, MapLocation justOffMapLoc) throws GameActionException {
        MapInfoFlag mif = new MapInfoFlag(dirToSet, justOffMapLoc);
        setFlag(mif.flag);
    }

    /**
     * Set an ECSightingFlag if I see an EC I'm not already aware of.
     * This is important, so it overwrites an existing flag if there is one.
     */
    public void setECSightingFlag() throws GameActionException {
        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (!enemyECLocsToIDs.containsKey(ri.location)) {
                    enemyECLocsToIDs.put(ri.location, ri.ID);
                    // If EC used be neutral, remove it from neutral map.
                    if (neutralECLocsToIDs.containsKey(ri.location)) {
                        neutralECLocsToIDs.remove(ri.location);
                    }
                    // If EC used to be captured ally, remove it from captured allies map
                    if (allyECLocsToIDs.containsKey(ri.location)) {
                        allyECLocsToIDs.remove(ri.location);
                    }
                    setECSightingFlagHelper(ri.location, enemyTeam, moveThisTurn, ri.influence);
                    // we may not want to return in the future when there is more computation to be done.
                    // than just setting a flag.
                    return;
                }

            }
        }
        for (RobotInfo ri : nearbyNeutral) {
            if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (!neutralECLocsToIDs.containsKey(ri.location)) {
                    neutralECLocsToIDs.put(ri.location, ri.ID);
                    setECSightingFlagHelper(ri.location, neutralTeam, moveThisTurn, ri.influence);
                    return;
                }
            }
        }
        if (sawNewAllyLastTurn == 0) {
            for (RobotInfo ri : nearbyAllies) {
                if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                    if (neutralECLocsToIDs.containsKey(ri.location)) {
                        allyECLocsToIDs.put(ri.location, ri.ID);
                        neutralECLocsToIDs.remove(ri.location);
                        sawNewAllyLastTurn = 1;
                        setECSightingFlagHelper(ri.location, allyTeam, moveThisTurn, ri.influence);
                        return;
                    } else if (enemyECLocsToIDs.containsKey(ri.location)) {
                        allyECLocsToIDs.put(ri.location, ri.ID);
                        enemyECLocsToIDs.remove(ri.location);
                        sawNewAllyLastTurn = 1;
                        setECSightingFlagHelper(ri.location, allyTeam, moveThisTurn, ri.influence);
                        return;
                    } else if (!allyECLocsToIDs.containsKey(ri.location)) {
                        allyECLocsToIDs.put(ri.location, ri.ID);
                        sawNewAllyLastTurn = 1;
                        setECSightingFlagHelper(ri.location, allyTeam, moveThisTurn, ri.influence);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Helper function to actually set an ECSightingFlag.
     */
    public void setECSightingFlagHelper(MapLocation ecLoc, Team t, Direction lastMove, int inf) throws GameActionException {
        int ecType = ECSightingFlag.ENEMY_EC;
        if (t == allyTeam) {
            ecType = ECSightingFlag.ALLY_EC;
        } else if (t == neutralTeam) {
            ecType = ECSightingFlag.NEUTRAL_EC;
        }
        ECSightingFlag ecsf = new ECSightingFlag(ecLoc, ecType, inf);
        setFlag(ecsf.flag);
        // //System.out.println\("Sending EC Sighting at " + ecLoc);
    }

    /**
     * Tell new ally ECs your base ID. This occurs when you encounter any ally EC
     * at all for the first time, except for your base. If you encounter an ally EC
     * that we captured, you first communicate that you found a captured ally to everyone,
     * and then you send this message the next round (hence the var sawNewAllyLastTurn).
     * Why is this necessary? We want mid-game ECs to know the original bases, so they
     * can read that base's messages.
     *
     * Returns without doing anything if unit does not have a base.
     */
    public void setMidGameAllyIDFlag(Direction lastMove) throws GameActionException {
        if (baseID == 0) return;
        MidGameAllyFlag maf = new MidGameAllyFlag(lastMove, MidGameAllyFlag.ID_MAF, baseID);
        setFlag(maf.flag);
    }

    /**
     * Tell new ally ECs your base location. This occurs the round after
     * setMidGameAllyIDFlag.
     */
     public void setMidGameAllyLocFlag(Direction lastMove) throws GameActionException {
         MidGameAllyFlag maf = new MidGameAllyFlag();
         maf.writeLastMove(lastMove);
         maf.writeType(MidGameAllyFlag.LOCATION_MAF);
         maf.writeLocation(baseLocation);
         setFlag(maf.flag);
        //  //System.out.println\("Telling ally EC about my base loc: " + baseLocation);
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
            move(dir);
            return true;
        }
        moveThisTurn = Direction.CENTER;
        return false;
    }

    /**
     * Use this function instead of rc.move(). Still need
     * to verify canMove before calling this.
     */
    void move(Direction dir) throws GameActionException {
        rc.move(dir);
        moveThisTurn = dir;
        myLocation = myLocation.add(dir);
    }

    /**
     * Moves towards destination, in the optimal direction or diagonal offsets based on which is
     * cheaper to move through. Assumes rc.isReady() == true, or otherwise wastes bytecode on
     * unnecessary computation. Allows orthogonal moves to unlodge.
     */
    void fuzzyMove(MapLocation destination) throws GameActionException {
        // TODO: This is not optimal! Sometimes taking a slower move is better if its diagonal.
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};
        double cost = -1;
        Direction optimalDir = null;
        for (int i = 0; i < dirs.length; i++) {
            // Prefer forward moving steps over horizontal shifts
            if (i > 2 && cost > 0) {
                break;
            }
            Direction dir = dirs[i];
            if (rc.canMove(dir)) {
                double newCost = rc.sensePassability(myLocation.add(dir));
                // add epsilon boost to forward direction
                if (dir == toDest) {
                    newCost += 0.001;
                }
                if (newCost > cost) {
                    cost = newCost;
                    optimalDir = dir;
                }
            }
        }
        if (optimalDir != null) {
            move(optimalDir);
        }
    }

    /**
     * Moves towards destination, prefering high passability terrain, with an option
     * to repel friendly units of the same type.
     *
     * Note: Politicians will repel from slanderers, since they can't tell the difference.
     */
    void weightedFuzzyMove(MapLocation destination, boolean shouldRepel) throws GameActionException {
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
            toDest.rotateRight().rotateRight(), toDest.opposite().rotateLeft(), toDest.opposite().rotateRight(), toDest.opposite()};
        double[] costs = new double[8];
        for (int i = 0; i < dirs.length; i++) {
            MapLocation newLocation = myLocation.add(dirs[i]);
            // Movement invalid, set higher cost than starting value
            if (!rc.onTheMap(newLocation)) {
                costs[i] = -999999;
                continue;
            }
            double cost = (rc.sensePassability(newLocation) - 1) * 60;
            // Preference tier for moving towards target
            if (i >= 3) {
                cost -= 60;
            }
            if (shouldRepel) {
                for (RobotInfo robot : nearbyAllies) {
                    if (robot.type == rc.getType()) {
                        cost -= 40 - newLocation.distanceSquaredTo(robot.location);
                    }
                }
            }
            costs[i] = cost;
        }
        double cost = -99999;
        Direction optimalDir = null;
        for (int i = 0; i < dirs.length; i++) {
            Direction dir = dirs[i];
            if (rc.canMove(dir)) {
                double newCost = costs[i];
                // add epsilon boost to forward direction
                if (dir == toDest) {
                    newCost += 0.001;
                }
                if (newCost > cost) {
                    cost = newCost;
                    optimalDir = dir;
                }
            }
        }
        if (optimalDir != null) {
            move(optimalDir);
        }
    }

    /**
     * Repels from nearby units of the same type after the initial
     * few turns to avoid clustering.
     */
    void weightedFuzzyMove(MapLocation destination) throws GameActionException {
        // Ignore repel factor in beginning and when close to target
        boolean shouldRepel = turnCount > 50 && myLocation.distanceSquaredTo(destination) > 40;
        weightedFuzzyMove(destination, shouldRepel);
    }

    /**
     * Update destination for non-slanderers.
     * We switch to the latest destination our base is sending out for non-slanderer units
     * if we can see our destination and there is an ally EC on it,
     * and the base is telling us a different destination.
     * Returns a boolean indicating whether we switched or not.
     */
    boolean switchToLatestBaseDestination() throws GameActionException {
        if (rc.getType() == RobotType.SLANDERER) {
            return false;
        }
        if (turnCount <= 3) {
            return false;
        }

        MapLocation potentialDest = null;
        boolean baseGivingExplore = true;
        int newInstruction = instruction;
        if (rc.canGetFlag(baseID)) {
            int flagInt = rc.getFlag(baseID);
            int flagSchema = Flag.getSchema(flagInt);
            switch (flagSchema) {
                case Flag.NO_SCHEMA:
                    break;
                case Flag.EC_SIGHTING_SCHEMA:
                    // Not relevant, ECs do not send such flags to robots.
                    break;
                case Flag.MAP_INFO_SCHEMA:
                    // Not relevant, ECs do not send such flags to robots.
                    break;
                case Flag.LOCATION_SCHEMA:
                    break;
                case Flag.SPAWN_UNIT_SCHEMA:
                    // Not relevant, handled elsewhere.
                    break;
                case Flag.SPAWN_DESTINATION_SCHEMA:
                    // Use this to figure out where our base is sending currently produced unit
                    SpawnDestinationFlag sdf = new SpawnDestinationFlag(flagInt);
                    newInstruction = sdf.readInstruction();
                    //System.out.println\("I see my base has given instruction: " + newInstruction);
                    if (newInstruction != SpawnDestinationFlag.INSTR_SLANDERER &&
                        ((newInstruction == SpawnDestinationFlag.INSTR_MUCKRAKER ||
                        newInstruction == SpawnDestinationFlag.INSTR_MUCK_TO_SLAND) ^
                        rc.getType() == RobotType.POLITICIAN)) {
                        // the robot spawned is going to an enemy, we may want to go here.
                        potentialDest = sdf.readAbsoluteLocation(myLocation);
                        baseGivingExplore = sdf.readGuess();
                    }
                    break;
                case Flag.UNIT_UPDATE_SCHEMA:
                    // Not relevant, ECs do not send such flags to ECs.
                    break;
                case Flag.MIDGAME_ALLY_SCHEMA:
                    // Not relevant, ECs do not send such flags to ECs.
                    break;
            }
        }


        if (potentialDest == null) {
            // //System.out.println\("Did not switch, base not giving destinations.");
            return false;
        }
        if (baseGivingExplore) {
            // //System.out.println\("Did not switch, base giving explore.");
            return false;
        }
        if (potentialDest.equals(destination)) {
            // //System.out.println\("Did not switch, base giving same destination.");
            return false;
        }

        boolean canSenseDestination = rc.canSenseLocation(destination);
        RobotInfo destRobot = null;
        if (canSenseDestination) {
            destRobot = rc.senseRobotAtLocation(destination);
        }

        if (rc.getType() == RobotType.MUCKRAKER
            && newInstruction == SpawnDestinationFlag.INSTR_MUCK_TO_SLAND
            && instruction != SpawnDestinationFlag.INSTR_MUCK_TO_SLAND
            && rc.getRoundNum() > 100) {
            //System.out.println\("SWITCHING TO SLAND DESTINATION.");
            destination = potentialDest;
            momentumDir = myLocation.directionTo(destination);
            exploreMode = false;
            instruction = SpawnDestinationFlag.INSTR_MUCK_TO_SLAND;
            return true;
        }
        // Reroute if in explore mode and spawned after first 100 turns
        if (exploreMode && spawnRound > 100) {
            // //System.out.println\("Explore Reroute: " + potentialDest);
            destination = potentialDest;
            momentumDir = myLocation.directionTo(destination);;
            exploreMode = false;
            if (rc.getType() == RobotType.MUCKRAKER) {
                instruction  = newInstruction;
            }
            return true;

            // Old Criteria
            // If you are in explore mode, and the following conditions hold:
            // 1) If three steps to your destination is off the map
            // 2) or you can sense your destination and it is off the map
            // 3) or you can sense your destination and your destination has a robot on it and that robot is not an enemy EC
            // then change your destination

            // MapLocation nearDestination = myLocation;
            // for (int i = 0; i < 3; i++) {
            //     nearDestination = nearDestination.add(nearDestination.directionTo(destination));
            // }

            // if (!rc.onTheMap(nearDestination) || canSenseDestination &&
            //     (!rc.onTheMap(destination) ||
            //     (destRobot != null && !(destRobot.team == enemyTeam && destRobot.type == RobotType.ENLIGHTENMENT_CENTER)))) {
            //     destination = potentialDest;
            //     exploreMode = false;
            //     // //System.out.println\("Re-routing to latest base destination!! " + potentialDest);
            //     return true;
            // } else {
            //     // //System.out.println\("Did not switch for personal location reasons.");
            // }
        }

        // If you are NOT in explore mode, and the following conditions hold
        // 1) If you can sense your destination
        // 2) AND your destination has an ally EC on it
        // Then change destinations.
        // Explanation: Presumably not in explore mode, your EC really wants you to go to your destination, so the conditions are stricter.
        if (!exploreMode && canSenseDestination && destRobot != null && destRobot.team == allyTeam
                && destRobot.type == RobotType.ENLIGHTENMENT_CENTER) {
            destination = potentialDest;
            momentumDir = myLocation.directionTo(destination);
            exploreMode = false;
            if (rc.getType() == RobotType.MUCKRAKER) {
                instruction  = newInstruction;
            }
            // //System.out.println\("Re-routing to latest base destination!! " + potentialDest);
            return true;
        } else if (!exploreMode && rc.getType() == RobotType.MUCKRAKER && (canSenseDestination || nearbyAllies.length > 20)) {
            if (nearbyAllies.length > 20 || !(destRobot != null && destRobot.team == enemyTeam && destRobot.type == RobotType.SLANDERER)) {
                destination = potentialDest;
                momentumDir = myLocation.directionTo(destination);
                exploreMode = false;
                instruction  = newInstruction;
            }
        }
        return false;
    }
    /**
     * Update destination to encourage exploration if destination is off map or destination is not
     * an enemy target. Uses rejection sampling to avoid destinations near already explored areas.
     * @throws GameActionException.
     * TODO: Make this smarter for non-slanderers. Listen to your base and, if possible,
     * go where your base is telling newly spawned units of your type to go.
     */
    void updateDestinationForExploration(boolean isECHunter) throws GameActionException {
        if (turnCount == 1 || turnCount == 2) {
            return;
        }
        MapLocation nearDestination = myLocation;
        // //System.out.println\(destination);
        if (destination != null) {
            for (int i = 0; i < 3; i++) {
                nearDestination = nearDestination.add(nearDestination.directionTo(destination));
            }
        }
        // //System.out.println\("Explore: " + nearDestination);
        // Reroute if 1) nearDestination not on map or 2) can sense destination and it's not on the map
        // or it's not occupied (so no EC) or 3) the EC is a neutral EC and we're not hunting the EC
        if (destination == null || !rc.onTheMap(nearDestination) ||
            (rc.canSenseLocation(destination)
            && (!rc.onTheMap(destination)
                || !rc.isLocationOccupied(destination)
                || (rc.senseRobotAtLocation(destination).team == neutralTeam && !isECHunter)
                || rc.senseRobotAtLocation(destination).team == allyTeam))) {
            //System.out.println\("Deets, rerouting");
            if (rc.getType() == RobotType.MUCKRAKER) {
                instruction = SpawnDestinationFlag.INSTR_MUCKRAKER;
                muckrackerRerouteDestination();
                return;
            }
            if (destination != null) {
                priorDestinations.add(destination);
            }
            if (baseLocation == null) {
                baseLocation = myLocation;
            }
            boolean valid = true;
            int dxexplore = (int)(Math.random()*80);
            int dyexplore = 120 - dxexplore;
            dxexplore = Math.random() < .5 ? dxexplore : -dxexplore;
            dyexplore = Math.random() < .5 ? dyexplore : -dyexplore;
            destination = new MapLocation(baseLocation.x + dxexplore, baseLocation.y + dyexplore);
            exploreMode = true;
            for (int i = 0; i < priorDestinations.size(); i++) {
                if (destination.distanceSquaredTo(priorDestinations.get(i)) < 40) {
                    valid = false;
                    break;
                }
            }
            // if (edgeLocations[0] != -1 && edgeLocations[0] < destination.y
            //     || edgeLocations[1] != -1 && edgeLocations[1] < destination.x
            //     || edgeLocations[2] != -1 && edgeLocations[2] > destination.y
            //     || edgeLocations[3] != -1 && edgeLocations[3] > destination.x) {
            //     valid = false;
            // }
            while (!valid) {
                valid = true;
                destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
                // if (edgeLocations[0] != -1 && edgeLocations[0] < destination.y
                //     || edgeLocations[1] != -1 && edgeLocations[1] < destination.x
                //     || edgeLocations[2] != -1 && edgeLocations[2] > destination.y
                //     || edgeLocations[3] != -1 && edgeLocations[3] > destination.x) {
                //     valid = false;
                // }
                // else {
                    for (int i = 0; i < priorDestinations.size(); i++) {
                        if (destination.distanceSquaredTo(priorDestinations.get(i)) < 40) {
                            valid = false;
                            break;
                        }
                    }
                // }
            }
            // //System.out.println\("Exploration dest: " + destination);
        }
    }

    void muckrackerRerouteDestination() throws GameActionException {
        return;
    }
}
