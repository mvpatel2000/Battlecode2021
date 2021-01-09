package scoutplayer;

import battlecode.common.*;

public class SpawnDestinationFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x coordinate mod 128 (COORD_BITS)
     * - y coordinate mod 128 (COORD_BITS)
     * - instruction schema (INSTRUCTION_SCHEMA_BITS)
     * 
     * Total bits used: 3 + 7 + 7 + 3 = 20.
     */
    
    final int INSTRUCTION_SCHEMA_BITS = 3;

    public static final int INSTR_SCOUT = 0;
    public static final int INSTR_ATTACK = 1;

    public SpawnDestinationFlag() {
        super();
    }

    public SpawnDestinationFlag(MapLocation loc, int instrSchema) {
        super(loc);
        writeInstructionSchema(instrSchema);
    }

    public SpawnDestinationFlag(int received) {
        super(received);
    }

    public boolean writeInstructionSchema(int instrSchema) {
        return writeToFlag(instrSchema, INSTRUCTION_SCHEMA_BITS);
    }

    public int readInstructionSchema() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, INSTRUCTION_SCHEMA_BITS);
    }
}
