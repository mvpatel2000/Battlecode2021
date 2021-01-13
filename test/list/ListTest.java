package list;

import static org.junit.Assert.*;
import org.junit.Test;
import scoutplayer.List;

public class ListTest {

	@Test
    public void testList() {
        List<Integer> L = new List<Integer>();
        L.add(1);
        L.add(2);
        L.add(3);
        L.resetIter();
        assertEquals((Integer) 3, L.curr());
        assertEquals((Integer) 3, L.popStep());
        assertEquals((Integer) 2, L.step());
        assertEquals((Integer) 1, L.curr());
    }
}
