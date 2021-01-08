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
        MapTerrainFlag mtf = new MapTerrainFlag(rc.getFlag(scoutID));
        if (mtf.getSchema() == Flag.MAP_TERRAIN_SCHEMA) { // TODO: better way of checking schema?
            Direction lastMove = mtf.getLastMove();
            scoutLoc = scoutLoc.add(lastMove);
            mtq.step(null, lastMove, scoutLoc);
            for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
                if (mtq.isEmpty()) break;
                MapLocation loc = mtq.pop().loc;
                double pa = mtf.getPassability(i);
                // System.out.println("I hear there's passability " + pa + " at " + loc.toString());
                map.set(loc.x-myLoc.x, loc.y-myLoc.y, pa);
                rc.setIndicatorDot(loc, 0, (int) (255 * pa), 0);
            }
        }
        return true;
    }
}
