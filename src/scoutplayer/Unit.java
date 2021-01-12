package scoutplayer;

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
    boolean spawnedSilently;

    // variables to keep track of EC sightings
    // May only need sets instead of maps.
    // But we may want to use the IDs later, in which case we will need maps.
    Map<MapLocation, Integer> enemyECLocsToIDs;
    Map<MapLocation, Integer> neutralECLocsToIDs;

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
                        System.out.println("Not spawned silently");
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

        // variables to keep track of EC sightings
        enemyECLocsToIDs = new HashMap<>();
        neutralECLocsToIDs = new HashMap<>();

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
    }

    /**
     * TODO: @Nikhil
     * If I see an EC that I am not already aware of, send a flag
     */
    public void setECSightingFlag(MapLocation ecLoc, Team t, Direction lastMove) throws GameActionException {
        int ecType = (t == enemyTeam) ? ECSightingFlag.ENEMY_EC : ECSightingFlag.NEUTRAL_EC;
        ECSightingFlag ecsf = new ECSightingFlag(ecLoc, ecType, lastMove);
        setFlag(ecsf.flag);
        System.out.println("Sending EC Sighting at " + ecLoc);
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
                System.out.println("MAJOR ERROR: I was expecting a flag from the EC, but can't see one!");
                return false;
            }
            SpawnDestinationFlag sdf = new SpawnDestinationFlag(rc.getFlag(baseID));
            if (sdf.getSchema() != Flag.SPAWN_DESTINATION_SCHEMA) {
                System.out.println("MAJOR ERROR: I was expecting a SpawnDestinationFlag from the EC, but didn't get one!");
                return false;
            }
            destination = sdf.readAbsoluteLocation(myLocation);
            System.out.println("I have my destination: " + destination.toString());
            return true;
        }
        return false;
    }

    public void parseVision() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
        nearbyEnemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemyTeam);
        nearbyAllies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, allyTeam);
        nearbyNeutral = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, neutralTeam);
        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (!enemyECLocsToIDs.containsKey(ri.location)) {
                    enemyECLocsToIDs.put(ri.location, ri.ID);
                    // If EC used be neutral, remove it from neutral map.
                    if (neutralECLocsToIDs.containsKey(ri.location)) {
                        neutralECLocsToIDs.remove(ri.location);
                    }
                    setECSightingFlag(ri.location, enemyTeam, moveLastTurn);
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
                    setECSightingFlag(ri.location, neutralTeam, moveLastTurn);
                    return;
                }
            }
        }
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
