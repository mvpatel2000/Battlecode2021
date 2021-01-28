package jefferson;

import battlecode.common.*;

public class RelativeMap {

    // double[][] map;
    public static final int ALLY_EC = -1;
    public static final int NEUTRAL_EC = -2;
    public static final int ENEMY_EC = -3;
    // Vars used to determine map boundaries
    public int xLineAboveUpper; // between 1 and 64
    public int xLineAboveLower; // between 1 and 64
    public int xLineBelowLower; // between -64 and -1
    public int xLineBelowUpper; // between -64 and -1
    public int yLineAboveUpper; // between 1 and 64
    public int yLineAboveLower; // between 1 and 64
    public int yLineBelowLower; // between -64 and -1
    public int yLineBelowUpper; // between -64 and -1
    MapLocation myLocation;

    public RelativeMap(MapLocation myLocation) {
        // map = new double[64][64];
        xLineAboveUpper = 64;
        xLineAboveLower = 1;
        xLineBelowLower = -64;
        xLineBelowUpper = -1;
        yLineAboveUpper = 64;
        yLineAboveLower = 1;
        yLineBelowLower = -64;
        yLineBelowUpper = -1;
        this.myLocation = myLocation;
    }

    /**
     * xRel and yRel should be between -63 and 63 when pa > 0. In particular, the
     * vector [xRel, yRel] from myLocation should lie fully within the map.
     * Guaranteed to update map boundaries correctly if impassable tiles are set
     * AFTER passable tiles that are just inside the boundary, e.g. with an
     * iteration through SENSE_SPIRAL_LOCS.
     */
    public void set(int xRel, int yRel, double pa) {
        if (pa == 0) {
            // impassable tile spotted; update edges of map
            // if x > 0 and x < current upper bound of upper line and
            // y coordinate is within known-on-the-map range, then we
            // know that the reason this point is off the map is that
            // this point's x coordinate is too large.
            if (xRel > 0 && xRel < xLineAboveUpper && yRel > yLineBelowUpper && yRel < yLineAboveLower)
                xLineAboveUpper = xRel;
            if (xRel < 0 && xRel > xLineBelowLower && yRel > yLineBelowUpper && yRel < yLineAboveLower)
                xLineBelowLower = xRel;
            if (yRel > 0 && yRel < yLineAboveUpper && xRel > xLineBelowUpper && xRel < xLineAboveLower)
                yLineAboveUpper = yRel;
            if (yRel < 0 && yRel > yLineBelowLower && xRel > xLineBelowUpper && xRel < xLineAboveLower)
                yLineBelowLower = yRel;
        } else {
            // update bounds on boundaries where applicable
            if (xRel < 0 && xRel + 64 < xLineAboveUpper)
                xLineAboveUpper = xRel + 64;
            if (xRel < 0 && xRel <= xLineBelowUpper)
                xLineBelowUpper = xRel - 1;
            if (xRel > 0 && xRel - 64 > xLineBelowLower)
                xLineBelowLower = xRel - 64;
            if (xRel > 0 && xRel >= xLineAboveLower)
                xLineAboveLower = xRel + 1;
            if (yRel < 0 && yRel + 64 < yLineAboveUpper)
                yLineAboveUpper = yRel + 64;
            if (yRel < 0 && yRel <= yLineBelowUpper)
                yLineBelowUpper = yRel - 1;
            if (yRel > 0 && yRel - 64 > yLineBelowLower)
                yLineBelowLower = yRel - 64;
            if (yRel > 0 && yRel >= yLineAboveLower)
                yLineAboveLower = yRel + 1;
            // map[xRel & 63][yRel & 63] = pa;
        }
    }

    public void summarize() {
        System.out.println("left wall in [" + xLineBelowLower + ", " + xLineBelowUpper + "]");
        System.out.println("right wall in [" + xLineAboveLower + ", " + xLineAboveUpper + "]");
        System.out.println("bottom wall in [" + yLineBelowLower + ", " + yLineBelowUpper + "]");
        System.out.println("top wall in [" + yLineAboveLower + ", " + yLineAboveUpper + "]");
    }

    /**
     * Coordinates of relLoc should be between -63 and 63 when pa > 0.
     */
    public void set(int[] relLoc, double pa) {
        set(relLoc[0], relLoc[1], pa);
    }

    public boolean knowAllWalls() {
        return (xLineAboveUpper == xLineAboveLower &&
                xLineBelowUpper == xLineBelowLower &&
                yLineAboveUpper == yLineAboveLower &&
                yLineBelowUpper == yLineBelowLower);
    }

    /**
     * Input relative x and y coordinates, which are each between -64 and 127
     * inclusive.
     */
    // public double get(int xRel, int yRel) {
    //     return map[xRel & 63][yRel & 63];
    // }

    /**
     * Coordinates of relLoc should be between -64 and 127 inclusive.
     */
    // public double get(int[] relLoc) {
    //     return map[relLoc[0] & 63][relLoc[1] & 63];
    // }

    /**
     * Mod64 a relative location in-place. Both coordinates must be between -64 and
     * 127 inclusive.
     */
    public static void mod64(int[] relLoc) {
        relLoc[0] = relLoc[0] & 63;
        relLoc[1] = relLoc[1] & 63;
    }

    /**
     * Converts relative coordinates from 0 to 63 inclusive to MapLocation. CAUTION:
     * the output could be wrong if the requested coordinate is in an unexplored
     * part of the map and little information about the map boundary's location is
     * known. In general, this function should only be called on input coordinates
     * that have already been scouted.
     *
     * Specifically, the output x is well-defined iff either xRel > xLineAbove or
     * xRel < xLineBelow + 65; analogous conditions hold for the output y.
     */
    public MapLocation getAbsoluteLocation(int xRel, int yRel) {
        int x = myLocation.x + xRel;
        int y = myLocation.y + yRel;
        if (xRel > xLineAboveUpper)
            x -= 65;
        if (yRel > yLineAboveUpper)
            y -= 65;
        return new MapLocation(x, y);
    }
}
