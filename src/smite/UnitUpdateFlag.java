package smite;

import battlecode.common.*;

/**
 * Flag used to tell allies about what's going on around me. Includes
 * last move for EC UnitTrackers, info on whether I'm a slanderer for
 * friendly units who can't tell the difference, and where the nearest
 * enemy is to me. If no nearby enemy is found, then the hasEnemyInfo()
 * function will return false, the read enemy location functions will
 * return [0, 0] for relative and the flagbearer's location for absolute.
 */
public class UnitUpdateFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - isSlanderer (IS_SLANDERER_BITS)
     * - x mod 128 (COORD_BITS)
     * - y mod 128 (COORD_BITS)
     * - nearest enemy type (ENEMY_TYPE_BITS)
     * 
     * Total bits used: 3 + 1 + 7 + 7 + 2 = 20.
     */

    final int IS_SLANDERER_BITS = 1;
    final int COORD_BITS = 7;
    final int ENEMY_TYPE_BITS = 2;

    public UnitUpdateFlag() {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
    }
    
    public UnitUpdateFlag(boolean isSlanderer) {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
        writeIsSlanderer(isSlanderer);
    }

    public UnitUpdateFlag(boolean isSlanderer, MapLocation enemyLoc, RobotType enemyType) {
        super();
        setSchema(UNIT_UPDATE_SCHEMA);
        writeIsSlanderer(isSlanderer);
        writeEnemyLocation(enemyLoc);
        writeEnemyType(enemyType);
    }

    public UnitUpdateFlag(int flag) {
        super(flag);
    }
    public boolean readIsSlanderer() {
        return readFromFlag(SCHEMA_BITS, IS_SLANDERER_BITS) == 1;
    }

    public boolean writeIsSlanderer(boolean isSlanderer) {
        return writeToFlag(isSlanderer ? 1 : 0, IS_SLANDERER_BITS);
    }

    public boolean hasEnemyInfo(MapLocation flagbearerLoc) {
        int[] relCoords = readRelativeEnemyLocationFrom(flagbearerLoc);
        return (relCoords[0] != 0 || relCoords[1] != 0);
    }

    /**
     * Encode a location modulo 128.
     * @param loc Location to be encoded
     * @return Whether the write was successful.
     */
    public boolean writeEnemyLocation(MapLocation loc) {
        return writeToFlag(loc.x & 127, COORD_BITS) && writeToFlag(loc.y & 127, COORD_BITS);
    }

    /**
     * Write the mod 128 version of your x-coordinate, y-coordinate
     */
    public boolean writeEnemyLocation(int x, int y) {
        return writeToFlag(x & 127, COORD_BITS) && writeToFlag(y & 127, COORD_BITS);
    }

    /**
     * Returns an array of two ints representing
     * [location.x % 128, location.y % 128]
     */
    public int[] readEnemyLocation() {
        return new int[]{ readFromFlag(SCHEMA_BITS + IS_SLANDERER_BITS, COORD_BITS), readFromFlag(SCHEMA_BITS + IS_SLANDERER_BITS + COORD_BITS, COORD_BITS) };
    }

    /**
     * Get the vector from the robot reading this flag to the location
     * encoded in the flag.
     * @param myLoc Location of the robot reading the flag.
     * @return [xRel, yRel] vector that lies inside the map from the
     * robot reading the location to the location in this flag.
     */
    public int[] readRelativeEnemyLocationFrom(MapLocation myLoc) {
        // extract relative x and y s.t. both lie within the map
        int xRel = readFromFlag(SCHEMA_BITS + IS_SLANDERER_BITS, COORD_BITS) - (myLoc.x & 127);
        if (xRel > 64) {
            xRel -= 128;
        } else if (xRel < -64) {
            xRel += 128;
        }
        int yRel = readFromFlag(SCHEMA_BITS + IS_SLANDERER_BITS + COORD_BITS, COORD_BITS) - (myLoc.y & 127);
        if (yRel > 64) {
            yRel -= 128;
        } else if (yRel < -64) {
            yRel += 128;
        }
        return new int[]{ xRel, yRel };
    }

    /**
     * Get the absolute location encoded in this flag.
     * @param myLoc Location of the robot reading this flag.
     * @return Absolute location encoded in this flag. Returns null
     * if no enemy is known.
     */
    public MapLocation readAbsoluteEnemyLocation(MapLocation myLoc) {
        int[] relLoc = readRelativeEnemyLocationFrom(myLoc);
        if (relLoc[0] == 0 && relLoc[1] == 0) return null;
        return new MapLocation(myLoc.x + relLoc[0], myLoc.y + relLoc[1]);
    }

    public boolean writeEnemyType(RobotType type) {
        switch (type) {
            case POLITICIAN:
                return writeToFlag(1, ENEMY_TYPE_BITS);
            case SLANDERER:
                return writeToFlag(2, ENEMY_TYPE_BITS);
            case MUCKRAKER:
                return writeToFlag(3, ENEMY_TYPE_BITS);
            default:
                return writeToFlag(0, ENEMY_TYPE_BITS);
        }
    }

    public RobotType readEnemyType() {
        switch (readFromFlag(SCHEMA_BITS + IS_SLANDERER_BITS + COORD_BITS + COORD_BITS, ENEMY_TYPE_BITS)) {
            case 1:
                return RobotType.POLITICIAN;
            case 2:
                return RobotType.SLANDERER;
            case 3:
                return RobotType.MUCKRAKER;
            default:
                return RobotType.ENLIGHTENMENT_CENTER;
        }
    }

}
