package flags;

import static org.junit.Assert.*;
import org.junit.Test;
import battlecode.common.*;
import scoutplayer.Flag;
import scoutplayer.SpawnUnitFlag;
import scoutplayer.LocationFlag;
import scoutplayer.SpawnDestinationFlag;
import scoutplayer.UnitUpdateFlag;

public class SpecificFlagTests {

	@Test
	public void testSpawnUnitFlag() {
        SpawnUnitFlag suf = new SpawnUnitFlag(RobotType.POLITICIAN, Direction.EAST, 19344);
        assertEquals(Flag.SPAWN_UNIT_SCHEMA, suf.getSchema());
        assertEquals(RobotType.POLITICIAN, suf.readUnitType());
        assertEquals(Direction.EAST, suf.readSpawnDir());
        assertEquals(19344, suf.readID());
    }

    @Test
	public void testLocationFlag() {
        LocationFlag lf = new LocationFlag(new MapLocation(23847, 22947));
        assertEquals(lf.readLocation()[0], 23847 & 127);
        assertEquals(lf.readLocation()[1], 22947 & 127);
        assertTrue(lf.getFlag() <= GameConstants.MAX_FLAG_VALUE);
    }

    @Test
    public void testSpawnDestinationFlag() {
        SpawnDestinationFlag sdf = new SpawnDestinationFlag(new MapLocation(23847, 22931), SpawnDestinationFlag.INSTR_SCOUT);
        assertEquals(Flag.SPAWN_DESTINATION_SCHEMA, sdf.getSchema());
        assertEquals(SpawnDestinationFlag.INSTR_SCOUT, sdf.readInstruction());
    }

    @Test
    public void testUnitUpdateFlag() {
        UnitUpdateFlag uuf = new UnitUpdateFlag(13409113);
        assertEquals(false, uuf.readIsSlanderer());
    }
}
