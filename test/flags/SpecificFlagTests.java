package flags;

import static org.junit.Assert.*;
import org.junit.Test;
import battlecode.common.*;
import fdr.ECSightingFlag;
import fdr.Flag;
import fdr.SpawnUnitFlag;
import fdr.LocationFlag;
import fdr.SpawnDestinationFlag;
import fdr.UnitUpdateFlag;

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
        SpawnDestinationFlag sdf = new SpawnDestinationFlag(new MapLocation(23847, 22931), SpawnDestinationFlag.INSTR_SCOUT, true);
        assertEquals(Flag.SPAWN_DESTINATION_SCHEMA, sdf.getSchema());
        assertEquals(SpawnDestinationFlag.INSTR_SCOUT, sdf.readInstruction());
		assertEquals(true, sdf.readGuess());
    }

    @Test
    public void testUnitUpdateFlag() {
        UnitUpdateFlag uuf = new UnitUpdateFlag(13409113);
        assertEquals(false, uuf.readIsSlanderer());
    }

    @Test
    public void testECSightingFlag() {
        ECSightingFlag ecsf = new ECSightingFlag(3, 4, ECSightingFlag.NEUTRAL_EC, Direction.NORTH, 100);
        assertEquals(98, ecsf.readECInfluence());
        ECSightingFlag ecsf2 = new ECSightingFlag(3, 4, ECSightingFlag.NEUTRAL_EC, Direction.NORTH, 500);
        assertEquals(469, ecsf2.readECInfluence());
        ECSightingFlag ecsf3 = new ECSightingFlag(3, 4, ECSightingFlag.NEUTRAL_EC, Direction.NORTH, Integer.MAX_VALUE);
        assertEquals(1969251187, ecsf3.readECInfluence());
    }
}
