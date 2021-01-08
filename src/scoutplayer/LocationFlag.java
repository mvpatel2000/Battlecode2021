package scoutplayer;

public class LocationFlag extends Flag {

    final int NUM_COORD_BITS = 7;

    public LocationFlag() {
        super();
        setSchema(LOCATION_SCHEMA);
    }

    /* Call this when you know the int you just received
     * represents a FindAllyFlag.
     */
    public LocationFlag(int received) {
        super(received);
        writtenTo = 24;
    }

    /*
     * Write the mod 128 version of your x-coordinate, y-coordinate
     */
    public boolean writeLocation(int x, int y) {
        return writeToFlag(x, NUM_COORD_BITS) && writeToFlag(y, NUM_COORD_BITS);
    }

    /* Returns an array of two ints representing
     * [location.x % 128, location.y % 128]
     */
    public int[] readLocation() {
        return new int[]{readFromFlag(3, NUM_COORD_BITS), readFromFlag(3+NUM_COORD_BITS, NUM_COORD_BITS)};
    }
}
