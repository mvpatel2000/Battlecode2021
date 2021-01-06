package scoutplayer;

import battlecode.common.*;

// TODO

public class MapTerrainFlagHandler extends FlagHandler {

    final int MAX_QUEUE_SPOTS = 50;
    int[][] locQueue = new int[MAX_QUEUE_SPOTS][2];
    
    public MapTerrainFlagHandler() throws GameActionException {
    }

    @Override
    public void update(boolean beforeMove) {
        if (beforeMove) {
            return;
        }
    }

    public int availableSpots() {
        return 0;
    }

    public void updateQueue() {

    }

    @Override
    public int query() {
        return 0;
    }
}
