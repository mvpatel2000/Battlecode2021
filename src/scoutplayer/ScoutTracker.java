package scoutplayer;

import battlecode.common.*;

// TODO: account for edges of map
public class ScoutTracker {
 
    int scoutID;
    MapLocation scoutLoc;
    RelativeMap map;
    boolean active;
    RobotController rc;
    MapTerrainQueue mtq;

    public ScoutTracker(RobotController ecRC, int idToTrack, MapLocation startingLoc, RelativeMap ecMap) {
        scoutID = idToTrack;
        scoutLoc = startingLoc;
        map = ecMap;
        rc = ecRC;
        active = true;
        mtq = new MapTerrainQueue();
    }

    public boolean isActive() {
        return active;
    }

    public boolean update() throws GameActionException {
        if (!active || !rc.canGetFlag(scoutID)) {
            active = false;
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        int flag = rc.getFlag(scoutID);
        MapTerrainFlag mtf = new MapTerrainFlag(flag);
        Direction lastMove = mtf.getLastMove();
        scoutLoc = scoutLoc.add(lastMove);
        mtq.step(lastMove, scoutLoc);
        for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
            MapLocation loc = mtq.pop();
            double pa = mtf.getPassability(i);
            map.set(loc.x-myLoc.x, loc.y-myLoc.y, pa);
        }
        return true;
    }
}
