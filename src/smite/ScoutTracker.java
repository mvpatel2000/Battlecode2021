package smite;

import battlecode.common.*;

public class ScoutTracker extends UnitTracker {

    MapTerrainQueue mtq;
    int INITIAL_COOLDOWN;

    public ScoutTracker(EnlightmentCenter ec, RobotType type, int idToTrack, MapLocation spawnLoc) {
        super(ec, type, idToTrack, spawnLoc);
        switch (type) {
            case POLITICIAN:
                INITIAL_COOLDOWN = Politician.INITIAL_COOLDOWN;
                break;
            case SLANDERER:
                INITIAL_COOLDOWN = Slanderer.INITIAL_COOLDOWN;
                break;
            case MUCKRAKER:
                INITIAL_COOLDOWN = Muckraker.INITIAL_COOLDOWN;
                break;
            case ENLIGHTENMENT_CENTER:
                break;
        }
        // mtq is initialized as late as possible, after the initial cooldown,
        // in order to allow EC as many bytecodes as possible for initial comms.
    }

    public int update() throws GameActionException {
        int returnVal = super.update();
        if (turnCount < INITIAL_COOLDOWN) { // 10 turn cooldown for the scout hasn't passed yet; nothing to do.
            return 0;
        }
        if (turnCount == INITIAL_COOLDOWN) {
            mtq = new MapTerrainQueue(type); // lazy initialization at the last possible moment
        }
        MapTerrainFlag mtf = new MapTerrainFlag(flagInt);
        if (mtf.getSchema() == Flag.MAP_TERRAIN_SCHEMA) { // TODO: better way of checking schema?
            mtq.step(null, lastMove, currLoc);
            for (int i = 0; i < MapTerrainFlag.NUM_LOCS; i++) {
                if (mtq.isEmpty()) break;
                MapLocation loc = mtq.pop().loc;
                double pa = mtf.readPassability(i);
                ec.map.set(loc.x-ec.myLocation.x, loc.y-ec.myLocation.y, pa);
                ec.//rc.setIndicatorDot(loc, 0, (int) (255 * pa), 0);
            }
        }
        return returnVal;
    }
}
