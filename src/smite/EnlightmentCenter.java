package smite;

import battlecode.common.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

class Destination {
    MapLocation destLoc;
    boolean isGuess;

    Destination(MapLocation thisLocation, boolean thisGuess) {
        destLoc = thisLocation;
        isGuess = thisGuess;
    }

    boolean getGuess() {
        return isGuess;
    }

    MapLocation getDestinationLocation() {
        return destLoc;
    }
}

public class EnlightmentCenter extends Robot {
    final static int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4},{1,5},{5,1},{5,-1},{1,-5},{-1,-5},{-5,-1},{-5,1},{-1,5},{2,5},{5,2},{5,-2},{2,-5},{-2,-5},{-5,-2},{-5,2},{-2,5},{4,4},{4,-4},{-4,-4},{-4,4},{3,5},{5,3},{5,-3},{3,-5},{-3,-5},{-5,-3},{-5,3},{-3,5},{0,6},{6,0},{0,-6},{-6,0},{1,6},{6,1},{6,-1},{1,-6},{-1,-6},{-6,-1},{-6,1},{-1,6},{2,6},{6,2},{6,-2},{2,-6},{-2,-6},{-6,-2},{-6,2},{-2,6}};
    final static int[] SLANDERER_INFLUENCE_THRESHOLDS = new int[]{21, 41, 63, 85, 107, 130, 154, 178, 203, 228, 255, 282, 310, 339, 368, 399, 431, 463, 497, 532, 568, 605, 643, 683, 724, 766, 810, 855, 902, 949};
    // Symmetries - horizontal, vertical, rotational, true until ruled out.
    boolean[] symmetries;

    // Initial EC to EC communication
    boolean scannedAllIDs;
    int searchRound;
    FindAllyFlag initialFaf;    // initial flag used to communicate my existence and location
    int[] searchBounds;
    ArrayList<Integer> firstRoundIDsToConsider;
    // Generate Secret Code. Change these two numbers before uploading to competition
    final int CRYPTO_KEY = 92747507; // A random large number
    final int MODULUS = 997; // A random number strictly smaller than CRYPTO KEY and 2^10 = 1024

    // Ally ECs
    // We are treating unverified allies as ground truth for now.
    // For better security, eventually implement and use the verified variables
    int numAllyECs;
    int[] allyECIDs;
    MapLocation[] allyECLocs; // absolute locations
    int[] allyDistances;

    // Environment and enemy; maps from MapLocations to influences. If influence is unknown it is null.
    Map<MapLocation, Integer> enemyECLocsToInfluence;
    Map<MapLocation, Integer> neutralECLocsToInfluence;
    Map<MapLocation, Integer> capturedAllyECLocsToInfluence;    // for an ally captured after us, our robots only communicate
                                                                // the new ally's location, not the ID. So, we cannot add that ally
                                                                // to the allyECID/location arrays above, we must maintain this separate map.
    Map<MapLocation, Integer> sentRobotsToNeutralECs;
    MapLocation enemySlanderer;
    int enemySlandererRound;
    int numUUFprocessed;
    // Flags to initialize whenever a unit is spawned, and then set
    // at the earliest available flag slot.
    SpawnUnitFlag latestSpawnFlag;
    LocationFlag latestSpawnDestinationFlag;
    int latestSpawnRound;
    boolean spawnDestIsGuess;   // whether the destination we send units to is a guess (true) or exact (false)

    // Troop Counts
    int numSlanderers;
    double approxNumSlanderers;
    int numMuckrakers;
    int numPoliticians;
    int numScouts; // scouts don't count in troop counts

    // Build order
    int initialBuildStep;
    boolean openingBuild;
    int mediumSizedPolitician;
    // Mid-game EC variables.
    boolean isMidGame;
    Map<Integer, Destination> basesToDestinations;  // <BaseID, DestinationForRobots>
    Map<Integer, Integer> pendingBaseLocations;  // <RobotID, BaseID>
    Set<Integer> trackedRobots;
    Set<Integer> trackedBases;

    // UnitTrackers
    int[] trackingList;
    int MAX_UNITS_TRACKED = 400;
    int numUnitsTracked;

    // Bidding information
    int currentBid;
    boolean descendingBid;
    int previousTeamVotes;

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
        enemyECLocsToInfluence = new HashMap<MapLocation, Integer>();
        neutralECLocsToInfluence = new HashMap<MapLocation, Integer>();
        capturedAllyECLocsToInfluence = new HashMap<MapLocation, Integer>();
        sentRobotsToNeutralECs = new HashMap<MapLocation, Integer>();
        enemySlanderer = null;    // a location to send muckrackers where we have previously seen an enemy slanderer.
        enemySlandererRound = 0;
        latestSpawnRound = -1;
        spawnDestIsGuess = true;
        // Troop counts
        numSlanderers = 0;
        approxNumSlanderers = 0.0;
        numMuckrakers = 0;
        numPoliticians = 0;
        numScouts = 0;
        // Build orders
        initialBuildStep = 0;
        mediumSizedPolitician = 300 + (int)(200.0*Math.random());
        // Everyone spawned after round 1 is a mid-game EC.
        if (currentRound > 1) {
            MAX_UNITS_TRACKED = 60;
            isMidGame = true;
            // //System.out.println\("I am a mid-game EC!");
            basesToDestinations = new HashMap<Integer, Destination>();
            pendingBaseLocations = new HashMap<Integer, Integer>();
            trackedRobots = new HashSet<Integer>(); // set for quick access to see if a robot is already in our UnitTracker.
            trackedBases = new HashSet<Integer>();  // essentially gets built up so it just becomes a set version of allyECIDs.
        }

        // Tracking
        trackingList = new int[MAX_UNITS_TRACKED];
        numUnitsTracked = 0;

        currentBid = 1;
        descendingBid = false;
        previousTeamVotes = 0;

        openingBuild = true;
    }

    @Override
    public void run() throws GameActionException {
        super.run();

        if (currentRound == 100) {
            // //rc.resign\(); // TODO: remove; just for debugging
        }

        spawnDestIsGuess = true;
        numUUFprocessed = 0;
        considerBid();

        // Do not add any code in the run() function before this line.
        // initialFlagsAndAllies must run here to fit properly with bytecode.
        // This function will return with ~2700 bytecode left for rounds 1 - 3.
        // TODO: Potentially improve initialization scheme for neutral ECs we take over.
        // //System.out.println\("Bytecodes used before midgame check: " + Clock.getBytecodeNum());
        if (!isMidGame) {
            if (currentRound < searchBounds.length) {
                initialFlagsAndAllies();
            }
            if (turnCount == searchBounds.length) {
                putVisionTilesOnMap();
                updateSymmetryFromAllies();
                allyDistances = furthestAllyDistances(); // only calculate this once after initial allies set.
            }
            if (turnCount >= searchBounds.length) {
                readAllyECUpdates(); // read EC updates before building units/prod logic.
                setSpawnOrDirectionFlag(); // this needs to be run before spawning any unit
                // spawnOrUpdateScout();
            }
        }
        if (isMidGame) {
            // We do not send out initial flags or use allies to calculate symmetry for mid-game ECs.
            // Instead, we add new nearby robots to our UnitTracker every round and listen to them
            // for special flags.
            if (turnCount == 1) {
                putVisionTilesOnMap();
            }
            // //System.out.println\("Bytecodes used before tracking new robots: " + Clock.getBytecodeNum());
            trackNewNearbyRobots();
            // //System.out.println\("Bytecodes used before reading ec updates: " + Clock.getBytecodeNum());
            readAllyECUpdates(); // read EC updates before building units/prod logic.
            // //System.out.println\("Bytecodes used before setting spawn flag: " + Clock.getBytecodeNum());
            setSpawnOrDirectionFlag(); // this needs to be run before spawning any unit
        }
        // Be careful about bytecode usage on rounds < searchBounds.length, especially round 1.
        // We currently end round 1 with 10 bytecode left. Rounds 2 and 3, ~2000 left.
        ////System.out.println\("I am tracking " + unitTrackerList.length + " units");
        // //System.out.println\("Bytecodes used before UnitTrackers: " + Clock.getBytecodeNum());
        updateUnitTrackers();
        // //System.out.println\("Bytecodes used before buildUnit: " + Clock.getBytecodeNum());
        buildUnit();
        // //System.out.println\("Bytecodes used end: " + Clock.getBytecodeNum());


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
        // We have more than half the votes, stop bidding. Also don't bid for first 100 turns of
        // midgame EC unless game is about to end.
        if (currentVotes > 750 || isMidGame && turnCount < 100 && currentRound < 1490) {
            currentBid = 0;
            return;
        }

        if (currentBid == 0) currentBid = 1;

        int canAffordToLose = Math.max(749 - rc.getRoundNum() + rc.getTeamVotes(), 0);
        double maxOfferFactor = 1 + ((1500 - rc.getRoundNum()) / 50) + (canAffordToLose / 20);
        int maxWillingToBid = (int) (rc.getInfluence() / maxOfferFactor);

        // //System.out.println\("Conds: " + (rc.getTeamVotes() > previousTeamVotes) + " " + descendingBid));
        if (rc.getTeamVotes() > previousTeamVotes) { // we won the last bid
            previousTeamVotes++;
            if (descendingBid) {
                currentBid = (int) (0.9*currentBid);
                descendingBid = true;
            }
        } else { // we lost the last bid
            if (descendingBid) {
                currentBid = (int) ((1 + currentBid)/0.9);
                descendingBid = false;
            } else {
                currentBid = Math.min((int) (1.5 * currentBid), maxWillingToBid);
            }
        }
        // Always bid at least 1
        currentBid = Math.max(currentBid, 2);
        // Don't bid more than 1 in first 100 turns
        if (currentRound < 100) {
            currentBid = Math.min(currentBid, 1);
        } if (currentRound < 20) { // don't bid more than 0 first 20 turns for build order
            currentBid = Math.min(currentBid, 0);
        }
        if (rc.canBid(currentBid)) {
            rc.bid(currentBid);
        }
    }

    /**
     * Wrapper function for spawnRobot. Determines build order. Spawns an initial silent slanderer
     * and subsequently builds a mix of all units based on ratios. Destination is based on the
     * nearest enemy EC or a random exploration destination.
     * @throws GameActionException
     */
    void buildUnit() throws GameActionException {
        // Not ready to build anything
        if (!rc.isReady() || rc.getRoundNum() >= 1490) {
            return;
        }
        Direction optimalDir = findOptimalSpawnDir();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared, enemyTeam);
        if (nearbyEnemies.length > 0) {
            openingBuild = false;
        }
        // Opening build order
        if (!isMidGame && openingBuild) {
            if (optimalDir == null) return;
            if (numSlanderers == 4 && numPoliticians == 0) {
                spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 16, optimalDestination(false), SpawnDestinationFlag.INSTR_DEFEND, spawnDestIsGuess);
            } else if (numSlanderers < 8 && numSlanderers > numMuckrakers) {
                spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, optimalDestination(false), SpawnDestinationFlag.INSTR_MUCKRAKER, spawnDestIsGuess);
            } else if (numSlanderers < 7) {
                int optimalSland = getOptimalSlandererInfluence(rc.getConviction());
                if (optimalSland > 0) {
                    spawnRobotWithTracker(RobotType.SLANDERER, optimalDir, optimalSland, optimalSlandererDestination(), SpawnDestinationFlag.INSTR_SLANDERER, spawnDestIsGuess);
                }
            } else if (numPoliticians < 4) {
                spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 16, optimalDestination(false), SpawnDestinationFlag.INSTR_DEFEND, spawnDestIsGuess);
                if (numPoliticians == 4) {
                    openingBuild = false;
                }
            }
        }
        // Otherwise, do normal build order
        else {
            if (optimalDir != null) {
                // Check for nearby enemy muckraker
                boolean nearbyMuckraker = false;
                double enemyMultiplier = rc.getEmpowerFactor(enemyTeam, 0);
                double remainingHealth = rc.getConviction();
                int nearestEnemyDistance = 10000;
                Direction dirToNearestEnemy = null;
                for (RobotInfo robot : nearbyEnemies) {
                    int distTo = myLocation.distanceSquaredTo(robot.location);
                    if (distTo < nearestEnemyDistance) {
                        nearestEnemyDistance = distTo;
                        dirToNearestEnemy = myLocation.directionTo(robot.location);
                    }
                    switch (robot.type) {
                        case MUCKRAKER: {
                            nearbyMuckraker = true;
                            break;
                        }
                        case POLITICIAN: {
                            remainingHealth -= (robot.conviction - 10) * enemyMultiplier;
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
                dirToNearestEnemy = fanOutFromBuildDir(dirToNearestEnemy);
                optimalDir = dirToNearestEnemy == null ? optimalDir : dirToNearestEnemy;
                int myConviction = rc.getConviction();
                int maxInfluence = Math.min(Math.min(949, rc.getInfluence() - 5), (int)remainingHealth);
                MapLocation optimalNeutral = getLowestInfluenceNeutralEC();
                int currentInfluence = rc.getInfluence();
                double dilutedRemainingHealth = currentInfluence - (currentInfluence - remainingHealth) / 3.0;
                // Highly EC at risk, only build muckrakers to dilute damage
                if (remainingHealth < 0) {
                    //System.out.println\("Spawning muck.");
                    MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                    enemyLocation = (enemySlandererRound > currentRound - 50 && enemySlanderer != null) ? enemySlanderer : enemyLocation;
                    spawnDestIsGuess = enemyLocation.equals(enemySlanderer) ? false : spawnDestIsGuess;
                    int instruction = enemyLocation.equals(enemySlanderer) ? SpawnDestinationFlag.INSTR_MUCK_TO_SLAND : SpawnDestinationFlag.INSTR_MUCKRAKER;
                    spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, instruction, spawnDestIsGuess);
                }
                // Assuming some base level of income, if we know about neutral ECs, get them.
                // numSlanderers*3*5 is a heuristic for our income. It is there so we only save for neutral ECs when we can produce a killer in < ~5 turns.
                else if (numSlanderers > 2 && optimalNeutral != null && (numSlanderers*3*5) + dilutedRemainingHealth > neutralECLocsToInfluence.get(optimalNeutral)) {
                    MapLocation enemyLocation = optimalNeutral;
                    int influence = neutralECLocsToInfluence.get(enemyLocation);
                    int infNeeded = (int)(influence*1.1 + 20);
                    if (dilutedRemainingHealth > infNeeded) {
                        // //System.out.println\("Spawning neutral killer: " + enemyLocation);
                        spawnDestIsGuess = false;   // we are sending them to exactly the neutral ECs
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, infNeeded, enemyLocation, SpawnDestinationFlag.INSTR_ATTACK, spawnDestIsGuess);
                        sentRobotsToNeutralECs.put(enemyLocation, currentRound);
                    } else {
                        // //System.out.println\("Biding time and saving for neutral killer.");
                        //System.out.println\("Spawning muck.");
                        enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                        enemyLocation = (enemySlandererRound > currentRound - 50 && enemySlanderer != null) ? enemySlanderer : enemyLocation;
                        spawnDestIsGuess = enemyLocation.equals(enemySlanderer) ? false : spawnDestIsGuess;
                        int instruction = enemyLocation.equals(enemySlanderer) ? SpawnDestinationFlag.INSTR_MUCK_TO_SLAND : SpawnDestinationFlag.INSTR_MUCKRAKER;
                        spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, instruction, spawnDestIsGuess);
                    }
                }
                // No more neutral ECs! Cases below here should not run if we know there are neutral ECs and we have the requisite income.
                // If don't have majority votes and not contested and no nearby muckrakers and has sufficient influence
                else if (rc.getTeamVotes() < 751 && remainingHealth > myConviction/2 && !nearbyMuckraker && myConviction < 8000
                    && (numSlanderers - 1) * 2 < (numMuckrakers + numPoliticians)*Math.ceil((double)(currentRound+1)/(double)500)
                    && (maxInfluence >= 41 || isMidGame)) {
                    int slandInfluence = getOptimalSlandererInfluence(maxInfluence);
                    // //System.out.println\("SPAWN SLANDERER:  " + enemyLocation + " " + shiftedLocation);
                    if (slandInfluence > 0) {
                        spawnRobotWithTracker(RobotType.SLANDERER, optimalDir, slandInfluence, optimalSlandererDestination(), SpawnDestinationFlag.INSTR_SLANDERER, spawnDestIsGuess);
                    }
                }
                // Politicians vs muckrakers ratio 3:2 in the later game
                // Ratio 2:3 in early game
                else if (numPoliticians > numMuckrakers * poliMuckRatio()) {
                    int muckInf = 1;
                    if (Math.random() < 0.2) {
                        muckInf = (int) Math.pow(rc.getConviction(), 0.8);
                    }
                    //System.out.println\("Spawning muck.");
                    MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                    enemyLocation = (enemySlandererRound > currentRound - 50 && enemySlanderer != null) ? enemySlanderer : enemyLocation;
                    spawnDestIsGuess = enemyLocation.equals(enemySlanderer) ? false : spawnDestIsGuess;
                    int instruction = enemyLocation.equals(enemySlanderer) ? SpawnDestinationFlag.INSTR_MUCK_TO_SLAND : SpawnDestinationFlag.INSTR_MUCKRAKER;
                    spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, muckInf, enemyLocation, instruction, spawnDestIsGuess);
                    // //System.out.println\("Spawn Muckraker: " + enemyLocation);
                }
                // Build politician
                else {
                    ////System.out.println\("Medium Sized Politician threshold: " + mediumSizedPolitician);
                    ////System.out.println\("Diluted Remaining Health:" + dilutedRemainingHealth);
                    if (Math.random() < 0.5) { // spawn defender
                        MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                        //System.out.println\("Spawning defender: " + enemyLocation);
                        int influence = rc.getRoundNum() < 50 ? 14 : 18;
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, influence, enemyLocation, SpawnDestinationFlag.INSTR_DEFEND, false);
                    } else if (currentInfluence > 10000) {
                        MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(true) : optimalDestination(true);
                        //System.out.println\("Spawning thicc killer: " + enemyLocation);
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, (int) Math.sqrt(currentInfluence) * 10, enemyLocation, SpawnDestinationFlag.INSTR_ATTACK, spawnDestIsGuess);
                    } else if (currentInfluence > 1000) {
                        MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(true) : optimalDestination(true);
                        //System.out.println\("Spawning killer: " + enemyLocation);
                        int instr = SpawnDestinationFlag.INSTR_ATTACK;
                        if (rc.getRoundNum() > 300 && Math.random() < 0.5) {
                            instr = SpawnDestinationFlag.INSTR_DEFEND_ATTACK;
                        }
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, 1000, enemyLocation, instr, spawnDestIsGuess);
                    } else if ((approxNumSlanderers*3*5) + dilutedRemainingHealth > mediumSizedPolitician && dilutedRemainingHealth > 50) {
                        int infNeeded = mediumSizedPolitician;
                        if (dilutedRemainingHealth > infNeeded) {
                            MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(true) : optimalDestination(true);
                            //System.out.println\("Spawning medium sized: " + enemyLocation);
                            int instr = SpawnDestinationFlag.INSTR_ATTACK;
                            if (rc.getRoundNum() > 300 && Math.random() < 0.5) {
                                instr = SpawnDestinationFlag.INSTR_DEFEND_ATTACK;
                            }
                            spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, mediumSizedPolitician, enemyLocation, instr, spawnDestIsGuess);
                            mediumSizedPolitician = 300 + (int)(200.0*Math.random());
                        } else {
                            //System.out.println\("Spawning muck.");
                            //System.out.println\("Biding time waiting for medium-sized Politician!");
                            MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                            enemyLocation = (enemySlandererRound > currentRound - 50 && enemySlanderer != null) ? enemySlanderer : enemyLocation;
                            spawnDestIsGuess = enemyLocation.equals(enemySlanderer) ? false : spawnDestIsGuess;
                            int instruction = enemyLocation.equals(enemySlanderer) ? SpawnDestinationFlag.INSTR_MUCK_TO_SLAND : SpawnDestinationFlag.INSTR_MUCKRAKER;
                            spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, instruction, spawnDestIsGuess);
                        }
                    } else {
                        MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                        //System.out.println\("Spawning defender: " + enemyLocation);
                        int influence = 14;
                        if (rc.getRoundNum() > 100) {
                            if (Math.random() < 0.5) {
                                influence = 18;
                            }
                            else {
                                influence = 30;
                            }
                        }
                        int instruction = SpawnDestinationFlag.INSTR_ATTACK;
                        if (Math.random() < 0.4) {
                            instruction = SpawnDestinationFlag.INSTR_DEFEND_ATTACK;
                        }
                        spawnRobotWithTracker(RobotType.POLITICIAN, optimalDir, influence, enemyLocation, instruction, spawnDestIsGuess);
                        mediumSizedPolitician = 300 + (int)(200.0*Math.random());   // re-sample medium sized politician.
                    }
                }
                if (rc.isReady()) {
                    //System.out.println\("Spawning muck.");
                    MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(false) : optimalDestination(false);
                    enemyLocation = (enemySlandererRound > currentRound - 50 && enemySlanderer != null) ? enemySlanderer : enemyLocation;
                    spawnDestIsGuess = enemyLocation.equals(enemySlanderer) ? false : spawnDestIsGuess;
                    int instruction = enemyLocation.equals(enemySlanderer) ? SpawnDestinationFlag.INSTR_MUCK_TO_SLAND : SpawnDestinationFlag.INSTR_MUCKRAKER;
                    spawnRobotWithTracker(RobotType.MUCKRAKER, optimalDir, 1, enemyLocation, instruction, spawnDestIsGuess);
                }
            }
        }
    }

    int getOptimalSlandererInfluence(int maxInfluence) {
        int optimalSland = Arrays.binarySearch(SLANDERER_INFLUENCE_THRESHOLDS, maxInfluence);
        if (optimalSland == -1) {
            return 0;
        }
        if (optimalSland < 0) {
            optimalSland = -optimalSland;
            optimalSland -=2;
        }
        return SLANDERER_INFLUENCE_THRESHOLDS[optimalSland];
    }

    MapLocation optimalSlandererDestination() {
        MapLocation enemyLocation = isMidGame ? optimalDestinationMidGame(true) : optimalDestination(true);
        Direction awayFromEnemy = enemyLocation.directionTo(myLocation);
        MapLocation oneStep = myLocation.add(awayFromEnemy);
        int dx = oneStep.x - myLocation.x;
        int dy = oneStep.y - myLocation.y;
        int multiplier = turnCount < 50 ? 5 : 10;
        return myLocation.translate(multiplier*dx, multiplier*dy);
    }

    double poliMuckRatio() {
        if (rc.getRoundNum() < 100) {
            return 0.6;
        } else if (rc.getRoundNum() < 200) {
            return 0.8;
        } else if (rc.getRoundNum() < 400) {
            return 1;
        } else if (rc.getRoundNum() < 800) {
            return 1.2;
        } return 1.5;
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
                    case Flag.MAP_INFO_SCHEMA:
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
                        startTrackingBot(spawnID, spawnType);
                        // //System.out.println\("Ally " + allyECLocs[i] + " told me about new " + spawnType);
                        break;
                    case Flag.SPAWN_DESTINATION_SCHEMA:
                        if (isMidGame) {
                            SpawnDestinationFlag sdf = new SpawnDestinationFlag(flagInt);
                            if (sdf.readInstruction() == SpawnDestinationFlag.INSTR_ATTACK ||
                                sdf.readInstruction() == SpawnDestinationFlag.INSTR_DEFEND_ATTACK) {
                                // the robot spawned is going to an enemy, we want to record destination
                                // so we can use it as a destination for our own robots.
                                MapLocation potentialEnemy = sdf.readAbsoluteLocation(myLocation);
                                if (!(potentialEnemy.x == myLocation.x && potentialEnemy.y == myLocation.y)) {
                                    basesToDestinations.put(allyECIDs[i], new Destination(potentialEnemy, sdf.readGuess()));
                                    //System.out.println\("Base " + allyECIDs[i] + " told me about a destination " + potentialEnemy);
                                }
                            }
                        }
                        break;
                    case Flag.UNIT_UPDATE_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                    case Flag.MIDGAME_ALLY_SCHEMA:
                        // Not relevant, ECs do not send such flags to ECs.
                        break;
                }
            } else {
                // Oh no! ally has been captured
                // Delete from list by replacing elem i with last elem of List
                // and decrementing list length.
                if (!enemyECLocsToInfluence.containsKey(allyECLocs[i])) {
                    // Add to list on enemies.
                    enemyECLocsToInfluence.put(allyECLocs[i], null);
                    map.set(allyECLocs[i].x-myLocation.x, allyECLocs[i].y-myLocation.y, RelativeMap.ENEMY_EC);
                }
                // For mid-game ECs. Update another list to remove this EC.
                if (basesToDestinations != null && basesToDestinations.containsKey(allyECIDs[i])) {
                    basesToDestinations.remove(allyECIDs[i]);
                }
                // Remove EC from list.
                allyECIDs[i] = allyECIDs[numAllyECs-1];
                allyECLocs[i] = allyECLocs[numAllyECs-1];
                numAllyECs -= 1;
                i -= 1;
                // //System.out.println\("Ally Lost! I now have " + numAllyECs + " original allies.");
            }
        }
    }

    /**
     * Helper function to start tracking a robot with a given ID and type.
     */
    void startTrackingBot(int id, RobotType type) {
        // //System.out.println\("Tracking " + type.toString() + "#" + id);
        if (numUnitsTracked == MAX_UNITS_TRACKED) return;
        int typeInt = typeToInt(type);
        trackingList[numUnitsTracked] = (typeInt << 28) + id;
        numUnitsTracked++;
    }

    /**
     * Helper function to get the ID of a robot being tracked.
     */
    int getTrackedID(int index) {
        return trackingList[index] & 268435455;
    }

    /**
     * Helper function to get the type of a robot being tracked.
     */
    RobotType getTrackedType(int index) {
        return robotTypes[trackingList[index] >> 28 ];
    }

    /**
     * Stop tracking a robot at a particular index, e.g. when the robot is dead.
     */
    void stopTrackingBot(int index) {
        trackingList[index] = trackingList[numUnitsTracked - 1];
        numUnitsTracked--;
    }

    /**
     * Reads flags from all tracked units (not ally ECs).
     * Handles logic: Given a flag from a unit we're tracking,
     * what does this EC do?
     */
    void updateUnitTrackers() throws GameActionException {
        for (int i = 0; i < numUnitsTracked; i++) {
            int trackedID = getTrackedID(i);
            if (!rc.canGetFlag(trackedID)) { // if I can't get its flag, the bot is dead
                if (getTrackedType(i) == RobotType.SLANDERER) {
                    approxNumSlanderers -= 1.0/((double)(numAllyECs+1));
                }
                stopTrackingBot(i);
                i--;
                continue;
            }
            int flagInt = rc.getFlag(trackedID);
            switch (Flag.getSchema(flagInt)) {
                case Flag.NO_SCHEMA:
                    break;
                case Flag.EC_SIGHTING_SCHEMA:
                    ECSightingFlag ecsf = new ECSightingFlag(flagInt);
                    MapLocation ecLoc = ecsf.readAbsoluteLocation(myLocation);
                    int[] relECLoc = new int[] { ecLoc.x - myLocation.x, ecLoc.y - myLocation.y };
                    int ecInf = ecsf.readECInfluence();
                    if (ecsf.readECType() == ECSightingFlag.NEUTRAL_EC && !ecLoc.equals(myLocation)) {
                        if (!neutralECLocsToInfluence.containsKey(ecLoc)) {
                            map.set(relECLoc, RelativeMap.NEUTRAL_EC);
                            //System.out.println\("Informed about NEUTRAL EC at " + ecLoc + " with influence " + ecInf);
                        }
                        neutralECLocsToInfluence.put(ecLoc, ecInf);
                    } else if (ecsf.readECType() == ECSightingFlag.ENEMY_EC && !ecLoc.equals(myLocation)) {
                        if (!enemyECLocsToInfluence.containsKey(ecLoc)) {
                            map.set(relECLoc, RelativeMap.ENEMY_EC);
                            // This EC has been converted from neutral to enemy since we last saw it.
                            if(neutralECLocsToInfluence.containsKey(ecLoc)) {
                                neutralECLocsToInfluence.remove(ecLoc);
                            }
                            // This EC has been converted from a captured ally to an enemy since we last saw it.
                            if(capturedAllyECLocsToInfluence.containsKey(ecLoc)) {
                                capturedAllyECLocsToInfluence.remove(ecLoc);
                            }
                            // It is also possible for one of our original allies to be converted into an enemy.
                            // If that happens, we will remove the ally from allyECIDs and allyECLocs when we
                            // are looking for its flag in readAllyECUpdates() and cannot read it.
                            //System.out.println\("Informed about ENEMY EC at " + ecLoc + " with influence " + ecInf);
                        }
                        enemyECLocsToInfluence.put(ecLoc, ecInf);
                    } else if (ecsf.readECType() == ECSightingFlag.ALLY_EC && !ecLoc.equals(myLocation)) {
                        if (!capturedAllyECLocsToInfluence.containsKey(ecLoc)) {
                            map.set(relECLoc, RelativeMap.ALLY_EC);
                            if (enemyECLocsToInfluence.containsKey(ecLoc)) {
                                enemyECLocsToInfluence.remove(ecLoc);
                            }
                            if(neutralECLocsToInfluence.containsKey(ecLoc)) {
                                neutralECLocsToInfluence.remove(ecLoc);
                            }
                            //System.out.println\("Informed about new ALLY EC at " + ecLoc + " with influence " + ecInf);
                        }
                        capturedAllyECLocsToInfluence.put(ecLoc, ecInf);
                    }
                    break;
                case Flag.MAP_INFO_SCHEMA:
                    // Handles Edge logic.
                    MapInfoFlag mif = new MapInfoFlag(flagInt);
                    Direction edgeDir = mif.readEdgeDirection();
                    int[] relLocs = mif.readRelativeLocationFrom(myLocation);
                    // //System.out.println\("relLocs: " + relLocs[0] + ", " + relLocs[1] + "; dir: " + edgeDir);
                    switch(edgeDir) {
                        case NORTH:
                            map.set(relLocs[0], relLocs[1]-1, 1);
                            map.set(relLocs, 0);
                            break;
                        case EAST:
                            map.set(relLocs[0]-1, relLocs[1], 1);
                            map.set(relLocs, 0);
                            break;
                        case SOUTH:
                            map.set(relLocs[0], relLocs[1]+1, 1);
                            map.set(relLocs, 0);
                            break;
                        case WEST:
                            map.set(relLocs[0]+1, relLocs[1], 1);
                            map.set(relLocs, 0);
                            break;
                        default:
                            break;
                    }
                    // map.summarize();
                    break;
                case Flag.LOCATION_SCHEMA:
                    break;
                case Flag.SPAWN_UNIT_SCHEMA:
                    // handled in readAllyECUpdates()
                    break;
                case Flag.SPAWN_DESTINATION_SCHEMA:
                    break;
                case Flag.UNIT_UPDATE_SCHEMA:
                    if (numUUFprocessed < 300) {
                        UnitUpdateFlag uuf = new UnitUpdateFlag(flagInt);
                        if (uuf.readHasNearbyEnemy()) {
                            if(uuf.readEnemyType() == RobotType.SLANDERER) {
                                enemySlanderer = uuf.readAbsoluteLocation(myLocation);
                                ////System.out.println\("ENEMY SLANDERER AT: " + enemySlanderer);
                                enemySlandererRound = currentRound;
                                break;
                            }
                        }
                        numUUFprocessed += 1;
                    }
                    break;
                case Flag.MIDGAME_ALLY_SCHEMA:
                    // Relevant for unit --> mid-game ECs.
                    // Units communicate to mid-game ECs their base ID and location.
                    // Here, we process those flags.
                    if (isMidGame) {
                        MidGameAllyFlag maf = new MidGameAllyFlag(flagInt);
                        if (maf.readType() == MidGameAllyFlag.ID_MAF) {
                            int newBaseID = maf.readID();
                            if (!trackedBases.contains(newBaseID)) {
                                pendingBaseLocations.put(trackedID, newBaseID);
                            }
                        } else {
                            // Must be LOCATION_MAF
                            if(pendingBaseLocations.containsKey(trackedID)) {
                                // Ensure base is not already tracked and added to list of allies.
                                int newBaseID = pendingBaseLocations.get(trackedID);
                                if (!trackedBases.contains(newBaseID)) {
                                    MapLocation baseLoc = maf.readAbsoluteLocation(myLocation);
                                    int[] relLoc = maf.readRelativeLocationFrom(myLocation);
                                    map.set(relLoc, RelativeMap.ALLY_EC);
                                    allyECIDs[numAllyECs] = newBaseID;
                                    allyECLocs[numAllyECs] = baseLoc;
                                    numAllyECs += 1;
                                    // //System.out.println\("A robot has just informed me of its BASE at " + baseLoc);
                                    trackedBases.add(newBaseID);
                                } else {
                                    // We're already aware of this robot's base, no need to keep in set.
                                    pendingBaseLocations.remove(trackedID);
                                }
                            }
                        }
                    }
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
            startTrackingBot(id, type);
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
    boolean spawnRobotWithTracker(RobotType type, Direction direction, int influence, MapLocation destination, int instruction, boolean isGuess) throws GameActionException {
        if (spawnRobot(type, direction, influence, destination, instruction, isGuess)) {
            int id = rc.senseRobotAtLocation(myLocation.add(direction)).ID;
            startTrackingBot(id, type);
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
     * Returns the closest direction to input destination parameter
     *  which we can build.
     */
    Direction fanOutFromBuildDir(Direction toDest) throws GameActionException {
        if (toDest == null) {
            return null;
        }
        Direction[] fanDirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
                              toDest.rotateRight().rotateRight(), toDest.opposite().rotateLeft(), toDest.opposite().rotateRight(), toDest.opposite()};
        for (Direction d: fanDirs) {
            // Dummy robot build check for valid direction
            if (rc.canBuildRobot(RobotType.MUCKRAKER, d, 1)) {
                return d;
            }
        }
        return null;
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
        // MapLocation spawnLoc = myLocation.add(direction);
        // //System.out.println\("Built " + type.toString() + " silently at " + spawnLoc.toString());
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
    boolean spawnRobot(RobotType type, Direction direction, int influence, MapLocation destination, int instruction, boolean isGuess) throws GameActionException {
        if (!rc.canBuildRobot(type, direction, influence)) {
            return false;
        }
        rc.buildRobot(type, direction, influence);
        switch (type) {
            case MUCKRAKER:
                numMuckrakers++;
                break;
            case POLITICIAN:
                numPoliticians++;
                break;
            case SLANDERER:
                numSlanderers++;
                approxNumSlanderers += 1;
                break;
            default:
                break;
        }
        MapLocation spawnLoc = myLocation.add(direction);
        if (isGuess) {
            // //System.out.println\("Built " + type.toString() + " at " + spawnLoc.toString() + " to " + destination + " in explore mode.");
        } else {
            // //System.out.println\("Built " + type.toString() + " at " + spawnLoc.toString() + " to " + destination + " in precise mode.");
        }
        int newBotID = rc.senseRobotAtLocation(spawnLoc).ID;
        latestSpawnRound = currentRound;
        latestSpawnFlag = new SpawnUnitFlag(type, direction, newBotID);
        latestSpawnDestinationFlag = new SpawnDestinationFlag(destination, instruction, isGuess);
        return true;
    }


    /**
     * Get lowest influence neutral EC. Tiebreaker goes to closest EC.
     */
    MapLocation getLowestInfluenceNeutralEC() {
        if (neutralECLocsToInfluence.isEmpty()) {
            return null;
        }
        int minInf = 1000000;
        int minDist = 1000000; // distance is tiebreaker for equal influence.
        MapLocation minLoc = null;
        for (MapLocation neutralECLoc : neutralECLocsToInfluence.keySet()) {
            if (sentToNeutralRecently(neutralECLoc)) {
                continue;
            }
            int thisECInfluence = neutralECLocsToInfluence.get(neutralECLoc);
            if (thisECInfluence < minInf) {
                minInf = thisECInfluence;
                minDist = myLocation.distanceSquaredTo(neutralECLoc);
                minLoc = neutralECLoc;
            } else if (thisECInfluence == minInf) {
                if (myLocation.distanceSquaredTo(neutralECLoc) < minDist) {
                    minInf = thisECInfluence;
                    minDist = myLocation.distanceSquaredTo(neutralECLoc);
                    minLoc = neutralECLoc;
                }
            }
        }
        return minLoc;
    }

    /**
     * Determines destination to send robots. First choice is closest enemyECLoc.
     * If includeNeutral is false, then it does not consider neutralECLocsToInfluence as potential destinations.
     * If we don't know about any ECs, goes to fallback based on heuristic
     */
     MapLocation optimalDestination(boolean includeNeutral) {
         return optimalDestination(includeNeutral, false, false);
     }

     MapLocation optimalDestination(boolean includeNeutral, boolean prioritizeDistanceOverEnemy) {
         return optimalDestination(includeNeutral, prioritizeDistanceOverEnemy, false);
     }

    MapLocation optimalDestination(boolean includeNeutral, boolean prioritizeDistanceOverEnemy, boolean killerPolitician) {
        boolean randomFallback = false;
        if (currentRound < searchBounds.length) {
            int[] dArr = randomDestination();
            return myLocation.translate(dArr[0], dArr[1]);
        }
        MapLocation enemyLocation = null;
        int enemyLocationDistance = 999999999;
        if (prioritizeDistanceOverEnemy) {
            if (enemyECLocsToInfluence.size() > 0) {
                for (MapLocation enemyECLoc : enemyECLocsToInfluence.keySet()) {
                    int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                    if (enemyECLocDestination < enemyLocationDistance) {
                        enemyLocation = enemyECLoc;
                        enemyLocationDistance = enemyECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            }
            if (includeNeutral && neutralECLocsToInfluence.size() > 0) {
                for (MapLocation neutralECLoc : neutralECLocsToInfluence.keySet()) {
                    int neutralECLocDistance = myLocation.distanceSquaredTo(neutralECLoc);
                    if (neutralECLocDistance < enemyLocationDistance) {
                        if (!killerPolitician || !sentToNeutralRecently(neutralECLoc)) {
                            enemyLocation = neutralECLoc;
                            enemyLocationDistance = neutralECLocDistance;
                            spawnDestIsGuess = false;
                        }
                    }
                }
            }
        } else {
            if (enemyECLocsToInfluence.size() > 0) {
                for (MapLocation enemyECLoc : enemyECLocsToInfluence.keySet()) {
                    int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                    if (enemyECLocDestination < enemyLocationDistance) {
                        enemyLocation = enemyECLoc;
                        enemyLocationDistance = enemyECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            } else if (includeNeutral && neutralECLocsToInfluence.size() > 0) {
                for (MapLocation neutralECLoc : neutralECLocsToInfluence.keySet()) {
                    int neutralECLocDestination = myLocation.distanceSquaredTo(neutralECLoc);
                    if (neutralECLocDestination < enemyLocationDistance) {
                        enemyLocation = neutralECLoc;
                        enemyLocationDistance = neutralECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            }
        }
        if (enemyLocation == null) {
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
                double denom = (double)(vertFurthestWall + horizFurthestWall + Math.sqrt(2*vertFurthestWall*horizFurthestWall));
                if (symmetries[0] == true && symmetries[1] == true) {
                    // //System.out.println\("Unknown symmetry. Horizontal and vertical both potential.");
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
                        if (rand < (double)horizFurthestWall/denom) {
                            dArr = dVert;
                        } else if (rand < (double)(horizFurthestWall + vertFurthestWall)/denom) {
                            dArr = dHoriz;
                        } else {
                            dArr[0] = dHoriz[0];
                            dArr[1] = dVert[1];
                        }
                    }
                } else if (symmetries[0] == true) {
                    // //System.out.println\("Horizontal Symmetry."); // send units vertically
                    dArr = optimalVerticalDestination(vertAbsSum, vertSum, vertFurthestDirection, vertFurthestWall);
                } else if (symmetries[1] == true) {
                    // //System.out.println\("Vertical Symmetry.");   // send units horizontally
                    dArr = optimalHorizontalDestination(horizAbsSum, horizSum, horizFurthestDirection, horizFurthestWall);
                } else {
                    // only rotational symmetry possible
                    // //System.out.println\("Only rotational symmetry.");
                    // //System.out.println\("Ally Distance Horiz: " + allyDistances[0] + " Vert: " + allyDistances[1]);
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
                        // //System.out.println\("Sending: X: " + stop*sendX + " Y:" + stop*sendY);
                        dArr[0] = horizFurthestDirection == Direction.EAST ? stop*sendX : -stop*sendX;
                        dArr[1] = vertFurthestDirection == Direction.NORTH ? stop*sendY : -stop*sendY;
                        enemyLocation = myLocation.translate(dArr[0], dArr[1]);
                        // //System.out.println\("Sending to enemyLoc: " + enemyLocation);
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
            spawnDestIsGuess = true;
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
            if (k < (double)map.xLineAboveUpper/(double)horizAbsSum) {
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
            if (k < (double)map.yLineAboveUpper/(double)vertAbsSum) {
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
                // //System.out.println\("I set my initial flag to: " + initialFaf.flag);
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
                    // //System.out.println\("Found a POTENTIAL ally! ID: " + i + ". I now have: " + numAllyECs + " allies.");
                    // //System.out.println\("Adding POTENTIAL ally " + i + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                }
            }
            firstRoundIDsToConsider.clear();
        }

        // Continue scanning for friendly ECs
        if (!scannedAllIDs) {
            int startPoint = searchBounds[searchRound];
            int endPoint = searchBounds[searchRound+1];
            // //System.out.println\("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
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
                        // //System.out.println\("Adding POTENTIAL ally " + j + " at RELATIVE location (" + relLocs[0] + ", " + relLocs[1] + ")");
                    } else if (currentRound == 1) {
                        // First turn, not guaranteed that ally flags are set to be secret code yet.
                        // Add to list to revisit in round 2.
                        firstRoundIDsToConsider.add(j);
                    }
                }
            }
            // //System.out.println\("Round: " + rc.getRoundNum() + " Bytecodes: " + Clock.getBytecodesLeft());
            searchRound += 1;
            if (searchRound == searchBounds.length-1) {
                scannedAllIDs = true;
                // //System.out.println\("Done finding allies.");
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
     * MID-GAME ALLIES ONLY.
     * Add ally robots around you to your unitTracker if you aren't currently tracking them.
     * Called once per round for mid-game ECs.
     */
    void trackNewNearbyRobots() {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared, allyTeam);
        int maxIters = Math.min(nearbyAllies.length, 20);
        for (int j = 0; j < maxIters; j++) {
            RobotInfo ri = nearbyAllies[j];
            if (ri.ID == myID) {
                continue;
            }
            if (trackedRobots.contains(ri.ID)) {
                continue;
            }
            if (ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                // an ally EC happens to be within vision radius.
                boolean alreadySeen = false;
                for (int i=0; i<numAllyECs; i++) {
                    if (allyECIDs[i] == ri.ID) {
                        alreadySeen = true;
                        break;
                    }
                }
                if (!alreadySeen) {
                    allyECIDs[numAllyECs] = ri.ID;
                    allyECLocs[numAllyECs] = ri.location;
                    map.set(ri.location.x - myLocation.x, ri.location.y - myLocation.y, RelativeMap.ALLY_EC);
                    numAllyECs += 1;
                }
                trackedRobots.add(ri.ID);   // technically, this is not a tracked robot, but that's okay, because this list is just something we check against when we see a new robot and we do not want to spend the bytecode every time to check over the ally EC list.
                continue;
            } else if (numUnitsTracked < MAX_UNITS_TRACKED) {
                startTrackingBot(ri.ID, ri.type);
                trackedRobots.add(ri.ID);
            }
        }
    }

    boolean sentToNeutralRecently(MapLocation neutralLocation) {
        if (sentRobotsToNeutralECs.containsKey(neutralLocation)) {
            int lastSentRound = sentRobotsToNeutralECs.get(neutralLocation);
            if (lastSentRound+60 > currentRound) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    /**
     * MID-GAME ALLIES ONLY.
     * Similar to optimalDestination(), except used for mid-game ECs only. Our potential destinations
     * includes a list of recent destinations our original allies sent robots too.
     */
    MapLocation optimalDestinationMidGame(boolean includeNeutral) {
        return optimalDestinationMidGame(includeNeutral, false, false);
    }

    MapLocation optimalDestinationMidGame(boolean includeNeutral, boolean prioritizeDistanceOverEnemy) {
        return optimalDestinationMidGame(includeNeutral, prioritizeDistanceOverEnemy, false);
    }

    MapLocation optimalDestinationMidGame(boolean includeNeutral, boolean prioritizeDistanceOverEnemy, boolean killerPolitician) {
        MapLocation enemyLocation = null;
        int enemyLocationDistance = 999999999;
        if (prioritizeDistanceOverEnemy) {
            if (enemyECLocsToInfluence.size() > 0) {
                for (MapLocation enemyECLoc : enemyECLocsToInfluence.keySet()) {
                    int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                    if (enemyECLocDestination < enemyLocationDistance) {
                        enemyLocation = enemyECLoc;
                        enemyLocationDistance = enemyECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            }
            if (includeNeutral && neutralECLocsToInfluence.size() > 0) {
                for (MapLocation neutralECLoc : neutralECLocsToInfluence.keySet()) {
                    int neutralECLocDistance = myLocation.distanceSquaredTo(neutralECLoc);
                    if (neutralECLocDistance < enemyLocationDistance) {
                        if (!killerPolitician || !sentToNeutralRecently(neutralECLoc)) {
                            enemyLocation = neutralECLoc;
                            enemyLocationDistance = neutralECLocDistance;
                            spawnDestIsGuess = false;
                        }
                    }
                }
            }
            if (basesToDestinations.size() > 0) {
                for (Integer key: basesToDestinations.keySet()) {
                    Destination myDest = basesToDestinations.get(key);
                    int destDistance = myLocation.distanceSquaredTo(myDest.destLoc);
                    if (destDistance < enemyLocationDistance) {
                        enemyLocation = myDest.destLoc;
                        enemyLocationDistance = destDistance;
                        spawnDestIsGuess = myDest.isGuess;
                    }
                }
            }
        } else {
            if (enemyECLocsToInfluence.size() > 0) {
                for (MapLocation enemyECLoc : enemyECLocsToInfluence.keySet()) {
                    int enemyECLocDestination = myLocation.distanceSquaredTo(enemyECLoc);
                    if (enemyECLocDestination < enemyLocationDistance) {
                        enemyLocation = enemyECLoc;
                        enemyLocationDistance = enemyECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            } else if (basesToDestinations.size() > 0) {
                for (Integer key: basesToDestinations.keySet()) {
                    Destination myDest = basesToDestinations.get(key);
                    int destDistance = myLocation.distanceSquaredTo(myDest.destLoc);
                    if (destDistance < enemyLocationDistance) {
                        enemyLocation = myDest.destLoc;
                        enemyLocationDistance = destDistance;
                        spawnDestIsGuess = myDest.isGuess;
                    }
                }
            } else if (includeNeutral && neutralECLocsToInfluence.size() > 0) {
                for (MapLocation neutralECLoc : neutralECLocsToInfluence.keySet()) {
                    int neutralECLocDestination = myLocation.distanceSquaredTo(neutralECLoc);
                    if (neutralECLocDestination < enemyLocationDistance) {
                        enemyLocation = neutralECLoc;
                        enemyLocationDistance = neutralECLocDestination;
                        spawnDestIsGuess = false;
                    }
                }
            }
        }

        if (enemyLocation == null) {
            // FALLBACK: We don't know about any ECs.
            // Note: Heuristic from optimalDestination() only applies to initial ECs, not mid-game ECs.
            int[] dArr = randomDestination();
            enemyLocation = myLocation.translate(dArr[0], dArr[1]);
            spawnDestIsGuess = true;
        }
        return enemyLocation;
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
