package scoutplayer;

import battlecode.common.*;

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

    MapTerrainQueue mtq;

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
        mtq = new MapTerrainQueue();
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        if (mtq.hasRoom() && tryMove(randomDirection())) {
            System.out.println("I moved "+lastMove.toString()+" to "+rc.getLocation().toString());
        }
        mtq.step(lastMove, rc.getLocation());
        MapTerrainFlag mtf = new MapTerrainFlag();
        mtf.setLastMove(lastMove);
        for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
            MapLocation loc = mtq.pop();
            mtf.addPassability(rc.sensePassability(loc));
        }
        rc.setFlag(mtf.getFlag());
    }

    /*
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
}