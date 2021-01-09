package scoutplayer;

import battlecode.common.*;

/**
 * Flag that robots can use to communicate the location of a non-ally EC.
 */
public class ECSightingFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x coordinate mod 128 (COORD_BITS)
     * - y coordinate mod 128 (COORD_BITS)
     * - type of EC that was sighted (EC_TYPE_BITS)
     * 
     * Total bits used: 3 + 7 + 7 + 1 = 18.
     */

    final int EC_TYPE_BITS = 1;

    public static final int NEUTRAL_EC = 0;
    public static final int ENEMY_EC = 1;

    public ECSightingFlag() {
        super(true); // call the LocationFlag constructor that doesn't set the schema
        setSchema(EC_SIGHTING_SCHEMA);
    }

    public ECSightingFlag(MapLocation loc, int ecType) {
        super(true); // call the LocationFlag constructor that doesn't set the schema
        setSchema(EC_SIGHTING_SCHEMA);
        writeLocation(loc);
        writeECType(ecType);
    }

    public ECSightingFlag(int received) {
        super(received);
    }

    public boolean writeECType(int ecType) {
        return writeToFlag(ecType, EC_TYPE_BITS);
    }

    public int readECType(int ecType) {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, EC_TYPE_BITS);
    }
}
