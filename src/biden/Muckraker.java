package biden;

import battlecode.common.*;
import java.util.*;

public class Muckraker extends Unit {

    public final static int INITIAL_COOLDOWN = 10;

    Direction momentumDir;
    double destWeight;
    double spreadWeight;
    double momentumWeight;
    double passabilityWeight;
    double noMovePenalty;
    final double initialNoMovePenalty = 1.1;
    final double noMovePenaltyMultiplier = 1.5;
    MapLocation previousDestination;
    int enemiesAtPrevDest;

    public Muckraker(RobotController rc) throws GameActionException {
        super(rc);

        noMovePenalty = initialNoMovePenalty;
        setInitialMomentum();
    }

    @Override
    public void runUnit() throws GameActionException {
        super.runUnit();
        previousDestination = baseLocation;
        enemiesAtPrevDest = 0;
        // System.out.println("Explore Mode: " + exploreMode);
        setMoveWeights();

        updateDestinationForExploration(false);
        // Search for nearest slanderer. If one exists, kill it or move towards it.
        if (rc.isReady()) {
            if (!denyNeutralEC()) {
                unClog();
                huntSlanderersOrToDestination();
            }
        }
        rc.setIndicatorLine(myLocation, destination, 255, 0, 0);
    }


    boolean unClog() throws GameActionException {
        if (rc.canDetectLocation(destination.add(myLocation.directionTo(destination)))
        && rc.canDetectLocation(destination.add(myLocation.directionTo(destination).rotateLeft()))
        && rc.canDetectLocation(destination.add(myLocation.directionTo(destination).rotateRight()))) {
            RobotInfo destRobot = rc.senseRobotAtLocation(destination);
            if (destRobot != null && destRobot.team == enemyTeam && destRobot.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (myLocation.distanceSquaredTo(destination) <= 2) {
                    return false;
                }
                if (   (rc.isLocationOccupied(destination.add(Direction.NORTH)) || !rc.onTheMap(destination.add(Direction.NORTH)))
                    && (rc.isLocationOccupied(destination.add(Direction.NORTHEAST)) || !rc.onTheMap(destination.add(Direction.NORTHEAST)))
                    && (rc.isLocationOccupied(destination.add(Direction.EAST)) || !rc.onTheMap(destination.add(Direction.EAST)))
                    && (rc.isLocationOccupied(destination.add(Direction.SOUTHEAST)) || !rc.onTheMap(destination.add(Direction.SOUTHEAST)))
                    && (rc.isLocationOccupied(destination.add(Direction.SOUTH)) || !rc.onTheMap(destination.add(Direction.SOUTH)))
                    && (rc.isLocationOccupied(destination.add(Direction.SOUTHWEST)) || !rc.onTheMap(destination.add(Direction.SOUTHWEST)))
                    && (rc.isLocationOccupied(destination.add(Direction.WEST)) || !rc.onTheMap(destination.add(Direction.WEST)))
                    && (rc.isLocationOccupied(destination.add(Direction.NORTHWEST)) || !rc.onTheMap(destination.add(Direction.NORTHWEST)))
                    ) {
                        destination = new MapLocation(baseLocation.x + (int)(Math.random()*80 - 40), baseLocation.y + (int)(Math.random()*80 - 40));
                        return true;
                    }
            }
        }
        return false;
    }
    /**
     * Dilute enemy politicians when they try to capture neutral EC.
     * Returns true if it took an action.
     * @throws GameActionException
     */
    boolean denyNeutralEC() throws GameActionException {
        if (currentRound < 100) {
            return false;
        }

        RobotInfo[] nearbyNeutrals = rc.senseNearbyRobots(RobotType.MUCKRAKER.sensorRadiusSquared, neutralTeam);
        // No nearby neutral units
        if (nearbyNeutrals.length == 0) {
            return false;
        }
        RobotInfo nearestNeutralEC = null;
        int nearestNeutralECDistance = 100000;
        for (RobotInfo robot : nearbyNeutrals) {
            int distance = robot.location.distanceSquaredTo(myLocation);
            if (distance < nearestNeutralECDistance) {
                nearestNeutralECDistance = distance;
                nearestNeutralEC = robot;
            }
        }
        // No nearby neutral ECs
        if (nearestNeutralEC == null) {
            return false;
        }
        for (RobotInfo robot : nearbyAllies) {
            // Another muckraker has EC covered
            if (robot.type == RobotType.MUCKRAKER && robot.location.distanceSquaredTo(nearestNeutralEC.location) <= 2) {
                return false;
            }
            // Ally policitian attempts to take EC
            if (robot.type == RobotType.POLITICIAN && robot.location.distanceSquaredTo(nearestNeutralEC.location) <= 2) {
                return false;
            }
        }
        // Nearest enemy politician
        RobotInfo nearestEnemyPolitician = null;
        int nearestEnemyPoliticianDistance = 100000;
        for (RobotInfo robot : nearbyEnemies) {
            int distance = robot.location.distanceSquaredTo(nearestNeutralEC.location);
            if (robot.type == RobotType.POLITICIAN && distance < nearestEnemyPoliticianDistance) {
                nearestEnemyPoliticianDistance = distance;
                nearestEnemyPolitician = robot;
            }
        }
        // No enemy politicians
        if (nearestEnemyPolitician == null) {
            // Don't move, already adjacent
            if (nearestNeutralECDistance > 2) {
                fuzzyMove(nearestNeutralEC.location);
            }
        }
        else {
            // Redefine as unchanging variable for compiler
            MapLocation nearestEnemyPoliticianLocation = nearestEnemyPolitician.location;
            int nearestEnemyPoliticianDistanceFinal = nearestEnemyPoliticianDistance;
            MapLocation nearestNeutralECLocation = nearestNeutralEC.location;
            Arrays.sort(allDirections, new Comparator<Direction>() {
                public int compare(Direction d1, Direction d2) {
                    MapLocation m1 = myLocation.add(d1);
                    MapLocation m2 = myLocation.add(d2);
                    int cost1 = m1.distanceSquaredTo(nearestEnemyPoliticianLocation) <= nearestEnemyPoliticianDistanceFinal ? 0 : 100000;
                    int cost2 = m2.distanceSquaredTo(nearestEnemyPoliticianLocation) <= nearestEnemyPoliticianDistanceFinal ? 0 : 100000;
                    cost1 += m1.distanceSquaredTo(nearestNeutralECLocation);
                    cost2 += m2.distanceSquaredTo(nearestNeutralECLocation);
                    return cost1 - cost2;
                }
            });
            for (Direction dir : allDirections) {
                // Stay put
                if (dir == Direction.CENTER) {
                    break;
                }
                else if (rc.canMove(dir)) {
                    // System.out.println("Took deny neutral EC move: " + dir);
                    move(dir);
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Search for nearest slanderer. If one exists, kill it or move towards it.
     * @throws GameActionException
     */
    void huntSlanderersOrToDestination() throws GameActionException {
        int attackRadius = rc.getType().actionRadiusSquared;
        RobotInfo nearestSlanderer = null;
        int nearestSlandererDistSquared = 100;
        RobotInfo biggestSlanderer = null;
        int size = -1;
        for (RobotInfo robot : nearbyEnemies) {
            int robotDistSquared = myLocation.distanceSquaredTo(robot.location);
            if (robot.type == RobotType.SLANDERER) {
                if (robotDistSquared < nearestSlandererDistSquared) {
                    nearestSlanderer = robot;
                    nearestSlandererDistSquared = robotDistSquared;
                }
                if (robot.conviction > size && robotDistSquared < attackRadius) {
                    biggestSlanderer = robot;
                    size = robot.conviction;
                }
            }
        }
        // Kill biggest you can
        if (biggestSlanderer != null && rc.canExpose(biggestSlanderer.location)) {
            rc.expose(biggestSlanderer.location);
        }
        // Move towards nearest
        else if (nearestSlanderer != null) {
            fuzzyMove(nearestSlanderer.location);
        }
        else {
            // Continue towards destination
            // Consider using weightedFuzzyMove
            muckrakerMove();
        }
    }

    void setInitialMomentum() {
        if (rc.getRoundNum() < 50 || destination == null) { // no momentum in early game
            momentumDir = Direction.CENTER;
            return;
        }
        double rng = Math.random();
        if (rng < 0.33) {
            momentumDir = myLocation.directionTo(destination).rotateLeft().rotateLeft();
            momentumWeight = Math.random() * 100;
        } else if (rng < 0.67) {
            momentumDir = myLocation.directionTo(destination).rotateRight().rotateRight();
            momentumWeight = Math.random() * 100;
        } else {
            momentumDir = Direction.CENTER;
        }
    }

    void setMoveWeights() {
        momentumWeight *= 0.97;
        destWeight = 1;
        passabilityWeight = 1;
        spreadWeight = Math.pow(rc.getRoundNum() + (exploreMode ? 500 : 100), 0.4);
    }

    void muckrakerMove() throws GameActionException {
        double[] scores = new double[9]; // we will pick the lowest-score tile
        boolean[] canMove = new boolean[9];

        for (int i = 0; i < 9; i++) {
            scores[i] = 0;
            Direction di = allDirections[i];
            MapLocation targetLoc = myLocation.add(di);
            // track if impossible to move
            if (!rc.canMove(di) && di != Direction.CENTER) {
                canMove[i] = false;
                continue;
            }
            canMove[i] = true;

            // R^2 component
            if (destination != null)
                scores[i] += destWeight * targetLoc.distanceSquaredTo(destination);

            // momentum component
            if (momentumDir == Direction.CENTER || di == momentumDir) {
                scores[i] += 0;
            } else if (di == momentumDir.rotateLeft() || di == momentumDir.rotateRight()) {
                scores[i] += momentumWeight;
            } else if (di == momentumDir.rotateLeft().rotateLeft() || di == momentumDir.rotateRight().rotateRight()) {
                scores[i] += 2 * momentumWeight;
            } else if (di == momentumDir.rotateLeft().rotateLeft().rotateLeft() || di == momentumDir.rotateRight().rotateRight().rotateRight()) {
                scores[i] += 3 * momentumWeight;
            } else if (di == momentumDir.opposite()) {
                scores[i] += 4 * momentumWeight;
            }

            // passability component
            scores[i] += passabilityWeight / rc.sensePassability(targetLoc);
        }

        // spread component
        // we do this separately to minimize iterating over nearby allies
        for (RobotInfo r : nearbyAllies) {
            if (r.type == RobotType.MUCKRAKER) {
                for (int i = 0; i < 9; i++) {
                    scores[i] += spreadWeight / myLocation.add(allDirections[i]).distanceSquaredTo(r.location);
                }
            }
        }
        scores[8] *= noMovePenalty; // multiplier penalty for not moving
        double bestScore = scores[8];
        Direction moveDir = Direction.CENTER;
        for (int i = 0; i < 8; i++) {
            if (canMove[i] && scores[i] < bestScore) {
                bestScore = scores[i];
                moveDir = allDirections[i];
            }
        }
        // System.out.println("Weights:\ndestWeight: "+destWeight+"\nspreadWeight: "+spreadWeight+"\nmomentumWeight: "+momentumWeight+"\npassabilityWeight: "+passabilityWeight);
        // System.out.println("Scores:\nNORTH: "+scores[0]+"\nNORTHEAST: "+scores[1]+"\nEAST: "+scores[2]+"\nSOUTHEAST: "+scores[3]+"\nSOUTH: "+scores[4]+"\nSOUTHWEST: "+scores[5]+"\nWEST: "+scores[6]+"\nNORTHWEST: "+scores[7]+"\nCENTER: "+scores[8]);
        if (moveDir == Direction.CENTER) {
            noMovePenalty *= noMovePenaltyMultiplier; // penalty for not moving increases if you don't move
        } else {
            noMovePenalty = initialNoMovePenalty;
        }
        if (moveDir != Direction.CENTER)
            move(moveDir);
    }

    double scaleVector(int dx, int dy, int cap) {
        int maxdir = Math.max(dx, dy);
        return ((double)cap)/(double)maxdir;
    }

    double scaleOnTheMap(int dx, int dy) {
        for (double i=0.2; i<10; i+=0.5) {
            double resultingX = myLocation.x + i*dx;
            double resultingY = myLocation.y + i*dy;
            if ((edgeLocations[0] != -1 && edgeLocations[0] < resultingY)
                 || (edgeLocations[1] != -1 && edgeLocations[1] < resultingX)
                 || (edgeLocations[2] != -1 && edgeLocations[2] > resultingY)
                 || (edgeLocations[3] != -1 && edgeLocations[3] > resultingX)) {
                     return i;
                 }
        }
        return 1;
    }

    double angleBetween(int dx1, int dy1, int dx2, int dy2) {
        double ang = ((double)(dx1*dx2 + dy1*dy2))/(Math.sqrt(dx1*dx1 + dy1*dy1)*Math.sqrt(dx2*dx2 + dy2*dy2));
        return ang;
    }

    MapLocation nearDestination(MapLocation trueDestination) throws GameActionException {
        MapLocation nearDestination = myLocation;
        for (int i = 0; i < 3; i++) {
            nearDestination = nearDestination.add(nearDestination.directionTo(trueDestination));
        }
        rc.setIndicatorDot(nearDestination, 0, 0, 255);
        return nearDestination;
    }

    void muckrackerRerouteDestination() throws GameActionException {
        MapLocation currDestination = destination;
        int numEnemies = nearbyEnemies.length;
        int enemyDiff = numEnemies - enemiesAtPrevDest;
        int deltaX = destination.x - previousDestination.x;
        int deltaY = destination.y - previousDestination.y;
        double mult = scaleVector(deltaX, deltaY, 48);

        System.out.println("Prev Dest: " + previousDestination);
        System.out.println("Curr Dest: " + destination);

        deltaX = (int)((double)(deltaX) * mult);
        deltaY = (int)((double)(deltaY) * mult);
        int tempDeltaX = 0;
        int tempDeltaY = 0;
        double maxDistance = 0.0;
        double rotate45 = Math.sqrt(2)/2.0;
        double scale = 1.0;
        for (int i=0; i<8; i++) {
            //System.out.println("Rotating 45 degrees. New Dest: " + destination);
            tempDeltaX = deltaX;
            tempDeltaY = deltaY;
            deltaX = (int)(tempDeltaX*rotate45 - tempDeltaY*rotate45);
            deltaY = (int)(tempDeltaX*rotate45 + tempDeltaY*rotate45);
            scale = scaleOnTheMap(deltaX, deltaY);
            MapLocation newDestination = myLocation.translate((int)(scale*deltaX), (int)(scale*deltaY));
            //System.out.println("Potential Destination: " + (int)(scale*deltaX) + " " + (int)(scale*deltaY));
            if (rc.canSenseLocation(newDestination) || !rc.onTheMap(nearDestination(newDestination))) {
                continue;
            }
            double distanceFrom = newDestination.distanceSquaredTo(baseLocation) + 0.5*newDestination.distanceSquaredTo(previousDestination);
            //System.out.println("Est. Distance from base: " + distanceFrom);
            if (distanceFrom > maxDistance) {
                destination = newDestination;
                maxDistance = distanceFrom;
            }
        }
        //double i = scaleOnTheMap(deltaX, deltaY);
        //System.out.println("Scale: " + i);
        //System.out.println("Best 45 shot is: " + destination);
        //destination = myLocation.translate((int)(i*bestDeltaX), (int)(i*bestDeltaY));
        System.out.println("Best dest is: " + destination);

        exploreMode = true;
        previousDestination = currDestination;
        enemiesAtPrevDest = numEnemies;
        return;
    }
}
