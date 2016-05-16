package dprs.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.lang.Integer.max;


public class VectorClock implements Serializable{
    private Map<Integer, Integer> vectorClock = new HashMap();

    private static final Logger logger = LoggerFactory.getLogger(VectorClock.class);

    public void incrementValueForComponent(final Integer component) {
//        vectorClock.computeIfPresent(component, (key, oldValue) -> ++oldValue);
        if(vectorClock.containsKey(component)){
            vectorClock.put(component, vectorClock.get(component) + 1);
            System.out.println("");
        }
    }

    public boolean isThisEqualOrNewerThan(final VectorClock other, final Integer byComponent) {
        return compareToByComponent(other, byComponent) == 1
                || compareToByComponent(other, byComponent) == 0;
    }

    public boolean isThisNewerThan(final VectorClock other, final Integer byComponent) {
        return compareToByComponent(other, byComponent) == 1;
    }
    /*
    * Vrati
    *   0 ak su rovnake
    *   -1 ak je this starsi
    *   1 ak je this novsi
    */
    private int compareToByComponent(final VectorClock other, final Integer component) {
        final Integer otherValue = other.vectorClock.get(component);
        final Integer thisValue = this.vectorClock.get(component);

        if (thisValue == null && otherValue == null)
            return 0;
        if (thisValue != null && otherValue == null)
            return 1;
        if (thisValue == null && otherValue != null)
            return -1;
        //if(thisValue != null && otherValue != null)
        return thisValue.compareTo(otherValue);
    }

    public void setValueForComponent(final Integer component, final Integer newValue) {
        vectorClock.computeIfPresent(component, (key, oldValue) -> newValue);
        vectorClock.computeIfAbsent(component, key -> newValue);
    }


    public String toJSON() {
        HashMap<String, String> vc = new HashMap<>();
        vectorClock.entrySet().forEach(e -> vc.put(e.getKey().toString(),e.getValue().toString()));
        return new Gson().toJson(vc);
    }

    public static VectorClock fromJSON(String json) {
        if (json == null) {
            return new VectorClock();
        } else {
            logger.debug("Attempting to deserialize vector clock from:  " + json);
            final String replace = json.replace("\\", "");
            Type typeOfHashMap = new TypeToken<Map<String, String>>(){}.getType();
//            Gson gson = new GsonBuilder().create();
            Map<String, String> deserializedMap = new Gson().fromJson(replace, typeOfHashMap);
            Map<Integer, Integer> newMap = new HashMap<>();
            deserializedMap.entrySet().forEach(e -> newMap.put(Integer.valueOf(e.getKey()),
                    Integer.valueOf(e.getValue())));

            final VectorClock vectorClock = new VectorClock();
            vectorClock.vectorClock = newMap;
            return vectorClock;
        }
    }

    public static VectorClock mergeNewest(VectorClock vc1, VectorClock vc2){

       VectorClock newVC = new VectorClock();

        Set<Integer> uniqKeys = new HashSet<>();

        uniqKeys.addAll(vc1.vectorClock.keySet());
        uniqKeys.addAll(vc2.vectorClock.keySet());

        for(Integer i : uniqKeys){
            if(vc1.vectorClock.containsKey(i) && vc2.vectorClock.containsKey(i)){
                newVC.setValueForComponent(i, max(vc1.vectorClock.get(i), vc2.vectorClock.get(i)));
                continue;
            }

            if(!vc2.vectorClock.containsKey(i)){
                newVC.setValueForComponent(i, vc1.vectorClock.get(i));
            }

            if(!vc1.vectorClock.containsKey(i)){
                newVC.setValueForComponent(i, vc2.vectorClock.get(i));
            }
        }

        return newVC;
    }

    public int getNumberOfComponents(){
        return this.vectorClock.size();
    };


}
