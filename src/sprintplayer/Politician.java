package sprintplayer;

import battlecode.common.*;

public class Politician extends Unit {

    final int[][] SENSE_SPIRAL_ORDER = {{0,0},{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,-1},{-1,1},{0,2},{2,0},{0,-2},{-2,0},{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{2,2},{2,-2},{-2,-2},{-2,2},{0,3},{3,0},{0,-3},{-3,0},{1,3},{3,1},{3,-1},{1,-3},{-1,-3},{-3,-1},{-3,1},{-1,3},{2,3},{3,2},{3,-2},{2,-3},{-2,-3},{-3,-2},{-3,2},{-2,3},{0,4},{4,0},{0,-4},{-4,0},{1,4},{4,1},{4,-1},{1,-4},{-1,-4},{-4,-1},{-4,1},{-1,4},{3,3},{3,-3},{-3,-3},{-3,3},{2,4},{4,2},{4,-2},{2,-4},{-2,-4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final int[][] NEW_SENSED_LOCS_NORTH = {{1,4},{-1,4},{2,4},{-2,4},{0,5},{3,4},{4,3},{5,0},{-5,0},{-4,3},{-3,4}};
    final int[][] NEW_SENSED_LOCS_NORTHEAST = {{0,4},{4,0},{1,4},{4,1},{3,3},{2,4},{4,2},{4,-2},{-2,4},{0,5},{3,4},{4,3},{5,0},{4,-3},{-3,4}};
    final int[][] NEW_SENSED_LOCS_EAST = {{4,1},{4,-1},{4,2},{4,-2},{0,5},{3,4},{4,3},{5,0},{4,-3},{3,-4},{0,-5}};
    final int[][] NEW_SENSED_LOCS_SOUTHEAST = {{4,0},{0,-4},{4,-1},{1,-4},{3,-3},{4,2},{4,-2},{2,-4},{-2,-4},{4,3},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4}};
    final int[][] NEW_SENSED_LOCS_SOUTH = {{1,-4},{-1,-4},{2,-4},{-2,-4},{5,0},{4,-3},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0}};
    final int[][] NEW_SENSED_LOCS_SOUTHWEST = {{0,-4},{-4,0},{-1,-4},{-4,-1},{-3,-3},{2,-4},{-2,-4},{-4,-2},{-4,2},{3,-4},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3}};
    final int[][] NEW_SENSED_LOCS_WEST = {{-4,-1},{-4,1},{-4,-2},{-4,2},{0,5},{0,-5},{-3,-4},{-4,-3},{-5,0},{-4,3},{-3,4}};
    final int[][] NEW_SENSED_LOCS_NORTHWEST = {{0,4},{-4,0},{-4,1},{-1,4},{-3,3},{2,4},{-4,-2},{-4,2},{-2,4},{0,5},{3,4},{-4,-3},{-5,0},{-4,3},{-3,4}};

    public Politician(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}