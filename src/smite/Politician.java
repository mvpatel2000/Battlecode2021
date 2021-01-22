package smite;

import battlecode.common.*;
import java.util.*;

public class Politician extends Unit {

    public final static int INITIAL_COOLDOWN = 10;

    boolean onlyECHunter;
    boolean convertedPolitician;

    boolean[] areSlanderers;
    boolean nearbySlanderer;

    // MapTerrainQueue mtq;

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
        // mtq = new MapTerrainQueue(RobotType.POLITICIAN);
        onlyECHunter = rc.getInfluence() > 15;
        convertedPolitician = false;
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // //System.out.println\("1: " + Clock.getBytecodesLeft());
        // Read flags to check for slanderers
        areSlanderers = new boolean[nearbyAllies.length];
        nearbySlanderer = false;
        for (int i = 0; i < nearbyAllies.length; i++) {
            // //System.out.println\("s (" + i + "): " + Clock.getBytecodesLeft());
            RobotInfo robot = nearbyAllies[i];
            if (rc.canGetFlag(robot.ID) ) {
                // //System.out.println\("s2 (" + i + "): " + Clock.getBytecodesLeft());
                int flagInt = rc.getFlag(robot.ID);
                // //System.out.println\("s3 (" + i + "): " + Clock.getBytecodesLeft());
                if (Flag.getSchema(flagInt) == Flag.UNIT_UPDATE_SCHEMA) {
                    // //System.out.println\("s4 (" + i + "): " + Clock.getBytecodesLeft());
                    UnitUpdateFlag uf = new UnitUpdateFlag(flagInt);
                    // //System.out.println\("s5 (" + i + "): " + Clock.getBytecodesLeft());
                    areSlanderers[i] = uf.readIsSlanderer();
                    nearbySlanderer |= areSlanderers[i];
                }
            }
            // //System.out.println\("s (" + i + "): " + Clock.getBytecodesLeft());
        }

        
        // //System.out.println\("2: " + Clock.getBytecodesLeft());
        // Converted politician or slanderer turned politician. Set baseLocation and destination.
        if (baseLocation == null) {
            setInitialDestination();
        }

        // //System.out.println\("3: " + Clock.getBytecodesLeft());
        updateDestinationForExploration(onlyECHunter);
        // //System.out.println\("4: " + Clock.getBytecodesLeft());
        updateDestinationForECHunting();

