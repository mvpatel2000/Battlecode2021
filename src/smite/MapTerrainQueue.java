package smite;

import battlecode.common.*;

// FIFO queue implemented as circular buffer with overwriting
// in case of overflow.
public class MapTerrainQueue {

    final int CAPACITY = 50;
    final int MAX_NEW_TILES_PER_STEP = 15;

    MapTerrain[] queue;
    RobotType type;
    int start;
    int size;

    public MapTerrainQueue(RobotType type) {
        queue = new MapTerrain[CAPACITY];
        start = 0;
        size = 0;
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Add newly sensable locations to the queue after taking a
     * step in direction di and ending at location currentLoc.
     * If the queue overflows, then the elements at the front
     * of the queue are overwritten.
     * 
     * Pass in rc in order to sense and store passabilities. If
     * rc is set to null, then step will only fill in the locations
     * on the terrain queue and all passabilities will be set to zero.
     * (This functionality is used by ScoutTracker, who only needs
     * to know the locations and the order they pop off in.)
     */
    public void step(RobotController rc, Direction di, MapLocation currentLoc) throws GameActionException {
        // //System.out.println\("MapTerrainQueue stepping, lastMove was " + di.toString());
        // Call the function specific to the unit; for now, it is Politician
        int[][] newTiles;
        switch(type) {
            case MUCKRAKER:
                return;
                // newTiles = Muckraker.newSensedLocationsRelative(di);
            case SLANDERER:
                newTiles = Slanderer.newSensedLocationsRelative(di);
                break;
            default:
                newTiles = Politician.newSensedLocationsRelative(di);
                break;
        }
        int idx = start + size;
        if (idx >= CAPACITY) idx -= CAPACITY; // modulo expensive, use if instead
        for (int i = 0; i < newTiles.length; i++) {
            double pa = 0;
            MapLocation loc = new MapLocation(newTiles[i][0] + currentLoc.x, newTiles[i][1] + currentLoc.y);
            if (rc != null && rc.onTheMap(loc)) {
                pa = rc.sensePassability(loc);
            }
            queue[idx] = new MapTerrain(loc, pa);
            idx++;
            if (idx == CAPACITY) idx = 0;
        }
        if (size + newTiles.length > CAPACITY) { // if we overflowed
            start += size + newTiles.length - CAPACITY; // increment start by overflow amount
            size = CAPACITY; // max out size
        } else {
            size = size + newTiles.length;
        }

        // //System.out.println\("MapTerrainQueue: " + toString());
    }

    public MapTerrain pop() {
        if (size == 0) {
            return null;
        }
        MapTerrain newTerrain = queue[start];
        start++;
        if (start == CAPACITY) start = 0;
        size--;
        return newTerrain;
    }

    public boolean hasRoom() {
        return size + MAX_NEW_TILES_PER_STEP <= CAPACITY;
    }

    public String toString() {
        String s = "[";
        for (int i = 0; i < size; i++) {
            int idx = start + i;
            if (idx >= CAPACITY) idx -= CAPACITY;
            s += queue[idx].loc.toString() + ", ";
        }
        s += "]";
        return s;
    }
}