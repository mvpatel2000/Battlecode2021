package scoutplayer;

import battlecode.common.*;

// FIFO queue implemented as circular buffer with overwriting
// in case of overflow.
public class MapTerrainQueue {

    final int CAPACITY = 50;
    final int MAX_NEW_TILES_PER_STEP = 15;

    int[][] queue;
    int start;
    int size;

    public MapTerrainQueue() {
        queue = new int[CAPACITY][2];
        start = 0;
        size = 0;
    }

    public int getSize() {
        return size;
    }

    /*
     * Add newly sensable locations to the queue after taking a
     * step in direction di and ending at location currentLoc.
     * If the queue overflows, then the elements at the front
     * of the queue are overwritten.
     */
    public void step(Direction di, MapLocation currentLoc) throws GameActionException {
        // Call the function specific to the unit; for now, it is Politician
        int[][] newTiles = Politician.newSensedLocationsRelative(di);
        System.out.println(newTiles);
        int idx = start + size;
        if (idx >= CAPACITY) idx -= CAPACITY; // modulo expensive, use if instead
        for (int i = 0; i < newTiles.length; i++) {
            queue[idx][0] = newTiles[i][0] + currentLoc.x;
            queue[idx][1] = newTiles[i][1] + currentLoc.y;
            idx++;
            if (idx == CAPACITY) idx = 0;
        }
        if (size + newTiles.length > CAPACITY) { // if we overflowed
            start += size + newTiles.length - CAPACITY; // increment start by overflow amount
            size = CAPACITY; // max out size
        } else {
            size = size + newTiles.length;
        }
    }

    public MapLocation pop() {
        if (size == 0) {
            return null;
        }
        MapLocation newLoc = new MapLocation(queue[start][0], queue[start][1]);
        start++;
        if (start == CAPACITY) start = 0;
        size--;
        return newLoc;
    }

    public boolean hasRoom() {
        return size + MAX_NEW_TILES_PER_STEP <= CAPACITY;
    }

}