package dprs.entity;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;


public class VectorClock {
    private Map<Integer, Integer> vectorClock = new HashMap();

    public void incrementValueForComponent(final Integer component) {
        vectorClock.computeIfPresent(component, (key, oldValue) -> oldValue++);
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

    private void setValueForComponent(final Integer component, final Integer newValue) {
        vectorClock.computeIfPresent(component, (key, oldValue) -> newValue);
        vectorClock.computeIfAbsent(component, key -> newValue);
    }


    public String toJSON() {
        return new Gson().toJson(this);
    }

    public static VectorClock fromJSON(String json) {
        return new Gson().fromJson(json, VectorClock.class);
    }

}
