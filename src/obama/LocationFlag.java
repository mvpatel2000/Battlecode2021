package obama;

import battlecode.common.*;

/**
 * Flag that any robot can use to send or receive locations.
 *
 * To add extra information in addition to a location, extend
 * this class (see SpawnDestinationFlag as an example).
 */
public class LocationFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x coordinate mod 128 (COORD_BITS)
     * - y coordinate mod 128 (COORD_BITS)
     *
     * Total bits used: 3 + 7 + 7 = 17.
     */

    final int COORD_BITS = 7;

    public LocationFlag() {
        super();
        setSchema(LOCATION_SCHEMA);
    }

    public LocationFlag(boolean withoutSchema) {
        super();
    }

    public LocationFlag(MapLocation loc) {
        super();
        setSchema(LOCATION_SCHEMA);
        writeLocation(loc);
    }

    /**
     * Call this when you know the int you just received
     * represents a LocationFlag.
     */
    public LocationFlag(int received) {
        super(received);
    }

    /**
     * Encode a location modulo 128.
     * @param loc Location to be encoded
     * @return Whether the write was successful.
     */
    public boolean writeLocation(MapLocation loc) {
        return writeToFlag(loc.x & 127, COORD_BITS) && writeToFlag(loc.y & 127, COORD_BITS);
    }

    /**
     * Write the mod 128 version of your x-coordinate, y-coordinate
     */
    public boolean writeLocation(int x, int y) {
        return writeToFlag(x & 127, COORD_BITS) && writeToFlag(y & 127, COORD_BITS);
    }

    /**
     * Returns an array of two ints representing
     * [location.x % 128, location.y % 128]
     */
    public int[] readLocation() {
        return new int[]{ readFromFlag(SCHEMA_BITS, COORD_BITS), readFromFlag(SCHEMA_BITS + COORD_BITS, COORD_BITS) };
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
        int xRel = readFromFlag(SCHEMA_BITS, COORD_BITS) - (myLoc.x & 127);
        if (xRel > 64) {
            xRel -= 128;
        } else if (xRel < -64) {
            xRel += 128;
        }
        int yRel = readFromFlag(SCHEMA_BITS + COORD_BITS, COORD_BITS) - (myLoc.y & 127);
        if (yRel > 64) {
            yRel -= 128;
        } else if (yRel < -64) {
            yRel += 128;
        }
        return new int[]{ xRel, yRel };
    }

    /**
     * Get the absolute location encoded in this flag.
     * @param myLoc Location of the robot reading this flag.
     * @return Absolute location encoded in this flag.
     */
    public MapLocation readAbsoluteLocation(MapLocation myLoc) {
        int[] relLoc = readRelativeLocationFrom(myLoc);
        return new MapLocation(myLoc.x + relLoc[0], myLoc.y + relLoc[1]);
    }
}
