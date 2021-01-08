package scoutplayer;

/**
 * Flag that ECs use to broadcast a secret code to allies
 * at the start of the game, and to verify the secret codes
 * that they see from other robots.
 */
public class FindAllyFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - secret code (CODE_BITS)
     * 
     * Total bits used: 3 + 21 = 24.
     */

    final int CODE_BITS = 21;

    public FindAllyFlag() {
        super();
        setSchema(FIND_ALLY_SCHEMA);
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

    public int readCode() {
        return readFromFlag(SCHEMA_BITS, CODE_BITS);
    }
}
