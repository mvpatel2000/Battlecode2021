package trump;

import battlecode.common.*;

/**
 * Flag that ECs use to broadcast to other ECs that they are
 * spawning a unit in a direction with some ID.
 * 
 * The subsequent round, the parent EC will send the unit a
 * DirectionsFlag.
 */
public class SpawnUnitFlag extends Flag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - unit type (UNIT_TYPE_BITS)
     * - spawn direction (SPAWN_DIR_BITS)
     * - unit ID (ID_BITS)
     * 
     * Total bits used: 3 + 2 + 3 + 16 = 24.
     */

    final int UNIT_TYPE_BITS = 2;
    final int SPAWN_DIR_BITS = 3;
    final int ID_BITS = 16;
    final int MIN_ROBOT_ID = 10000;
    
    public SpawnUnitFlag() {
        super();
        setSchema(SPAWN_UNIT_SCHEMA);
    }

    /**
     * Constructor to set up flag with known type, direction, and ID.
     */
    public SpawnUnitFlag(RobotType type, Direction spawnDir, int id) {
        super();
        setSchema(SPAWN_UNIT_SCHEMA);
        writeUnitType(type);
        writeSpawnDir(spawnDir);
        writeID(id);
    }

    /**
     * Use this constructor if you are reading a flag and
     * you already know it is a SpawnUnitFlag.
     */
    public SpawnUnitFlag(int received) {
        super(received);
    }

    public boolean writeUnitType(RobotType type) {
        switch (type) {
            case POLITICIAN:
                return writeToFlag(1, UNIT_TYPE_BITS);
            case SLANDERER:
                return writeToFlag(2, UNIT_TYPE_BITS);
            case MUCKRAKER:
                return writeToFlag(3, UNIT_TYPE_BITS);
            default: // should not be used
                return writeToFlag(0, UNIT_TYPE_BITS);
        }
    }

    public RobotType readUnitType() {
        switch (readFromFlag(SCHEMA_BITS, UNIT_TYPE_BITS)) {
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

    public boolean writeSpawnDir(Direction di) {
        return writeToFlag(Robot.directionToInt(di), SPAWN_DIR_BITS);
    }

    public Direction readSpawnDir() {
        return Robot.directions[readFromFlag(SCHEMA_BITS + UNIT_TYPE_BITS, SPAWN_DIR_BITS)];
    }

    public boolean writeID(int id) {
        return writeToFlag(id - MIN_ROBOT_ID, ID_BITS);
    }

    public int readID() {
        return MIN_ROBOT_ID + readFromFlag(SCHEMA_BITS + UNIT_TYPE_BITS + SPAWN_DIR_BITS, ID_BITS);
    }
}
