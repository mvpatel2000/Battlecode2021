package scouting;

import battlecode.common.*;
import static org.junit.Assert.*;
import org.junit.Test;
import scoutplayer.MapTerrainQueue;

public class MapTerrainTest {

	@Test
	public void testOneStep() throws GameActionException {
        MapTerrainQueue mtq = new MapTerrainQueue();
        mtq.step(Direction.NORTHWEST, new MapLocation(1,2));
        MapLocation loc = mtq.pop();
        assertEquals(1, loc.x);
        assertEquals(6, loc.y);
    }
    
    @Test
	public void testOverflow() throws GameActionException {
        MapTerrainQueue mtq = new MapTerrainQueue();
        mtq.step(Direction.NORTHEAST, new MapLocation(1,2));
        assertEquals(15, mtq.getSize());
        // assertEquals(0, mtq.start);
        mtq.step(Direction.NORTHEAST, new MapLocation(2,3));
        assertEquals(30, mtq.getSize());
        // assertEquals(0, mtq.start);
        mtq.step(Direction.NORTHEAST, new MapLocation(3,4));
        assertEquals(45, mtq.getSize());
        // assertEquals(0, mtq.start);
        mtq.step(Direction.NORTHEAST, new MapLocation(4,5));
        assertEquals(50, mtq.getSize());
        // assertEquals(10, mtq.start);
        MapLocation loc = mtq.pop();
        assertEquals(4, loc.x);
        assertEquals(6, loc.y);
    }
}
