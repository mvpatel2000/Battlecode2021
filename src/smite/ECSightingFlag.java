package smite;

import battlecode.common.*;

/**
 * Flag that robots can use to communicate the location of a non-ally EC.
 */
public class ECSightingFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - relative x coordinate (REL_COORD_BITS)
     * - relative y coordinate (REL_COORD_BITS)
     * - type of EC that was sighted (EC_TYPE_BITS)
     * - the scouting unit's last move (LAST_MOVE_BITS)
     * - the approximate influence of the EC (EC_INF_BITS)
     *
     * Total bits used: 3 + 4 + 4 + 2 + 4 + 7 = 24.
     */

    final int REL_COORD_BITS = 4;
    final int EC_TYPE_BITS = 2;
    final int LAST_MOVE_BITS = 4;
    final int EC_INF_BITS = 7;

    public static final int NEUTRAL_EC = 0;
    public static final int ENEMY_EC = 1;
    public static final int ALLY_EC = 2;

    public ECSightingFlag() {
        super();
        setSchema(EC_SIGHTING_SCHEMA);
    }

    public ECSightingFlag(int xRel, int yRel, int ecType, Direction lastMove, int inf) {
        super();
        setSchema(EC_SIGHTING_SCHEMA);
        writeRelECLocation(xRel, yRel);
        writeECType(ecType);
        writeLastMove(lastMove);
        writeECInfluence(inf);
    }

    public ECSightingFlag(int received) {
        super(received);
    }

    /**
     * Write the location of the spotted EC relative to the
     * reporting unit after its move.
     */
    public boolean writeRelECLocation(int xRel, int yRel) {
        return writeToFlag(xRel + 7, REL_COORD_BITS) && writeToFlag(yRel + 7, REL_COORD_BITS);
    }

    public int[] readRelECLocation() {
        return new int[] { readFromFlag(SCHEMA_BITS, REL_COORD_BITS) - 7, readFromFlag(SCHEMA_BITS + REL_COORD_BITS, REL_COORD_BITS) - 7 };
    }

    /**
     * Given the location of the reporting unit after its move, where
     * is the EC location that is being reported?
     */
    public MapLocation readRelECLocationFrom(MapLocation unitLoc) {
        int[] relLoc = readRelECLocation();
        return unitLoc.translate(relLoc[0], relLoc[1]);
    }

    public boolean writeECType(int ecType) {
        return writeToFlag(ecType, EC_TYPE_BITS);
    }

    public int readECType() {
        return readFromFlag(SCHEMA_BITS + REL_COORD_BITS + REL_COORD_BITS, EC_TYPE_BITS);
    }

    public boolean writeLastMove(int lm) {
        return writeToFlag(lm, LAST_MOVE_BITS);
    }

    public boolean writeLastMove(Direction lm) {
        return writeLastMove(Robot.directionToInt(lm));
    }

    public Direction readLastMove() {
        return Robot.allDirections[readFromFlag(SCHEMA_BITS + REL_COORD_BITS + REL_COORD_BITS + EC_TYPE_BITS, LAST_MOVE_BITS)];
    }

    // TODO: optimize log2?
    public boolean writeECInfluence(int influence) {
        return writeToFlag(1+(int)(3.5*Math.log(influence) / 0.69314718056), EC_INF_BITS);
    }

    public int readECInfluence() {
        return (int) Math.pow(2, (readFromFlag(SCHEMA_BITS + REL_COORD_BITS + REL_COORD_BITS + EC_TYPE_BITS + LAST_MOVE_BITS, EC_INF_BITS) - 0.5)/3.5);
    }
}
