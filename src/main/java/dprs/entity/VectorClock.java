package dprs.entity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.max;

/*
* Neextenduje HashMap aby nebolo mozne priamo pouzivat HashMap metody
*/
public class VectorClock{


    private static final Logger logger = LoggerFactory.getLogger(VectorClock.class);

    private Map<Integer, Integer> vectorClock = new HashMap();


    public void incrementValueForComponent(final Integer component) {

        // ak existuje hodnota, zvysi ju o 1
        vectorClock.computeIfPresent(component, (key, oldVal) -> oldVal+1);

        //ak neexistuje, ako keby bola predtym nula a preto ju nastavi na 1
        vectorClock.computeIfAbsent(component, c -> 1);

    }


    public boolean isThisNewerThan(final VectorClock other, final Integer byComponent) {
        return compareToByComponent(other, byComponent) == 1;
    }


    /*
    *  Funkcia porovna dva vector clocky.
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

    /*
     * VZDY Nastavi na danu hodnotu pre komponent vector clocku
     */
    public void setValueForComponent(final Integer component, final Integer newValue) {
        vectorClock.put(component, newValue);
    }


    /*
     * Funkcia spoji kolekciu vectorClockov do jedneho - novsieho ako vsetky, ktore boli
     * do funkcie poslane
     */
    public static VectorClock mergeToNewer(Set<VectorClock> setOfVectorClocks) {

        final VectorClock resultingVectorClock = new VectorClock();

        final Set<Integer> uniqVectorClockKeys = new HashSet<>();

        /*
         * Vytvorim mnozinu unikatnych klucov ktore su vo vector clockoch pouzite
         */
        setOfVectorClocks.forEach(vectorClockX -> {
            uniqVectorClockKeys.addAll(vectorClockX.vectorClock.keySet());
        });

        /*
         * Najde maximum kazdej hodnoty vectorClocku, ktora bola pouzita pre dany kluc
         */
        for (Integer vectorClockKey : uniqVectorClockKeys) {

            Integer maxComponentForKey = 0;

            for (VectorClock vc : setOfVectorClocks) {

                final Integer defaultValue = 0;

                final Integer currentComponentValue =
                        vc.vectorClock.getOrDefault(vectorClockKey, defaultValue);

                maxComponentForKey = max(currentComponentValue, maxComponentForKey);
            }

            maxComponentForKey += 1;

            resultingVectorClock.setValueForComponent(vectorClockKey, maxComponentForKey);
        }


        return resultingVectorClock;
    }


    /*
     * Serializacne metody
     */
    public String toJSON() {

        final HashMap<String, String> serializableMap = new HashMap<>();

        vectorClock.entrySet().forEach(e -> {
            final String key = e.getKey().toString();
            final String value = e.getValue().toString();
            serializableMap.put(key, value);
        });
        return new Gson().toJson(serializableMap);
    }

    public static VectorClock fromJSON(String json) {

        if (json == null || "".equals(json)) {

            logger.debug("Vector clock doesnt contain any deserializable value. Creating empty " +
                    "vector clock");

            return new VectorClock();

        } else {

            logger.debug("Attempting to deserialize vector clock from:  " + json);

            /*
             * GSON z nejakeho dovodu pridava do serializovanych stringov lomitka.
             * Preto su odstranovane
             */
            final String replace = json.replace("\\", "");

            Type typeOfHashMap = new TypeToken<Map<String, String>>() {
            }.getType();

            Map<String, String> deserializedMap = new Gson().fromJson(replace, typeOfHashMap);
            Map<Integer, Integer> newMap = new HashMap<>();

            deserializedMap.entrySet().forEach(e -> {
                Integer integerKey = Integer.valueOf(e.getKey());
                Integer integerValue = Integer.valueOf(e.getValue());
                newMap.put(integerKey, integerValue);
            });

            final VectorClock vectorClock = new VectorClock();
            vectorClock.vectorClock = newMap;
            return vectorClock;
        }
    }



    @Override
    public boolean equals(Object o) {

        if (o == null || this.getClass() != o.getClass()) return false;

        VectorClock that = (VectorClock) o;

        final Set<Integer> uniqVectorClockKeys = new HashSet<>();

        uniqVectorClockKeys.addAll(this.vectorClock.keySet());
        uniqVectorClockKeys.addAll(that.vectorClock.keySet());

        for(Integer uniqKey : uniqVectorClockKeys){
            Integer thisValue = this.vectorClock.get(uniqKey);
            Integer thatValue = this.vectorClock.get(uniqKey);

            //staci ze najdeme jeden rozdiel -> nie su rovnake
            if(thisValue.compareTo(thatValue) != 0)
                return false;
        }

        return true;
    }

}
