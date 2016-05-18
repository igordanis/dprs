package dprs.entity;

import org.junit.Test;

import java.util.HashSet;

import static dprs.entity.VectorClock.fromJSON;
import static dprs.entity.VectorClock.mergeToNewer;
import static org.junit.Assert.assertEquals;


public class VectorClockTest {

    VectorClock nonConcurentOlderVectorClock;
    VectorClock nonConcurentNewerVectorClock;

    VectorClock concurentVectorClock1;
    VectorClock concurentVectorClock2;


    /*
     * a|-> Maju vsetky vsetky kompoenty rovnake
         * b| Co ak maju iba niektore kompoenty spolocne ? TODO
         * c| Co ak nemaju ziadne kompoenty spolocne ? TODO

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

    }



    @Test
    public void testNonConcurentMerge() throws Exception{
        initNonconcurent();

        final HashSet<VectorClock> objects = new HashSet<>();
        objects.add(nonConcurentOlderVectorClock);
        objects.add(nonConcurentNewerVectorClock);

        final VectorClock resultingVectorClock = mergeToNewer(objects);

        final VectorClock expectedVectorClock = new VectorClock();
        expectedVectorClock.setValueForComponent(1, 3);
        expectedVectorClock.setValueForComponent(1, 6);
        expectedVectorClock.setValueForComponent(1, 3);

        assertEquals(resultingVectorClock, expectedVectorClock);
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
    public void testIncrement() throws Exception {}
}