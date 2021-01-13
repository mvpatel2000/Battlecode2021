package smite;

import battlecode.common.*;

public class UnitFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - last move (LAST_MOVE_BITS)
     * 
     * Total bits used: 3 + 4 = 7.
     */

    final int LAST_MOVE_BITS = 4;

    public UnitFlag() {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
    }

    public UnitFlag(Direction lastMove) {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
        writeLastMove(lastMove);
    }

    public UnitFlag(int flag) {
        super(flag);
    }

    public Direction readLastMove() {
        return Robot.allDirections[readFromFlag(SCHEMA_BITS, LAST_MOVE_BITS)];
    }

    public boolean writeLastMove(int lm) {
        return writeToFlag(lm, LAST_MOVE_BITS);
    }

    public boolean writeLastMove(Direction lm) {
        return writeLastMove(Robot.directionToInt(lm));
    }

}
