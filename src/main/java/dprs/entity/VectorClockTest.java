package dprs.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by igordanis on 16/05/16.
 */
public class VectorClockTest {

    VectorClock vectorClock;

    @Before
    public void initBeforeEachTest(){
        vectorClock = new VectorClock();
    }

    @Test
    public void testIncrementValueForComponent() throws Exception {
        //todo
    }

    @Test
    public void testIsThisNewerThan() throws Exception {

        VectorClock oldVK = new VectorClock();
        oldVK.setValueForComponent(0, 0);
        oldVK.setValueForComponent(1, 0);
        oldVK.setValueForComponent(2, 0);

        VectorClock newVK = new VectorClock();
        newVK.setValueForComponent(0, 1);
        newVK.setValueForComponent(1, 1);
        newVK.setValueForComponent(2, 1);

        assertTrue(newVK.isThisNewerThan(oldVK, 0));
        assertTrue(newVK.isThisNewerThan(oldVK, 1));
        assertTrue(newVK.isThisNewerThan(oldVK, 2));

        assertFalse(oldVK.isThisNewerThan(newVK, 0));
        assertFalse(oldVK.isThisNewerThan(newVK, 1));
        assertFalse(oldVK.isThisNewerThan(newVK, 2));

    }


    @Test
    public void testJSON() throws Exception {
        VectorClock vk = new VectorClock();
        vk.setValueForComponent(0, 0);
        vk.setValueForComponent(1, 0);
        vk.setValueForComponent(2, 0);

        final VectorClock deserializedClock = VectorClock.fromJSON(vk.toJSON());

        assertFalse(deserializedClock.isThisNewerThan(vk,0));
    }


}