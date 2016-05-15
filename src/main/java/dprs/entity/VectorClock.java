package dprs.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Martin
 */
public class VectorClock {
    Map<Integer, Integer> vectorClock;
    int myNodeId;
    int replicaNumber = 3;

    // constructor for initial setup
    public VectorClock(int number, int nodeId) {
        vectorClock = new HashMap();
        myNodeId = nodeId;
        this.replicaNumber = number;
        for (int i = 0; i < replicaNumber; i++)
            vectorClock.put(i, 0);
    }

    // to change replica number
    public void addReplica() {
        replicaNumber++;
        vectorClock.put(replicaNumber - 1, 0);
    }

    // add or update query for this node
    public void addVectorFromThisNode(int value) {
        vectorClock.put(myNodeId, value);
    }

    // adding or updating from node
    public void addVectorFromOtherNode(VectorClock v, int from) {
        vectorClock.put(from, (Integer) v.GetVectorClock().get(from));
        vectorClock.put(myNodeId, (Integer) v.GetVectorClock().get(from));
    }

    // return VectorClock
    public Map GetVectorClock() {
        return vectorClock;
    }

}
