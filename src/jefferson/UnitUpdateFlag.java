package jefferson;

import battlecode.common.*;

/**
 * Flag used to tell allies about what's going on around me. Includes
 * info on whether I'm a slanderer for friendly units who can't tell the
 * difference and where the nearest enemy is to me. If no nearby enemy
 * is found, then @TODO: Mihir
 */
public class UnitUpdateFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x mod 128 (COORD_BITS)
     * - y mod 128 (COORD_BITS)
     * - isSlanderer (IS_SLANDERER_BITS)
     * - isDefender (IS_DEFENDER_BITS)
     * - nearest enemy type (ENEMY_TYPE_BITS)
     * - has nearby enemy (HAS_NEARBY_ENEMY_BITS)
     * 
     * Total bits used: 3 + 7 + 7 + 1 + 1 + 2 + 1 = 22.
     */

    final int IS_SLANDERER_BITS = 1;
    final int IS_DEFENDER_BITS = 1;
    final int ENEMY_TYPE_BITS = 2;
    final int HAS_NEARBY_ENEMY_BITS = 1;

    public UnitUpdateFlag() {
        super(true);
        setSchema(UNIT_UPDATE_SCHEMA);
    }

    public UnitUpdateFlag(boolean isSlanderer, boolean isDefender) {
        super(true);
        setSchema(UNIT_UPDATE_SCHEMA);
        writeLocation(0, 0);
        writeIsSlanderer(isSlanderer);
        writeIsDefender(isDefender);
        writeEnemyType(RobotType.ENLIGHTENMENT_CENTER);
        writeHasNearbyEnemy(false);
    }

    public UnitUpdateFlag(boolean isSlanderer, boolean isDefender, MapLocation enemyLoc, RobotType enemyType) {
        super(true);
        setSchema(UNIT_UPDATE_SCHEMA);
        if (enemyLoc == null) {
            writeLocation(0, 0);
            writeIsSlanderer(isSlanderer);
            writeIsDefender(isDefender);
            writeEnemyType(RobotType.ENLIGHTENMENT_CENTER);
            writeHasNearbyEnemy(false);
        } else {
            writeLocation(enemyLoc);
            writeIsSlanderer(isSlanderer);
            writeIsDefender(isDefender);
            writeEnemyType(enemyType);
            writeHasNearbyEnemy(true);
        }
    }

    public UnitUpdateFlag(int flag) {
        super(flag);
    }

    public boolean readIsSlanderer() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, IS_SLANDERER_BITS) == 1;
    }

    public boolean writeIsSlanderer(boolean isSlanderer) {
        return writeToFlag(isSlanderer ? 1 : 0, IS_SLANDERER_BITS);
    }

    public boolean readIsDefender() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + IS_SLANDERER_BITS, IS_DEFENDER_BITS) == 1;
    }

    public boolean writeIsDefender(boolean isDefender) {
        return writeToFlag(isDefender ? 1 : 0, IS_DEFENDER_BITS);
    }

    public boolean writeEnemyType(RobotType type) {
        return writeToFlag(Robot.typeToInt(type), ENEMY_TYPE_BITS);
    }

    public RobotType readEnemyType() {
        return Robot.robotTypes[readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + IS_SLANDERER_BITS + IS_DEFENDER_BITS, ENEMY_TYPE_BITS)];
    }

    public boolean readHasNearbyEnemy() {
        return readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS + IS_SLANDERER_BITS + IS_DEFENDER_BITS + ENEMY_TYPE_BITS, HAS_NEARBY_ENEMY_BITS) == 1;
    }

    public boolean writeHasNearbyEnemy(boolean hasNearbyEnemy) {
        return writeToFlag(hasNearbyEnemy ? 1 : 0, HAS_NEARBY_ENEMY_BITS);
    }

}
