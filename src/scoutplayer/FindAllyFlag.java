package scoutplayer;

public class FindAllyFlag extends Flag {

    final int NUM_CODE_BITS = 9;

    public FindAllyFlag() {
        super();
        setSchema(FIND_ALLY_SCHEMA);
    }

    /* Call this when you know the int you just received
     * represents a FindAllyFlag.
     */
    public FindAllyFlag(int received) {
        super(received);
        writtenTo = 24;
    }

    public boolean writeCode(int code) {
        return writeToFlag(code, NUM_CODE_BITS);
    }

    public int readCode() {
        return readFromFlag(3, NUM_CODE_BITS);
    }

    /** TODO: Get these methods working!!!
     * Need to get locations in the form 0-64 so they
     * are in the form so I can write (x, y) in 12 bits.
     */
    public boolean writeLocation(int locx, int locy) {
        return writeToFlag(locx, 6) && writeToFlag(locy, 6);
    }

    public int readLocationX() {
        return readFromFlag(12, 6);
    }

    public int readLocationY() {
        return readFromFlag(18, 6);
    }
}
