package map;

import static org.junit.Assert.*;
import org.junit.Test;
import scoutplayer.RelativeMap;
import battlecode.common.*;

public class MapTest {

	@Test
    public void testWallBounds() {
        RelativeMap map = new RelativeMap(new MapLocation(100, 100));
        map.set(30, 20, RelativeMap.ALLY_EC);
        assertEquals(64, map.xLineAboveUpper);
        assertEquals(-34, map.xLineBelowLower);
        assertEquals(64, map.yLineAboveUpper);
        assertEquals(-44, map.yLineBelowLower);
        map.set(1, -6, 0);
        assertEquals(64, map.xLineAboveUpper);
        assertEquals(-34, map.xLineBelowLower);
        assertEquals(64, map.yLineAboveUpper);
        assertEquals(-6, map.yLineBelowLower);
        map.set(40, 20, 0);
        assertEquals(40, map.xLineAboveUpper);
        assertEquals(-34, map.xLineBelowLower);
        assertEquals(64, map.yLineAboveUpper);
        assertEquals(-6, map.yLineBelowLower);
    }
}
