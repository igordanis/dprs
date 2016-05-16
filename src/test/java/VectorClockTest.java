

import org.junit.Before;
import org.junit.Test;
import dprs.entity.VectorClock;

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

        assertTrue(newVK.isThisEqualOrNewerThan(oldVK, 0));
        assertTrue(newVK.isThisEqualOrNewerThan(oldVK, 1));
        assertTrue(newVK.isThisEqualOrNewerThan(oldVK, 2));

        assertFalse(oldVK.isThisEqualOrNewerThan(newVK, 0));
        assertFalse(oldVK.isThisEqualOrNewerThan(newVK, 1));
        assertFalse(oldVK.isThisEqualOrNewerThan(newVK, 2));

    }


    @Test
    public void testJSON() throws Exception {
        VectorClock vk = new VectorClock();
        vk.setValueForComponent(-20, 0);
        vk.setValueForComponent(1, 0);
        vk.setValueForComponent(2, 0);

        final VectorClock deserializedClock = VectorClock.fromJSON(vk.toJSON());

        assertTrue(deserializedClock.isThisEqualOrNewerThan(vk,0));
    }

     @Test
    public void testIncrement() throws Exception { 
        VectorClock vk = new VectorClock();
        VectorClock oldVk = new VectorClock();
        vk.setValueForComponent(0, 2);
        vk.setValueForComponent(1, 4);
        vk.setValueForComponent(2, 1);
        vk.incrementValueForComponent(1);
        vk.incrementValueForComponent(0);
        vk.incrementValueForComponent(2);
        
        oldVk.setValueForComponent(0, 2);
        oldVk.setValueForComponent(1, 4);
        oldVk.setValueForComponent(2, 3);
        oldVk.incrementValueForComponent(1);
        oldVk.incrementValueForComponent(0);
        oldVk.incrementValueForComponent(2);
        
        assertTrue(oldVk.isThisEqualOrNewerThan(vk,1));
        assertTrue(oldVk.isThisEqualOrNewerThan(vk,0));
        assertTrue(oldVk.isThisEqualOrNewerThan(vk,2));
    }
}