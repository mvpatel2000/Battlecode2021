package smite;

import battlecode.common.*;

/**
 * Flag that Robots use when the see a new ally EC
 * that they have never seen before. The robot uses this flag to
 * communicate its base ID so the new ally EC can listen to its
 * base's messages.
 */
public class MidGameAllyFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - schema (3 bits)
     * - type (1 bit)
     * - baseID (16 bits) if type = 0 OR baseLocation (14 bits) if type = 1
     * Total bits used: 3 + 1 + 16 = 20 OR 3 + 1 + 14 = 18.
     */

    final int ID_BITS = 16;
    final int TYPE_BITS = 1;
    // Types of MAFs.
    public static final int ID_MAF = 0;
    public static final int LOCATION_MAF = 1;

    final int MIN_ROBOT_ID = 10000;

    public MidGameAllyFlag() {
        super(true); // call the LocationFlag constructor that doesn't set the schema
        setSchema(MIDGAME_ALLY_SCHEMA);
    }

    /* Call this when you know the int you just received
     * represents a FindAllyFlag.
     */
    public MidGameAllyFlag(int received) {
        super(received);
    }

    public boolean writeType(int type) {
        return writeToFlag(type, TYPE_BITS);
    }

    public boolean writeID(int id) {
        return writeToFlag(id-MIN_ROBOT_ID, ID_BITS);
    }

    // Use the writeLocation method in the superclass.

    // Read methods
    public int readType() {
        return readFromFlag(SCHEMA_BITS, TYPE_BITS);
    }

    public int readID() {
        return MIN_ROBOT_ID + readFromFlag(SCHEMA_BITS + TYPE_BITS, ID_BITS);
    }

    /**
     * Returns an array of two ints representing
     * [location.x % 128, location.y % 128]
     */
    public int[] readLocation() {
        return new int[]{ readFromFlag(SCHEMA_BITS + TYPE_BITS, COORD_BITS), readFromFlag(SCHEMA_BITS + TYPE_BITS + COORD_BITS, COORD_BITS) };
    }

    /**
     * Get the vector from the robot reading this flag to the location
     * encoded in the flag.
     * @param myLoc Location of the robot reading the flag.
     * @return [xRel, yRel] vector that lies inside the map from the
     * robot reading the location to the location in this flag.
     */
    public int[] readRelativeLocationFrom(MapLocation myLoc) {
        // extract relative x and y s.t. both lie within the map
        int xRel = readFromFlag(SCHEMA_BITS + TYPE_BITS, COORD_BITS) - (myLoc.x & 127);
        if (xRel > 64) {
            xRel -= 128;
        } else if (xRel < -64) {
            xRel += 128;
        }
        int yRel = readFromFlag(SCHEMA_BITS + TYPE_BITS + COORD_BITS, COORD_BITS) - (myLoc.y & 127);
        if (yRel > 64) {
            yRel -= 128;
        } else if (yRel < -64) {
            yRel += 128;
        }
        return new int[]{ xRel, yRel };
    }
}
