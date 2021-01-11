package scoutplayer;

import battlecode.common.*;

public class RelativeMap {

    double[][] map;
    public static final int ALLY_EC = -1;
    // Vars used to determine map boundaries
    public int xLineAbove; // between 1 and 64
    public int xLineBelow; // between -64 and -1
    public int yLineRight; // between 1 and 64
    public int yLineLeft; // between -64 and -1
    MapLocation myLocation;

    public RelativeMap(MapLocation myLocation) {
        map = new double[64][64];
        xLineAbove = 64;
        xLineBelow = -64;
        yLineRight = 64;
        yLineLeft = -64;
        this.myLocation = myLocation;
    }

    /**
     * xRel and yRel should be between -63 and 63 when pa > 0.
     * In particular, the vector [xRel, yRel] from myLocation
     * should lie fully within the map.
     */
    public void set(int xRel, int yRel, double pa) {
        if (pa == 0) {
            // impassable tile spotted; update edges of map
            if (xRel > 0 && xRel < xLineAbove) xLineAbove = xRel;
            if (xRel < 0 && xRel > xLineBelow) xLineBelow = xRel;
            if (yRel > 0 && yRel < yLineRight) yLineRight = yRel;
            if (yRel < 0 && yRel > yLineLeft) yLineLeft = yRel;
        }
        else {
            // update bounds on boundaries where applicable
            if (xRel < 0 && xRel + 64 < xLineAbove) xLineAbove = xRel + 64;
            if (xRel > 0 && xRel - 64 > xLineBelow) xLineBelow = xRel - 64;
            if (yRel < 0 && yRel + 64 < yLineRight) yLineRight = yRel + 64;
            if (yRel > 0 && yRel - 64 > yLineLeft) yLineLeft = yRel - 64;
            map[xRel & 63][yRel & 63] = pa;
        }
    }

    /**
     * Coordinates of relLoc should be between -63 and 63 when pa > 0.
     */
    public void set(int[] relLoc, double pa) {
        set(relLoc[0], relLoc[1], pa);
    }

    /**
     * Input relative x and y coordinates, which are each between -64 and 127 inclusive.
     */
    public double get(int xRel, int yRel) {
        return map[xRel & 63][yRel & 63];
    }

    /**
     * Coordinates of relLoc should be between -64 and 127 inclusive.
     */
    public double get(int[] relLoc) {
        return map[relLoc[0] & 63][relLoc[1] & 63];
    }

    /**
     * Mod64 a relative location in-place. Both coordinates
     * must be between -64 and 127 inclusive.
     */
    public static void mod64(int[] relLoc) {
        relLoc[0] = relLoc[0] & 63;
        relLoc[1] = relLoc[1] & 63;
    }

    /**
     * Converts relative coordinates from 0 to 63 inclusive to MapLocation.
     * CAUTION: the output could be wrong if the requested coordinate is
     * in an unexplored part of the map and little information about the
     * map boundary's location is known. In general, this function should
     * only be called on input coordinates that have already been scouted.
     *
     * Specifically, the output x is well-defined iff either xRel > xLineAbove
     * or xRel < xLineBelow + 65; analogous conditions hold for the output y.
     */
    public MapLocation getAbsoluteLocation(int xRel, int yRel) {
        int x = myLocation.x + xRel;
        int y = myLocation.y + yRel;
        if (xRel > xLineAbove) x -= 65;
        if (yRel > yLineRight) y -= 65;
        return new MapLocation(x, y);
    }
}
