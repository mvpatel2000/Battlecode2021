package flags;

import static org.junit.Assert.*;
import org.junit.Test;
import scoutplayer.Flag;

public class FlagReadWriteTest {

	@Test
	public void testNewFlag() {
        Flag flag = new Flag();
        flag.setSchema(0);
        assertEquals(flag.getFlag(), 0);
	}

    @Test
    public void testFlagSchema() {
        Flag flag = new Flag();
        flag.setSchema(1);
        assertEquals(flag.getSchema(), 1);
    }

    @Test
    public void testFlagContents() {
        Flag flag = new Flag();
        assertTrue(flag.setSchema(3));
        assertTrue(flag.writeToFlag(15, 5));
        assertTrue(flag.writeToFlag(20, 5));
        assertEquals(flag.readFromFlag(3, 5), 15);
        assertEquals(flag.readFromFlag(8, 5), 20);
    }

    @Test
    public void testFlagOverload() {
        Flag flag = new Flag();
        assertTrue(flag.setSchema(4));
        assertTrue(flag.writeToFlag(1000, 18));
        assertFalse(flag.writeToFlag(4, 4));
        assertTrue(flag.writeToFlag(4, 3));
    }
}
