package scoutplayer;

import battlecode.common.*;

public abstract class Unit extends Robot {
    
    // Base EnlightmentCenter information
    MapLocation baseLocation;
    int baseID;

    RobotInfo[] nearbyAllies;
    RobotInfo[] nearbyEnemies;
    RobotInfo[] nearbyRobots;
    Direction moveThisTurn;

    MapLocation destination;
    boolean spawnedSilently;

    public Unit(RobotController rc) throws GameActionException {
        super(rc);
        moveThisTurn = Direction.CENTER;
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
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        moveThisTurn = Direction.CENTER;
        myLocation = rc.getLocation();
        parseVision();
        readECInstructions();
        setECSightingFlag();
    }

    /**
     * TODO: @Nikhil
     * If I see an EC that I am not already aware of, send a flag
     */
    public void setECSightingFlag() throws GameActionException {
        // TODO
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
}