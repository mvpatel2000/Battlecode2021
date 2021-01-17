package smite;

import battlecode.common.*;
import java.util.*;

public class Politician extends Unit {

    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTH = {{1,4},{-1,4},{2,4},{-2,4},{0,5},{3,4},{4,3},{5,0},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHEAST = {{0,4},{4,0},{1,4},{4,1},{3,3},{2,4},{4,2},{4,-2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_EAST = {{4,1},{4,-1},{4,2},{4,-2},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5}};
    final static int[][] NEW_SENSED_LOCS_SOUTHEAST = {{4,0},{0,-4},{4,-1},{1,-4},{3,-3},{4,2},{4,-2},{2,-4},{-2,-4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4}};
    final static int[][] NEW_SENSED_LOCS_SOUTH = {{1,-4},{-1,-4},{2,-4},{-2,-4},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0}};
    final static int[][] NEW_SENSED_LOCS_SOUTHWEST = {{0,-4},{-4,0},{-1,-4},{-4,-1},{-3,-3},{2,-4},{-2,-4},{-4,-2},{-4,2},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3}};
    final static int[][] NEW_SENSED_LOCS_WEST = {{-4,-1},{-4,1},{-4,-2},{-4,2},{0,5},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_NORTHWEST = {{0,4},{-4,0},{-4,1},{-1,4},{-3,3},{2,4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final static int[][] NEW_SENSED_LOCS_CENTER = {};

    public final static int INITIAL_COOLDOWN = 10;

    boolean onlyECHunter;

    // MapTerrainQueue mtq;

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
        // mtq = new MapTerrainQueue(RobotType.POLITICIAN);
        onlyECHunter = rc.getInfluence() > 499;
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // Converted politician or slanderer turned politician. Set baseLocation and destination.
        if (baseLocation == null) {
            baseLocation = myLocation;
            parseVision();
            // If enemies nearby, take nearest as destination
            if (nearbyEnemies.length > 0) {
                RobotInfo nearestRobot = null;
                int nearestRobotDistSquared = 1000;
                for (RobotInfo robot : nearbyEnemies) {
                    int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                    if (robotDistSquared < nearestRobotDistSquared) {
                        nearestRobot = robot;
                        nearestRobotDistSquared = robotDistSquared;
                    }
                }
                destination = nearestRobot.location;
            }
            else {
                RobotInfo nearestSignalRobot = getNearestEnemyFromAllies();
                // Take nearest smoke signal robot as destination
                if (nearestSignalRobot != null) {
                    destination = nearestSignalRobot.location;
                }
                // Pick random destination
                else {
                    destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
                }
            }
        }

        updateDestinationForExploration();
        updateDestinationForECHunting();

        if (onlyECHunter) {
            for (RobotInfo robot : nearbyRobots) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER && robot.team != allyTeam) {
                    destination = robot.location;
                    exploreMode = false;
                    break;
                }
            }
        }

        considerBoostEC();
        considerAttack(onlyECHunter, false);

        movePolitician();


        // else if (mtq.hasRoom()) { // move if queue isn't full
        // } else if (!mtq.hasRoom()) {
        //     // //System.out.println\("MapTerrainQueue full; not moving this round.");
        // }
        // mtq.step(rc, moveThisTurn, rc.getLocation());
        // MapTerrainFlag mtf = new MapTerrainFlag();
        // mtf.writeLastMove(moveThisTurn);
        // for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
        //     if (mtq.isEmpty()) break;
        //     MapTerrain terrain = mtq.pop();
        //     mtf.writePassability(terrain.pa);
        //     // //System.out.println\("Added to flag: " + terrain.loc.toString() + " has passability " + terrain.pa);
        // }
        // setFlag(mtf.getFlag());
    }

