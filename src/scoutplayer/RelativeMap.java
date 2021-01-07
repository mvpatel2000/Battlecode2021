package scoutplayer;

// TODO: intelligent wall finding
public class RelativeMap {

    double[][] map;

    // Vars used to determine map boundaries
    public int xLineAbove; // between 1 and 64
    public int xLineBelow; // between -64 and -1
    public int yLineAbove; // between 1 and 64
    public int yLineBelow; // between -64 and -1
    
    public RelativeMap() {
        map = new double[64][64];
        xLineAbove = 64;
        xLineBelow = -64;
        yLineAbove = 64;
        yLineBelow = -64;
    }

    // xRel and yRel should be between -63 and 63 when pa > 0.
    public void set(int xRel, int yRel, double pa) {
        if (pa == 0) { // impassable tile spotted; update edges of map
            if (xRel > 0 && xRel < xLineAbove) xLineAbove = xRel;
            if (xRel < 0 && xRel > xLineBelow) xLineBelow = xRel;
            if (yRel > 0 && yRel < yLineAbove) yLineAbove = yRel;
            if (yRel < 0 && yRel > yLineBelow) yLineBelow = yRel;
            return;
        }
        map[mod64(xRel)][mod64(yRel)] = pa;
    }

    // Coordinates of relLoc should be between -63 and 63 when pa > 0.
    public void set(int[] relLoc, double pa) {
        if (pa == 0) {
            // TODO: map boundaries
            return;
        }
        map[mod64(relLoc[0])][mod64(relLoc[1])] = pa;
    }

    // Input relative x and y coordinates, which are each between -64 and 127.
    public double get(int xRel, int yRel) {
        return map[mod64(xRel)][mod64(yRel)];
    }

    public double get(int[] relLoc) {
        return map[mod64(relLoc[0])][mod64(relLoc[1])];
    }

    // Mod64 a relative location in-place. Both coordinates must be between -64 and 127.
    public static void mod64(int[] relLoc) {
        relLoc[0] = mod64(relLoc[0]);
        relLoc[1] = mod64(relLoc[1]);
    }

    // Mod64 a relative coordinate between -64 and 127.
    public static int mod64(int c) {
        if (c < 0) return c + 64;
        if (c >= 64) return c - 64;
        return c;
    }
}
