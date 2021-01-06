package scoutplayer;

public class MapTerrainFlag extends Flag {

    final int PASSABILITY_BITS = 3;

    public MapTerrainFlag() {
        super();
        setSchema(MAP_TERRAIN_SCHEMA);
    }

    public void setLastMove(int lm) {
        writeToFlag(lm, 4);
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
            default:
                return 7; // pa = 0, i.e. off the map
        }
    }
}
