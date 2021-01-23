package taft;

import battlecode.common.*;
import java.util.*;

public class Politician extends Unit {

    public final static int INITIAL_COOLDOWN = 10;

    boolean onlyECHunter;
    boolean convertedPolitician;

    // Variables to keep track of defense lap prior to attack for defend-attackers
    boolean defend; // if true, take a lap around base. if false, head for destination.
    MapLocation farthestSlandererFromBase;
    boolean lapClockwise; // direction of lap around base for defend-attackers
    boolean leftStartingOctant; // true if I've started my lap and left the starting octant
    boolean startedLap; // true if I've moved far enough from my EC to start my lap
    Direction lapStartDirection; // direction from baseLocation to me at the start of lap

    boolean[] areSlanderers;
    boolean nearbySlanderer;

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
        onlyECHunter = rc.getInfluence() > 15;
        convertedPolitician = false;
        defend = true;
        lapClockwise = Math.random() < 0.5; // send half the defenders clockwise, other half counterclockwise
        leftStartingOctant = false;
        startedLap = false;
        lapStartDirection = null;
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();

        // System.out.println("1: " + Clock.getBytecodesLeft());
        // Read flags to check for slanderers
        areSlanderers = new boolean[nearbyAllies.length];
        nearbySlanderer = false;
        farthestSlandererFromBase = null;
        int maxDist = 0;
        for (int i = 0; i < nearbyAllies.length; i++) {
            // System.out.println("s (" + i + "): " + Clock.getBytecodesLeft());
            RobotInfo robot = nearbyAllies[i];
            if (rc.canGetFlag(robot.ID)) {
                // System.out.println("s2 (" + i + "): " + Clock.getBytecodesLeft());
                int flagInt = rc.getFlag(robot.ID);
                // System.out.println("s3 (" + i + "): " + Clock.getBytecodesLeft());
                if (Flag.getSchema(flagInt) == Flag.UNIT_UPDATE_SCHEMA) {
                    // System.out.println("s4 (" + i + "): " + Clock.getBytecodesLeft());
                    UnitUpdateFlag uf = new UnitUpdateFlag(flagInt);
                    // System.out.println("s5 (" + i + "): " + Clock.getBytecodesLeft());
                    areSlanderers[i] = uf.readIsSlanderer();
                    nearbySlanderer |= areSlanderers[i];
                    if (baseLocation != null) {
                        int dist = baseLocation.distanceSquaredTo(robot.location);
                        if (areSlanderers[i] && (farthestSlandererFromBase == null || dist > maxDist)) {
                            farthestSlandererFromBase = robot.location;
                            dist = maxDist;
                        }
                    }
                }
            }
            // System.out.println("s (" + i + "): " + Clock.getBytecodesLeft());
        }
        
        // System.out.println("2: " + Clock.getBytecodesLeft());
        // Converted politician or slanderer turned politician. Set baseLocation and destination.
        if (baseLocation == null) {
            setInitialDestination();
        }

        if (instruction == SpawnDestinationFlag.INSTR_ATTACK) {
            defend = false;
        }

