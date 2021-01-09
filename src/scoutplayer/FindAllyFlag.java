package scoutplayer;

/**
 * Flag that ECs use to broadcast a secret code to allies
 * at the start of the game, and to verify the secret codes
 * that they see from other robots.
 */
public class FindAllyFlag extends Flag {
    /**
     * Flag breakdown:
     * - secret code (CODE_BITS)
     * - x-coordinate (COORD_BITS)
     * - y-coordinate (COORD_BITS)
     * Total bits used: 10 + 7 + 7 = 24.
     * NOTE: THIS IS THE ONLY FLAG CLASS THAT DOES
     * NOT USE A SCHEMA.
     */

    final int CODE_BITS = 10;
    final int COORD_BITS = 7;

    public FindAllyFlag() {
        super();
    }

    /* Call this when you know the int you just received
     * represents a FindAllyFlag.
     */
    public FindAllyFlag(int received) {
        super(received);
    }

    public boolean writeCode(int code) {
        return writeToFlag(code, CODE_BITS);
    }

    /*
     * location mod 128
     */
    public boolean writeLocation(int x, int y) {
        return writeToFlag(x, COORD_BITS) && writeToFlag(y, COORD_BITS);
    }

    public int readCode() {
        return readFromFlag(0, CODE_BITS);
    }

    public int[] readLocation() {
        return new int[]{readFromFlag(CODE_BITS, COORD_BITS), readFromFlag(CODE_BITS + COORD_BITS, COORD_BITS)};
    }
}
