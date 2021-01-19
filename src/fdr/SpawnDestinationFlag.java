package fdr;

import battlecode.common.*;

/**
 * Flag used by ECs to communicate destinations to newly spawned units.
 */
public class SpawnDestinationFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x coordinate mod 128 (COORD_BITS)
     * - y coordinate mod 128 (COORD_BITS)
     * - instruction (INSTRUCTION_BITS)
     * - guess (1) or exact (0) (INSTRUCTION_BITS)
     *
     * Total bits used: 3 + 7 + 7 + 3 = 20.
     */

    final int INSTRUCTION_BITS = 3;
    final int GUESS_BITS = 1;

    public static final int INSTR_SCOUT = 0;
    public static final int INSTR_ATTACK = 1;
    public static final int INSTR_SLANDERER = 2;

    public SpawnDestinationFlag() {
        super(true); // call the LocationFlag constructor that doesn't set the schema
        setSchema(SPAWN_DESTINATION_SCHEMA);
    }

    public SpawnDestinationFlag(MapLocation loc, int instrSchema, boolean guess) {
        super(true); // call the LocationFlag constructor that doesn't set the schema
        setSchema(SPAWN_DESTINATION_SCHEMA);
        writeLocation(loc);
        writeInstruction(instrSchema);
        writeGuess(guess);
    }

    public SpawnDestinationFlag(int received) {
        super(received);
    }

    public boolean writeInstruction(int instrSchema) {
        return writeToFlag(instrSchema, INSTRUCTION_BITS);
    }

    public boolean writeGuess(boolean guess) {
        if (guess) {
            return writeToFlag(1, GUESS_BITS);
        } else {
            return writeToFlag(0, GUESS_BITS);
        }
    }

    public int readInstruction() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, INSTRUCTION_BITS);
    }

    public boolean readGuess() {
        int guess = readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + INSTRUCTION_BITS, GUESS_BITS);
        if (guess == 1) {
            return true;
        } else {
            return false;
        }
    }
}
