package dprs.entity;

import org.junit.Test;

import java.util.HashSet;

import static dprs.entity.VectorClock.fromJSON;
import static dprs.entity.VectorClock.mergeToNewer;
import static org.junit.Assert.*;
//import static org.junit.Assert.assertNotEquals;


public class VectorClockTest {

    VectorClock nonConcurentOlderVectorClock;
    VectorClock nonConcurentNewerVectorClock;

    VectorClock concurentVectorClock1;
    VectorClock concurentVectorClock2;


    /*
     * a|-> Maju vsetky alebo niektore kompoenty rovnake
         * c| Co ak nemaju ziadne kompoenty spolocne ?
         *      - neda sa povedat ci su konkurentne
    */
    private void initNonconcurent(){

        nonConcurentOlderVectorClock = new VectorClock();
        nonConcurentNewerVectorClock = new VectorClock();

        nonConcurentOlderVectorClock.setValueForComponent(1, 1);
        nonConcurentOlderVectorClock.setValueForComponent(2, 5);
        nonConcurentOlderVectorClock.setValueForComponent(3, 2);


        nonConcurentNewerVectorClock.setValueForComponent(1, 2);
        nonConcurentNewerVectorClock.setValueForComponent(2, 5);
        nonConcurentNewerVectorClock.setValueForComponent(3, 2);
        nonConcurentNewerVectorClock.setValueForComponent(4, 1);

    }
    
    private void initConcurent(){
        concurentVectorClock1 = new VectorClock();
        concurentVectorClock2 = new VectorClock();

        concurentVectorClock1.setValueForComponent(1, 2);
        concurentVectorClock1.setValueForComponent(2, 5);
        concurentVectorClock1.setValueForComponent(3, 2);
        concurentVectorClock1.setValueForComponent(4, 1);

        concurentVectorClock2.setValueForComponent(1, 2);
        concurentVectorClock2.setValueForComponent(2, 6);
        concurentVectorClock2.setValueForComponent(3, 1);
    }



    @Test
    public void testNonConcurentMerge() throws Exception{
        initNonconcurent();

        final HashSet<VectorClock> objects = new HashSet<>();
        objects.add(nonConcurentOlderVectorClock);
        objects.add(nonConcurentNewerVectorClock);

        final VectorClock resultingVectorClock = mergeToNewer(objects);

        VectorClock expectedVectorClock = new VectorClock();
        expectedVectorClock.setValueForComponent(2, 6);
        expectedVectorClock.setValueForComponent(3, 3);
        expectedVectorClock.setValueForComponent(1, 3);
        expectedVectorClock.setValueForComponent(4, 2);

        assertEquals(resultingVectorClock,expectedVectorClock);
        assertTrue(expectedVectorClock.isThisNewerThan(nonConcurentNewerVectorClock));
        assertTrue(expectedVectorClock.isThisNewerThan(nonConcurentOlderVectorClock));
        assertNotEquals(expectedVectorClock,nonConcurentNewerVectorClock);
        assertNotEquals(expectedVectorClock,nonConcurentOlderVectorClock);
    }

    @Test
    public void testNonconcurency(){
        initNonconcurent();
        assertNotEquals(nonConcurentNewerVectorClock, nonConcurentOlderVectorClock);
        assertTrue(nonConcurentNewerVectorClock.isThisNewerThan(nonConcurentOlderVectorClock));
    }

    @Test
    public void testConcurency(){
        initConcurent();
        assertTrue(concurentVectorClock1.isThisConcurentTo(concurentVectorClock2));
        assertNotEquals(concurentVectorClock1, concurentVectorClock2);
    }


    @Test
    public void testJSON() throws Exception {
        VectorClock vk = new VectorClock();
        vk.setValueForComponent(-20, 0);
        vk.setValueForComponent(1, 0);
        vk.setValueForComponent(2, 0);

        final VectorClock deserializedClock = fromJSON(vk.toJSON());

        assertEquals(vk, deserializedClock);
    }

    @Test
    public void testIncrement() throws Exception {
        initNonconcurent();
        nonConcurentOlderVectorClock.incrementValueForComponent(1);
        nonConcurentOlderVectorClock.incrementValueForComponent(3);

        VectorClock expectedVectorClock = new VectorClock();
        expectedVectorClock.setValueForComponent(1, 2);
        expectedVectorClock.setValueForComponent(2, 5);
        expectedVectorClock.setValueForComponent(3, 3);

        assertEquals(nonConcurentOlderVectorClock, expectedVectorClock);

    }


    @Test
    public void testConflictedComparison(){
        VectorClock vc1 = new VectorClock();
        vc1.setValueForComponent(1, 4);

        VectorClock vc2 = new VectorClock();
        vc2.setValueForComponent(2,3);

        assert !vc1.isThisNewerThan(vc2);
    }

}