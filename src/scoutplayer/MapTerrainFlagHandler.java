package scoutplayer;

import battlecode.common.*;

// TODO

public class MapTerrainFlagHandler extends FlagHandler {

    int[][] locQueue;
    
    public MapTerrainFlagHandler(Robot r) throws GameActionException {
        super(r);
    }

    @Override
    public void update(boolean beforeMove) {
        if (beforeMove) {
            return;
        }
    }

    public void updateQueue() {

    }

    @Override
    public int query() {
        return 0;
    }
}
