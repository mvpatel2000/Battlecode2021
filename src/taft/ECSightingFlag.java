package taft;

import battlecode.common.*;

/**
 * Flag that robots can use to communicate the location of a non-ally EC.
 */
public class ECSightingFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - mod 128 x coordinate (COORD_BITS)
     * - mod 128 y coordinate (COORD_BITS)
     * - type of EC that was sighted (EC_TYPE_BITS)
     * - the approximate influence of the EC (EC_INF_BITS)
     *
     * Total bits used: 3 + 7 + 7 + 2 + 5 = 24.
     */

    final int EC_TYPE_BITS = 2;
    final int EC_INF_BITS = 5;

    public static final int NEUTRAL_EC = 0;
    public static final int ENEMY_EC = 1;
    public static final int ALLY_EC = 2;

    public ECSightingFlag() {
        super(true);
        setSchema(EC_SIGHTING_SCHEMA);
    }

    public ECSightingFlag(MapLocation loc, int ecType, int inf) {
        super(true);
        setSchema(EC_SIGHTING_SCHEMA);
        writeLocation(loc);
        writeECType(ecType);
        writeECInfluence(inf);
    }

    public ECSightingFlag(int received) {
        super(received);
    }

    public boolean writeECType(int ecType) {
        return writeToFlag(ecType, EC_TYPE_BITS);
    }

    public int readECType() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, EC_TYPE_BITS);
    }

    public boolean writeECInfluence(int influence) {
        int encoded = (int) ((influence - 50) / 15);
        if (encoded > 31) encoded = 31;
        if (encoded < 0) encoded = 0;
        return writeToFlag(encoded, EC_INF_BITS);
    }

    /**
     * Returns an upper bound on the EC's influence when the influence is at most 515.
     * If it returns 530, then the influence is at least 516, but could be arbitrarily high.
     */
    public int readECInfluence() {
        return 15 * readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + EC_TYPE_BITS, EC_INF_BITS) + 65;
    }
}
