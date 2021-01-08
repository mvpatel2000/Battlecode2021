package scoutplayer;

public class FindAllyFlag extends Flag {

    final int NUM_CODE_BITS = 21;

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
}