        if (!defend) {
            rc.setIndicatorDot(myLocation, 255, 0, 0);
            // System.out.println("3: " + Clock.getBytecodesLeft());
            updateDestinationForExploration(onlyECHunter);
            // System.out.println("4: " + Clock.getBytecodesLeft());
            updateDestinationForECHunting();
    
            // System.out.println("6: " + Clock.getBytecodesLeft());
            considerAttack(onlyECHunter, false);
        } else {
            rc.setIndicatorDot(myLocation, 0, 0, 255);
            considerAttack(false, true);
        }
        // System.out.println("7: " + Clock.getBytecodesLeft());
        movePolitician();
        // System.out.println("8: " + Clock.getBytecodesLeft());
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
     * ECHunters bee-line towards enemy ECs. Defenders move like a gas around their destination.
     * Normal politicians weighted move and seek out nearby
     * muckrakers if they're uncovered.
     */
    void movePolitician() throws GameActionException {
        // ECHunters ignore other units
        if (defend) {
            if (baseLocation == null) { // this shouldn't happen; null-check just to be safe and end defense lap
                defend = false;
                movePolitician();
                return;
            }
            // if no slanderers are seen or if I'm farther away from the base than the farthest slanderer, then proceed in lap
            else if (farthestSlandererFromBase == null || myLocation.distanceSquaredTo(baseLocation) > farthestSlandererFromBase.distanceSquaredTo(baseLocation)) {
                if (!startedLap) { // start lap if it hasn't been started yet
                    lapStartDirection = baseLocation.directionTo(myLocation);
                    startedLap = true;
                } else if (startedLap && !leftStartingOctant) { // if we were still in the starting octant before, check if we still are now
                    leftStartingOctant = baseLocation.directionTo(myLocation) != lapStartDirection;
                } else if (startedLap && leftStartingOctant && baseLocation.directionTo(myLocation) == lapStartDirection) { // if we did a full lap, end defense lap
                    defend = false;
                    movePolitician();
                    return;
                }
                Direction moveDir;
                MapLocation pivot = farthestSlandererFromBase == null ? baseLocation : farthestSlandererFromBase;
                if (lapClockwise) {
                    moveDir = myLocation.directionTo(pivot).rotateLeft();
                } else {
                    moveDir = myLocation.directionTo(pivot).rotateRight();
                }
                if (!rc.onTheMap(myLocation.add(moveDir))) { // hit the wall, end defense lap
                    defend = false;
                    movePolitician();
                    return;
                }
                fuzzyMove(myLocation.add(moveDir)); // move orthogonal to direction to pivot, specified by moveDir
            } else { // I need to get farther from the base; get on the far side of the farthest slanderer
                fuzzyMove(farthestSlandererFromBase.add(farthestSlandererFromBase.directionTo(baseLocation).opposite()));
            }
            return;
        } else if (onlyECHunter) {
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
     * Analyzes if politician should attack. Returns true if it attacked. Sorts nearbyRobots and
     * considers various ranges of empowerment to optimize kills. Only kills ECs if parameter
     * passed in. If paranoid is true then blow up on an enemy muckraker whenever possible.
     */
    public boolean considerAttack(boolean onlyECs, boolean paranoid) throws GameActionException {
        // Recreate arrays with smaller radius only considering attack
        RobotInfo[] attackNearbyRobots = rc.senseNearbyRobots(RobotType.POLITICIAN.actionRadiusSquared);

        if (attackNearbyRobots.length > 8) {
            attackNearbyRobots = rc.senseNearbyRobots(4);
        }

        double multiplier = rc.getEmpowerFactor(allyTeam, 0);
        double totalDamage = (rc.getConviction() - 10) * multiplier;
        if (!rc.isReady() || totalDamage <= 0) {
            return false;
        }
        // We kill all muckrakers near our base unless its a knife fight and we're side by side
        boolean nearbyBase = myLocation.distanceSquaredTo(baseLocation) < 10;
        if (destination != null) {
            nearbyBase = myLocation.distanceSquaredTo(baseLocation) < myLocation.distanceSquaredTo(destination);
        }
        double totalAllyConviction = 0;
        int allyLength = Math.min(8, nearbyAllies.length);
        for (int i = 0; i < allyLength; i++) {
            RobotInfo robot = nearbyAllies[i];
            if (robot.type == RobotType.POLITICIAN && !areSlanderers[i]) {
                totalAllyConviction += (robot.conviction - 10) * multiplier;
            }
        }
        // System.out.println("Total Ally Conviction: " + totalAllyConviction);
        // System.out.println("Attack sort: " + Clock.getBytecodesLeft());
        Arrays.sort(attackNearbyRobots, new Comparator<RobotInfo>() {
            public int compare(RobotInfo r1, RobotInfo r2) {
                return myLocation.distanceSquaredTo(r1.location) - myLocation.distanceSquaredTo(r2.location);
            }
        });
        // System.out.println("Attack sort: " + Clock.getBytecodesLeft());
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
                        // System.out.println("Can kill MK: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        numEnemiesKilled++;
                    } 
                    // 10 points for enlightenment center
                    else if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                        // System.out.println("Can kill EC: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        numEnemiesKilled += 10;
                    } 
                    // points for politicians that return net positive influence
                    else if (robot.type == RobotType.POLITICIAN && multiplier > 2) {
                        // System.out.println("Can kill PN: " + i + " " + j + " " + robot.location + " " + perUnitDamage + " " + robot.influence + " " + robot.conviction);
                        double conversionScore = multiplier * (Math.min(robot.influence, perUnitDamage - robot.conviction) - 10) / rc.getConviction();
                        if (conversionScore > 0) {
                            numEnemiesKilled += conversionScore;
                        }
                    }
                }
                // If strong nearby politicians, weaken EC so allies can capture.
                else if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    if (robot.team != allyTeam && totalAllyConviction > robot.conviction + 5) {
                        // System.out.println("Weaken EC attack! Confirmed.");
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
        // System.out.println("Explode: " + optimalDist + " " + optimalNumEnemiesKilled + " " + nearbySlanderer);

        // 1. Can empower at optimalDist
        // 2. Either there are enemies you are hitting or you are only hitting one unit (so
        //    ECHunters don't waste on allied units) or adjacent to target
        // 3. Either force attack or kill multiple enemies or kill 1 enemy but close to base or slanderers nearby or end of game
        if (rc.canEmpower(optimalDist) &&
            (nearbyEnemies.length > 0 || optimalNumUnitsHit == 1 || myLocation.distanceSquaredTo(destination) <= 2) &&
            (optimalNumEnemiesKilled > 1 || ((nearbySlanderer || nearbyBase || paranoid || currentRound > 1450) && optimalNumEnemiesKilled > 0))) {
            rc.empower(optimalDist);
        }
        return false;
    }
}
