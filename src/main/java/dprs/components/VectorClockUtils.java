package dprs.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import dprs.entity.VectorClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin
 */
public class VectorClockUtils {
    private static final Logger logger = LoggerFactory.getLogger(VectorClockUtils.class);
    Map<String, VectorClock> myVectorClocks = new HashMap<String, VectorClock>(); // list of all database entries
    //List vectorClock = new ArrayList(); 
    private static final int replicaNumber = 3; // standard replica number, may be changed if node is lost
    int myId = 0; // TODO obtain my id somewhere

    // all hashes in this node
    Map GetAllClocks() {
        return myVectorClocks;
    }

    // for adding or updating from other nodes
    public void UpdateFromOtherNode(String key, VectorClock value, int from) {
        VectorClock vectorClock = new VectorClock(replicaNumber, myId);
        VectorClock val = myVectorClocks.putIfAbsent(key, vectorClock);
        vectorClock.addVectorFromOtherNode((VectorClock) value.GetVectorClock(), from);
    }

    // for adding or updating from this node - write/update query came to this node
    public void UpdateFromThisNode(String key, int value) {
        VectorClock vectorClock = new VectorClock(replicaNumber, myId);
        VectorClock val = myVectorClocks.putIfAbsent(key, vectorClock);
        vectorClock.addVectorFromThisNode(value);
        // TODO send update request request to other nodes
    }

    // read query from client came at this node
    public ArrayList readFromThisNode(String key) {
        VectorClock v = myVectorClocks.get(key);
        int result = (Integer) v.GetVectorClock().get(myId);
        // TODO search for other nodes
        // TODO foreach node get actual vector (readFromOtherNode)
        // int resultArray = uniq(tempArray); // tempArray needs to be filled with int numbers from other nodes
        //return resultArray; // return or send array
        return null;
    }

    // read query from other client came to other than this node
    public int readFromOtherNode(String key) {
        VectorClock v = myVectorClocks.get(key);
        int result = (Integer) v.GetVectorClock().get(myId);
        return result;
        // TODO send number to requestor node
    }

    public ArrayList uniq(ArrayList Vectors) {
        HashSet hs = new HashSet();
        hs.addAll(Vectors);
        Vectors.clear();
        Vectors.addAll(hs);
        return Vectors;
    }
}