        // //System.out.println\("5: " + Clock.getBytecodesLeft());
        if (!convertedPolitician) {
            considerBoostEC();
        }
        // //System.out.println\("6: " + Clock.getBytecodesLeft());
        considerAttack(onlyECHunter);
        // //System.out.println\("7: " + Clock.getBytecodesLeft());
        movePolitician();
        // //System.out.println\("8: " + Clock.getBytecodesLeft());
    }

    /**
     * Sets initial base location and destination if converted slanderer or politician
     * @throws GameActionException
     */
    void setInitialDestination() throws GameActionException {
        baseLocation = myLocation;
        convertedPolitician = true;
        // If enemies nearby, take nearest non-muck non-slanderer as destination
        if (nearbyEnemies.length > 0) {
            RobotInfo nearestRobot = null;
            int nearestRobotDistSquared = 1000;
            for (RobotInfo robot : nearbyEnemies) {
                int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
                if (robot.type != RobotType.SLANDERER && robot.type != RobotType.POLITICIAN && 
                    robotDistSquared < nearestRobotDistSquared) {
                    nearestRobot = robot;
                    nearestRobotDistSquared = robotDistSquared;
                }
            }
            if (nearestRobot != null) {
                destination = nearestRobot.location;
            }
        }
        // Was not set in previous phase, eg no nearbyEnemies or they are all slanderers
        if (destination == null) {
            RobotInfo nearestSignalRobot = getNearestEnemyFromAllies();
            // Take nearest smoke signal robot as destination
            if (nearestSignalRobot != null && nearestSignalRobot.type != RobotType.SLANDERER 
                    && nearestSignalRobot.type != RobotType.POLITICIAN) {
                destination = nearestSignalRobot.location;
            }
            // Pick random destination
            else {
                destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
            }
        }
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
        for (int i = 0; i < nearbyAllies.length; i++) {
            RobotInfo robot = nearbyAllies[i];
            // If there's a closer politician, don't worry about covering this muckraker.
            if (robot.type == RobotType.POLITICIAN && !areSlanderers[i]
                    && myDistance > robot.location.distanceSquaredTo(nearestMuckraker.location)) {
                // Consider using weightedFuzzyMove
                fuzzyMove(destination);
            }
        }
        fuzzyMove(nearestMuckraker.location);
    }

    /**
     * Explode if boost for EC is high.
     * @return
     * @throws GameActionException
     */
    public void considerBoostEC() throws GameActionException {
        int distToBase = myLocation.distanceSquaredTo(baseLocation);
        // Be close to base
        if (distToBase <= RobotType.POLITICIAN.actionRadiusSquared) {
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
                if (multiplier > numInRangeUnits*2 && rc.canEmpower(distToBase)) {
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
    public boolean considerAttack(boolean onlyECs) throws GameActionException {
        // Recreate arrays with smaller radius only considering attack
        RobotInfo[] attackNearbyRobots = rc.senseNearbyRobots(RobotType.POLITICIAN.actionRadiusSquared);
        RobotInfo[] attackNearbyAllies = rc.senseNearbyRobots(RobotType.POLITICIAN.actionRadiusSquared, allyTeam);

        if (attackNearbyRobots.length > 8) {
            attackNearbyRobots = rc.senseNearbyRobots(4);
            attackNearbyAllies = rc.senseNearbyRobots(4, allyTeam);
        }

        double multiplier = rc.getEmpowerFactor(allyTeam, 0);
        double totalDamage = rc.getConviction() * multiplier - 10;
        if (!rc.isReady() || totalDamage <= 0) {
            return false;
        }
        // We kill all muckrakers near our base unless its a knife fight and we're side by side
        boolean nearbyBase = myLocation.distanceSquaredTo(baseLocation) < 10;
        double totalAllyConviction = 0;
        for (int i = 0; i < attackNearbyAllies.length; i++) {
            RobotInfo robot = attackNearbyAllies[i];
            boolean isSlanderer = areSlanderers[i];
            if (robot.type == RobotType.POLITICIAN && !isSlanderer) {
                totalAllyConviction = robot.conviction * multiplier - 10;
            }
        }
        // //System.out.println\("Total Ally Conviction: " + totalAllyConviction);
        // //System.out.println\("Attack sort: " + Clock.getBytecodesLeft());
        Arrays.sort(attackNearbyRobots, new Comparator<RobotInfo>() {
            public int compare(RobotInfo r1, RobotInfo r2) {
                return myLocation.distanceSquaredTo(r1.location) - myLocation.distanceSquaredTo(r2.location);
            }
        });
        // //System.out.println\("Attack sort: " + Clock.getBytecodesLeft());
        int[] distanceSquareds = new int[attackNearbyRobots.length];
        for (int i = 0; i < attackNearbyRobots.length; i++) {
            distanceSquareds[i] = myLocation.distanceSquaredTo(attackNearbyRobots[i].location);
        }
        double optimalNumEnemiesKilled = 0;
        int optimalDist = -1;
        int optimalNumUnitsHit = 0;
        int maxDist = rc.getType().actionRadiusSquared;
        // Loop over subsets of attackNearbyRobots
        for (int i = 1; i <= attackNearbyRobots.length; i++) {
            // Skip over subsets which don't actually change range
            if (i != attackNearbyRobots.length && distanceSquareds[i-1] == distanceSquareds[i]) {
                continue;
            }
            // Remaining units are out of range, break.
            if (distanceSquareds[i-1] >= maxDist) {
                break;
            }
            // Count number of units to kill
            int perUnitDamage = (int)(totalDamage / i);
            double numEnemiesKilled = 0;
            for (int j = 0; j < i; j++) {
                RobotInfo robot = attackNearbyRobots[j];
                // Consider enemy and neutral units
                if (robot.team != allyTeam && perUnitDamage > robot.conviction) {
                    // 1 point for muckraker
                    if ((!onlyECs || nearbySlanderer) && robot.type == RobotType.MUCKRAKER) {
                        // //System.out.println\("Can kill MK: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        numEnemiesKilled++;
                    } 
                    // 10 points for enlightenment center
                    else if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                        // //System.out.println\("Can kill EC: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        numEnemiesKilled += 10;
                    } 
                    // points for politicians that return net positive influence
                    else if (robot.type == RobotType.POLITICIAN && multiplier > 2) {
                        // //System.out.println\("Can kill PN: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        numEnemiesKilled += multiplier * Math.min(robot.influence, perUnitDamage - robot.conviction) / rc.getConviction();
                    }
                }
                // If strong nearby politicians, weaken EC so allies can capture.
                else if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    if (robot.team != allyTeam && totalAllyConviction > robot.conviction + 5) {
                        // //System.out.println\("Weaken EC attack! Confirmed.");
                        numEnemiesKilled += 10;
                    }
                }
            }
            if (numEnemiesKilled > optimalNumEnemiesKilled) {
                optimalNumEnemiesKilled = numEnemiesKilled;
                optimalDist = distanceSquareds[i-1];
                optimalNumUnitsHit = i;
            }
        }
        // //System.out.println\("Explode: " + optimalDist + " " + optimalNumEnemiesKilled + " " + nearbySlanderer);

        // 1. Can empower at optimalDist
        // 2. Either there are enemies you are hitting or you are only hitting one unit (so
        //    ECHunters don't waste on allied units) or adjacent to target
        // 3. Either force attack or kill multiple enemies or kill 1 enemy but close to base or slanderers nearby or end of game
        if (rc.canEmpower(optimalDist) &&
            (nearbyEnemies.length > 0 || optimalNumUnitsHit == 1 || myLocation.distanceSquaredTo(destination) <= 2) &&
            (optimalNumEnemiesKilled > 1 || ((nearbySlanderer || nearbyBase || currentRound > 1450) && optimalNumEnemiesKilled > 0))) {
            rc.empower(optimalDist);
        }
        return false;
    }
}
