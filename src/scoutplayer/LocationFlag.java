package scoutplayer;

import battlecode.common.*;

/**
 * Flag that any robot can use to send or receive locations.
 */
public class LocationFlag extends Flag {

    final int COORD_BITS = 7;

    public LocationFlag() {
        super();
        setSchema(LOCATION_SCHEMA);
    }

    /**
     * Call this when you know the int you just received
     * represents a FindAllyFlag.
     */
    public LocationFlag(int received) {
        super(received);
    }

    /**
     * Write the mod 128 version of your x-coordinate, y-coordinate
     */
    public boolean writeLocation(int x, int y) {
        return writeToFlag(x, COORD_BITS) && writeToFlag(y, COORD_BITS);
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
