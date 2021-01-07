package scoutplayer;

import battlecode.common.*;

// TODO: current implementation leaves 2 bits unused
// TODO: consider setting PASSABILITY_BITS = 2 and rewriting encodePassability()
public class MapTerrainFlag extends Flag {

    final static int PASSABILITY_BITS = 3;
    final static int LAST_MOVE_BITS = 4;
    final static int NUM_LOCS = 6; // number of locations whose passabilities are stored in the flag

    public MapTerrainFlag() {
        super();
        setSchema(MAP_TERRAIN_SCHEMA);
    }

    public MapTerrainFlag(int flag) {
        super(flag);
    }

    // Caller must ensure that idx is at most NUM_LOCS.
    // Returns downsampled passability.
    public double getPassability(int idx) {
        return decodePassability(readFromFlag(SCHEMA_BITS + LAST_MOVE_BITS + PASSABILITY_BITS * idx, PASSABILITY_BITS));
    }

    public Direction getLastMove() {
        return Robot.allDirections[readFromFlag(SCHEMA_BITS, LAST_MOVE_BITS)];
    }

    public void setLastMove(int lm) {
        writeToFlag(lm, 4);
    }

    public void setLastMove(Direction lm) {
        writeToFlag(Robot.directionToInt(lm), LAST_MOVE_BITS);
    }

    public boolean isFull() {
        return writtenTo + PASSABILITY_BITS > FLAG_BITS;
    }

    public boolean addPassability(double pa) {
        return writeToFlag(encodePassability(pa), PASSABILITY_BITS);
    }

    // Encodes passability in a 3-bit integer.
    // If the input pa is 0, that means that the tile is off the map.
    // Transform [0.1, 1] to [0, 0.9], then map [0, 0.91) to [0, 7).
    public int encodePassability(double pa) {
        switch ((int) Math.floor((1-pa)*100/13)) {
            case 0:
                return 0; // 0.87 < pa <= 1.00
            case 1:
                return 1; // 0.74 < pa <= 0.87
            case 2:
                return 2; // 0.61 < pa <= 0.74
            case 3:
                return 3; // 0.48 < pa <= 0.61
            case 4:
                return 4; // 0.35 < pa <= 0.48
            case 5:
                return 5; // 0.22 < pa <= 0.35
            case 6:
                return 6; // 0.09 < pa <= 0.22
            default: // case 7
                return 7; // pa = 0, i.e. off the map
        }
    }

    public double decodePassability(int pa) {
        switch (pa) {
            case 0:
                return 0.935;
            case 1:
                return 0.805;
            case 2:
                return 0.675;
            case 3:
                return 0.545;
            case 4:
                return 0.415;
            case 5:
                return 0.285;
            case 6:
                return 0.155;
            default: // case 7
                return 0; // off the map
        }
    }
}
