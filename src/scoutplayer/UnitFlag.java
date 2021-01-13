package scoutplayer;

import battlecode.common.*;

public class UnitFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - last move (LAST_MOVE_BITS)
     * - isSlanderer (IS_SLANDERER)
     * 
     * Total bits used: 3 + 4 = 7.
     */

    final int LAST_MOVE_BITS = 4;
    final int IS_SLANDERER = 1;

    public UnitFlag() {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
    }

    public UnitFlag(Direction lastMove) {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
        writeLastMove(lastMove);
    }

    public UnitFlag(Direction lastMove, boolean isSlanderer) {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
        writeLastMove(lastMove);
        writeIsSlanderer(isSlanderer ? 1 : 0);
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

    public boolean readIsSlanderer() {
        return readFromFlag(SCHEMA_BITS + LAST_MOVE_BITS, IS_SLANDERER) == 1;
    }

    public boolean writeIsSlanderer(int isSlanderer) {
        return writeToFlag(isSlanderer, IS_SLANDERER);
    }

}
