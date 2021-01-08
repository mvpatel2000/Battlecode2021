package scoutplayer;

import battlecode.common.*;

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
        // System.out.println("ScoutTracker.update() bp 0: " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
        if (!active || !rc.canGetFlag(scoutID)) {
            active = false;
            return false;
        }
        MapLocation myLoc = rc.getLocation();
        MapTerrainFlag mtf = new MapTerrainFlag(rc.getFlag(scoutID));
        // System.out.println("ScoutTracker.update() bp 1: " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
        if (mtf.getSchema() == Flag.MAP_TERRAIN_SCHEMA) { // TODO: better way of checking schema?
            Direction lastMove = mtf.readLastMove();
            scoutLoc = scoutLoc.add(lastMove);
            // System.out.println("ScoutTracker.update() bp 2: " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
            mtq.step(null, lastMove, scoutLoc);
            // System.out.println("ScoutTracker.update() bp 3: " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
            for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
                if (mtq.isEmpty()) break;
                MapLocation loc = mtq.pop().loc;
                double pa = mtf.readPassability(i);
                // System.out.println("I hear there's passability " + pa + " at " + loc.toString());
                map.set(loc.x-myLoc.x, loc.y-myLoc.y, pa);
                rc.setIndicatorDot(loc, 0, (int) (255 * pa), 0);
                // System.out.println("ScoutTracker.update() bp 4." + i + ": " + Clock.getBytecodesLeft() + " bytecodes left in round " + rc.getRoundNum());
            }
        }
        return true;
    }
}