    /**
     * Redirect politicians to nearby EC if they can kill it.
     * @throws GameActionException
     */
    void updateDestinationForECHunting() throws GameActionException {
        double totalDamage = rc.getConviction() * rc.getEmpowerFactor(allyTeam, 0) - 10;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER && (robot.team == enemyTeam || (robot.team == neutralTeam && robot.conviction <= totalDamage))) {
                destination = robot.location;
                exploreMode = false;
                return;
            }
        }
    }

    /**
     * ECHunters bee-line towards enemy ECs. Normal politicians weighted move and seek out nearby
     * muckrakers if they're uncovered.
     * @throws GameActionException
     */
    void movePolitician() throws GameActionException {
        // ECHunters ignore other units
        if (onlyECHunter) {
            fuzzyMove(destination);
            return;
        }
        double totalDamage = rc.getConviction() * rc.getEmpowerFactor(allyTeam, 0) - 10;
        RobotInfo nearestMuckraker = null;
        int nearestMuckrakerDistSquared = 100;
        for (RobotInfo robot : nearbyEnemies) {
            int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
            // Chase the nearest muckraker that you can kill
            if (robot.type == RobotType.MUCKRAKER && robotDistSquared < nearestMuckrakerDistSquared
                && totalDamage > robot.conviction) {
                nearestMuckraker = robot;
                nearestMuckrakerDistSquared = robotDistSquared;
            }
        }
        // If no nearby Muckrakers, continue weighted movement.
        if (nearestMuckraker == null) {
            // Consider using weightedFuzzyMove
            fuzzyMove(destination);
            return;
        }
        int myDistance = myLocation.distanceSquaredTo(nearestMuckraker.location);
        for (RobotInfo robot : nearbyAllies) {
            // If there's a closer politician, don't worry about covering this muckraker.
            boolean closerPolitician = myDistance > robot.location.distanceSquaredTo(nearestMuckraker.location);
            if (closerPolitician) {
                // Consider using weightedFuzzyMove
                fuzzyMove(destination);
            }
        }
        fuzzyMove(nearestMuckraker.location);
    }

    /**
     * Returns newly sensable locations relative to the current location
     * after a move in direction lastMove.
     */
    public static int[][] newSensedLocationsRelative(Direction lastMove) throws GameActionException {
        switch (lastMove) {
            case NORTH:
                return NEW_SENSED_LOCS_NORTH;
            case NORTHEAST:
                return NEW_SENSED_LOCS_NORTHEAST;
            case EAST:
                return NEW_SENSED_LOCS_EAST;
            case SOUTHEAST:
                return NEW_SENSED_LOCS_SOUTHEAST;
            case SOUTH:
                return NEW_SENSED_LOCS_SOUTH;
            case SOUTHWEST:
                return NEW_SENSED_LOCS_SOUTHWEST;
            case WEST:
                return NEW_SENSED_LOCS_WEST;
            case NORTHWEST:
                return NEW_SENSED_LOCS_NORTHWEST;
            default:
                return NEW_SENSED_LOCS_CENTER;
        }
    }

    /**
     * Explode if boost for EC is high.
     * @return
     * @throws GameActionException
     */
    public void considerBoostEC() throws GameActionException {
        int distToBase = myLocation.distanceSquaredTo(baseLocation);
        // Be close to base
        if (distToBase <= 2) {
            double multiplier = rc.getEmpowerFactor(allyTeam, 0);
            // Have non-trivial boost
            if (multiplier > 2) {
                int numInRangeUnits = 0;
                for (RobotInfo robot : nearbyRobots) {
                    if (myLocation.distanceSquaredTo(robot.location) <= distToBase) {
                        numInRangeUnits++;
                    }
                }
                // Boost is sizable even after dispersion
                if (multiplier > numInRangeUnits && rc.canEmpower(distToBase)) {
                    rc.empower(distToBase);
                }
            }
        }
    }

    /**
     * Analyzes if politician should attack. Returns true if it attacked. Sorts nearbyRobots and
     * considers various ranges of empowerment to optimize kills. Only kills ECs if parameter
     * passed in.
     */
    public boolean considerAttack(boolean onlyECs, boolean alwaysAttack) throws GameActionException {
        double multiplier = rc.getEmpowerFactor(allyTeam, 0);
        double totalDamage = rc.getConviction() * multiplier - 10;
        if (!rc.isReady() || totalDamage <= 0) {
            return false;
        }
        boolean nearbySlanderer = false;
        double totalAllyConviction = 0;
        for (RobotInfo robot : nearbyAllies) {
            if (robot.type == RobotType.POLITICIAN) {
                totalAllyConviction = robot.conviction * multiplier - 10;
            }
            // //System.out.println\("Robot: " + robot.location);
            // Cannot tell apart slanderers and politicians, use flag
            if (rc.canGetFlag(robot.ID) ) {
                int flagInt = rc.getFlag(robot.ID);
                if (Flag.getSchema(flagInt) == Flag.UNIT_UPDATE_SCHEMA) {
                    UnitUpdateFlag uf = new UnitUpdateFlag(flagInt);
                    nearbySlanderer |= uf.readIsSlanderer();
                }
            }
        }
        totalAllyConviction *= rc.getEmpowerFactor(allyTeam, 0);
        Arrays.sort(nearbyRobots, new Comparator<RobotInfo>() {
            public int compare(RobotInfo r1, RobotInfo r2) {
                // Intentional: Reverse order for this demo
                int d1 = myLocation.distanceSquaredTo(r1.location);
                int d2 = myLocation.distanceSquaredTo(r2.location);
                return d1 - d2;
            }
        });
        int[] distanceSquareds = new int[nearbyRobots.length];
        for (int i = 0; i < nearbyRobots.length; i++) {
            distanceSquareds[i] = myLocation.distanceSquaredTo(nearbyRobots[i].location);
        }
        int optimalNumEnemiesKilled = 0;
        int optimalDist = -1;
        int maxDist = rc.getType().actionRadiusSquared;
        // Loop over subsets of nearbyRobots
        for (int i = 1; i <= nearbyRobots.length; i++) {
            // Skip over subsets which don't actually change range
            if (i != nearbyRobots.length && distanceSquareds[i-1] == distanceSquareds[i]) {
                continue;
            }
            // Remaining units are out of range, break.
            if (distanceSquareds[i-1] >= maxDist) {
                break;
            }
            // Count number of units to kill
            int perUnitDamage = (int)(totalDamage / i);
            int numEnemiesKilled = 0;
            for (int j = 0; j < i; j++) {
                RobotInfo robot = nearbyRobots[j];
                // Consider enemy and neutral units
                if (robot.team != allyTeam && perUnitDamage >= robot.conviction) {
                    if (!onlyECs && robot.type == RobotType.MUCKRAKER) {
                        numEnemiesKilled++;
                    } else if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                        numEnemiesKilled += 10;
                    }
                }
                // If strong nearby politicians, weaken EC so allies can capture.
                else if (robot.team == enemyTeam && robot.type == RobotType.ENLIGHTENMENT_CENTER
                    && totalAllyConviction > robot.conviction + 5) {
                    numEnemiesKilled += 10;
                }
            }
            if (numEnemiesKilled > optimalNumEnemiesKilled) {
                optimalNumEnemiesKilled = numEnemiesKilled;
                optimalDist = distanceSquareds[i-1];
            }
        }
        // //System.out.println\("Explode: " + optimalDist + " " + optimalNumEnemiesKilled + " " + nearbySlanderer);
        if (rc.canEmpower(optimalDist) &&
            (alwaysAttack || optimalNumEnemiesKilled > 1 || (nearbySlanderer && optimalNumEnemiesKilled > 0))) {
            rc.empower(optimalDist);
        }
        return false;
    }
}
