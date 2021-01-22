package smite;

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

    // TODO: optimize log2? Better formula here?
    public boolean writeECInfluence(int influence) {
        return writeToFlag(1+(int)(Math.log(influence) / 0.69314718056), EC_INF_BITS);
    }

    public int readECInfluence() {
        return (int) Math.pow(2, readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + EC_TYPE_BITS, EC_INF_BITS) - 0.5);
    }
}
