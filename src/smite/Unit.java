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
    Direction moveLastTurn;
    MapLocation destination;
    int instruction;
    boolean spawnedSilently;

    // variables to keep track of EC sightings
    // May only need sets instead of maps.
    // But we may want to use the IDs later, in which case we will need maps.
    Map<MapLocation, Integer> enemyECLocsToIDs;
    Map<MapLocation, Integer> neutralECLocsToIDs;
    Map<MapLocation, Integer> capturedAllyECLocsToIDs;

    ArrayList<MapLocation> priorDestinations;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        moveThisTurn = Direction.CENTER;
        moveLastTurn = Direction.CENTER;
        // Add base information. If no base is found, baseID will be 0.
        // If spawned silently, then baseLocation will be any base that
        // is adjacent to the unit when it spawns, and is not necessarily
        // specifically the one that spawned it.
        baseID = 0;
        spawnedSilently = true;
        RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, allyTeam);
        for (RobotInfo robot : adjacentRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (rc.canGetFlag(robot.ID)) {
                    SpawnUnitFlag suf = new SpawnUnitFlag(rc.getFlag(robot.ID));
                    if (suf.getSchema() == Flag.SPAWN_UNIT_SCHEMA && suf.readID() == rc.getID()) {
                        //System.out.println\("Not spawned silently");
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

        // Set destination as baseLocation until it's set by EC message
        destination = baseLocation;

        // variables to keep track of EC sightings
        enemyECLocsToIDs = new HashMap<>();
        neutralECLocsToIDs = new HashMap<>();
        capturedAllyECLocsToIDs = new HashMap<>();
        priorDestinations = new ArrayList<MapLocation>();
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        moveLastTurn = moveThisTurn;
        moveThisTurn = Direction.CENTER;
        myLocation = rc.getLocation();
        parseVision();
        readECInstructions();
        //System.out.println\("Destination: " + destination);

        // Call unit-specific run method
        runUnit();

        // Common wrap-up methods
        parseVision();
        setECSightingFlag();
        setUnitUpdateFlag();
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
        if (flagSetThisRound) return;
        int minDist = 10000;
        MapLocation enemyLoc = null;
        RobotType enemyType = null;
        for (RobotInfo r : nearbyEnemies) {
            int dist = myLocation.distanceSquaredTo(r.location);
            if (dist < minDist) {
                minDist = dist;
                enemyLoc = r.location;
                enemyType = r.type;
            }
        }
        if (nearbyEnemies.length == 0) {
            RobotInfo r = getNearestEnemyFromAllies();
            if (r == null) { // default behavior if no enemy is found is to send my info
                enemyLoc = myLocation;
                enemyType = rc.getType();
                //System.out.println\("No nearest enemy known.");
            }
            else {
                enemyLoc = r.location;
                enemyType = r.type;
            }
        } else { // TODO: remove else, for debugging
            //rc.setIndicatorLine(myLocation, enemyLoc, 30, 255, 40);
        }
        //System.out.println\("My nearest enemy is a " + enemyType.toString() + " at " + enemyLoc.toString());
        //rc.setIndicatorDot(enemyLoc, 30, 255, 40);
        UnitUpdateFlag uuf = new UnitUpdateFlag(moveThisTurn, rc.getType() == RobotType.SLANDERER, enemyLoc, enemyType);
        setFlag(uuf.flag);
    }

    /**
     * Get the nearest enemy to me by listening to the allies around me.
     * Returns a RobotInfo with known information about the enemy and 0
     * as its ID. Returns null if no nearby enemy is known.
     * 
     * WARNING: Make sure parseVision() is called after the most recent
     * move before you call this function!
     */
    public RobotInfo getNearestEnemyFromAllies() throws GameActionException {
        int minDist = 10000;
        MapLocation enemyLoc = null;
        MapLocation allyLoc = null; // TODO: remove, for debugging purposes
        RobotType enemyType = null;
        for (RobotInfo r : nearbyAllies) {
            if (rc.canGetFlag(r.ID)) {
                int flag = rc.getFlag(r.ID);
                if (Flag.getSchema(flag) == Flag.UNIT_UPDATE_SCHEMA) {
                    UnitUpdateFlag uuf = new UnitUpdateFlag(flag);
                    MapLocation loc = uuf.readAbsoluteEnemyLocation(r.location);
                    if (loc != null) {
                        int dist = myLocation.distanceSquaredTo(loc);
                        enemyType = uuf.readEnemyType();
                        // Penalize non-muckrakers
                        if (enemyType != RobotType.MUCKRAKER) {
                            dist += 100;
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
        if (enemyLoc != null) {
            //rc.setIndicatorLine(myLocation, allyLoc, 30, 255, 40);
            return new RobotInfo(0, enemyTeam, enemyType, 0, 0, enemyLoc);
        }
        return null;
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
            instruction = sdf.readInstruction();
            //System.out.println\("I have my destination: " + destination.toString());
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
                    if (capturedAllyECLocsToIDs.containsKey(ri.location)) {
                        capturedAllyECLocsToIDs.remove(ri.location);
                    }
                    setECSightingFlagHelper(ri.location, enemyTeam, moveThisTurn);
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
                    setECSightingFlagHelper(ri.location, neutralTeam, moveThisTurn);
                    return;
                }
            }
        }
        for (RobotInfo ri : nearbyAllies) {
            if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (neutralECLocsToIDs.containsKey(ri.location)) {
                    capturedAllyECLocsToIDs.put(ri.location, ri.ID);
                    neutralECLocsToIDs.remove(ri.location);
                    setECSightingFlagHelper(ri.location, allyTeam, moveThisTurn);
                    return;
                } else if (enemyECLocsToIDs.containsKey(ri.location)) {
                    capturedAllyECLocsToIDs.put(ri.location, ri.ID);
                    enemyECLocsToIDs.remove(ri.location);
                    setECSightingFlagHelper(ri.location, allyTeam, moveThisTurn);
                    return;
                }
            }
        }
    }

    /**
     * Helper function to actually set an ECSightingFlag.
     */
    public void setECSightingFlagHelper(MapLocation ecLoc, Team t, Direction lastMove) throws GameActionException {
        int ecType = ECSightingFlag.ENEMY_EC;
        if (t == allyTeam) {
            ecType = ECSightingFlag.ALLY_EC;
        } else if (t == neutralTeam) {
            ecType = ECSightingFlag.NEUTRAL_EC;
        }
        ECSightingFlag ecsf = new ECSightingFlag(ecLoc, ecType, lastMove);
        setFlag(ecsf.flag);
        //System.out.println\("Sending EC Sighting at " + ecLoc);
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
     * unnecessary computation.
     */
    void fuzzyMove(MapLocation destination) throws GameActionException {
        // TODO: This is not optimal! Sometimes taking a slower move is better if its diagonal.
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight()};
        double cost = -1;
        Direction optimalDir = null;
        for (Direction dir : dirs) {
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
     * Moves towards destination, in the optimal direction or diagonal offsets based on which is
     * cheaper to move through. Assumes rc.isReady() == true, or otherwise wastes bytecode on
     * unnecessary computation. Allows orthogonal moves to unlodge.
     */
    void wideFuzzyMove(MapLocation destination) throws GameActionException {
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
     * Moves towards destination, prefering high passability terrain. Repels from nearby units of
     * the same type to avoid clustering after initial few turns.
     */
    void weightedFuzzyMove(MapLocation destination) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        Direction toDest = myLocation.directionTo(destination);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
            toDest.rotateRight().rotateRight(), toDest.opposite().rotateLeft(), toDest.opposite().rotateRight(), toDest.opposite()};
        double[] costs = new double[8];
        // Ignore repel factor in beginning and when close to target
        boolean shouldRepel = turnCount > 50 && myLocation.distanceSquaredTo(destination) > 40;
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
     * Update destination to encourage exploration if destination is off map or destination is not
     * an enemy target. Uses rejection sampling to avoid destinations near already explored areas.
     * @throws GameActionException
     */
    void updateDestinationForExploration() throws GameActionException {
        MapLocation nearDestination = myLocation;
        for (int i = 0; i < 3; i++) {
            nearDestination = nearDestination.add(nearDestination.directionTo(destination));
        }
        if (!rc.onTheMap(nearDestination) ||
            myLocation.distanceSquaredTo(destination) < rc.getType().sensorRadiusSquared
            && (!rc.onTheMap(destination) || !rc.isLocationOccupied(destination))) {
            priorDestinations.add(destination);
            boolean valid = true;
            destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
            for (int i = 0; i < priorDestinations.size(); i++) {
                if (destination.distanceSquaredTo(priorDestinations.get(i)) < 40) {
                    valid = false;
                    break;
                }
            }
            while (!valid) {
                valid = true;
                destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
                for (int i = 0; i < priorDestinations.size(); i++) {
                    if (destination.distanceSquaredTo(priorDestinations.get(i)) < 40) {
                        valid = false;
                        break;
                    }
                }
            }
        }
    }
}
