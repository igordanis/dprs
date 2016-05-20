package dprs.entity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Integer.max;


/*
* Neextenduje HashMap aby nebolo mozne priamo pouzivat HashMap metody
*/
public class VectorClock implements Comparable<VectorClock> {


    private static final Logger logger = LoggerFactory.getLogger(VectorClock.class);

    private Map<Integer, Integer> vectorClock = new HashMap();

    public VectorClock() {}

    public VectorClock(Map<Integer, Integer> vectorClock) {
        this.vectorClock = vectorClock;
    }

    /*
       * Zisti, ci su konkurentne - alebo ci je niektory z nich novsi
       * vracia:
       *    a\ 3    ak su konkurentne
       *    b\ 0    ak su rovnake
       *    c\ 1    ak je prvy novsi
       *    d\ -1   ak je druhy novsi
       */
    private static int COMPARISON_RESULT_NEWER = 1;
    private static int COMPARISON_RESULT_OLDER = -1;
    private static int COMPARISON_RESULT_EQUAL = 0;
    private static int COMPARISON_RESULT_UNDEFINED = 3;

    private boolean vectorKeysHaveCommonComponents(VectorClock vc1, VectorClock vc2) {
        final Set<Integer> uniqVectorClockKeys = new HashSet<>();

        uniqVectorClockKeys.addAll(vc1.vectorClock.keySet());
        uniqVectorClockKeys.addAll(vc2.vectorClock.keySet());

        //zistime ci maju vectorclocky spolocne komponenty

        final long countOfCommonKeys = uniqVectorClockKeys.stream()
                //namapujeme na to ci je spolocny
                .map(uniqVectorClockKey -> {
                    return vc1.vectorClock.containsKey(uniqVectorClockKey)
                            && vc2.vectorClock.containsKey(uniqVectorClockKey);
                })
                //vyberieme iba spolocne
                .filter(r -> r == true)
                //a spocitame
                .count();
        return countOfCommonKeys > 0;
    }

    public void incrementValueForComponent(final Integer component) {
        // ak existuje hodnota, zvysi ju o 1
        vectorClock.computeIfPresent(component, (key, oldVal) -> oldVal + 1);

        // ak neexistuje, je to ako keby bola predtym nula a preto ju nastavi na 1
        vectorClock.computeIfAbsent(component, c -> 1);
    }

    public boolean isNewerThan(VectorClock vc) {
        return compareTo(vc) == COMPARISON_RESULT_NEWER;
    }

    public boolean isConcurentTo(VectorClock vc) {
        return compareTo(vc) == COMPARISON_RESULT_UNDEFINED;
    }

    /*
     * VZDY Nastavi na danu hodnotu pre komponent vector clocku
     */
    public void setValueForComponent(final Integer component, final Integer newValue) {
        vectorClock.put(component, newValue);
    }

    /*
     * Funkcia spoji kolekciu vectorClockov do jedneho - novsieho ako vsetky, ktore boli
     * do funkcie poslane. Pri spajani nezalezi na tom ci su konkurentne
     */
    public static VectorClock mergeToNewer(Set<VectorClock> setOfVectorClocks) {
        final VectorClock resultingVectorClock = new VectorClock();
        final Set<Integer> uniqueVectorClockKeys = new HashSet<>();

        /*
         * Vytvorim mnozinu unikatnych klucov ktore su vo vector clockoch pouzite
         */
        setOfVectorClocks.forEach(vectorClockX ->
                uniqueVectorClockKeys.addAll(vectorClockX.vectorClock.keySet())
        );

        /*
         * Najde maximum kazdej hodnoty vectorClocku, ktora bola pouzita pre dany kluc
         */
        for (Integer vectorClockKey : uniqueVectorClockKeys) {
            Integer maxComponentForKey = 0;

            for (VectorClock vc : setOfVectorClocks) {
                final Integer currentComponentValue = vc.vectorClock.getOrDefault(vectorClockKey, 0);
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
        Type typeOfHashMap = new TypeToken<Map<Integer, Integer>>() {
        }.getType();
        return new Gson().toJson(vectorClock, typeOfHashMap);
    }

    public static VectorClock fromJSON(String json) {
        if (json == null || "".equals(json)) {
            logger.debug("Vector clock doesnt contain any deserializable value. Creating empty vector clock");
            return new VectorClock();
        }

        logger.debug("Attempting to deserialize vector clock from:  " + json);

        /*
         * GSON z nejakeho dovodu pridava do serializovanych stringov lomitka.
         * Preto su odstranovane
        */
        String formattedJson = json.replace("\\", "");

        Type typeOfHashMap = new TypeToken<Map<String, Integer>>() {
        }.getType();
        Map<String, Integer> deserializedMap = new Gson().fromJson(formattedJson, typeOfHashMap);

        Map<Integer, Integer> resultMap = new HashMap<>();
        deserializedMap.keySet().forEach(key -> resultMap.put(Integer.valueOf(key), deserializedMap.get(key)));

        return new VectorClock(resultMap);
    }

    public void addDisjunctiveValuesFrom(VectorClock vc) {
        Set<Integer> keySet = new HashSet<>(vc.vectorClock.keySet());
        keySet.removeAll(vectorClock.keySet());

        keySet.stream().forEach(key -> vectorClock.put(key, vc.vectorClock.get(key)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass() || vectorClock == null)
            return false;

        VectorClock vc = (VectorClock) o;

        if (!vectorClock.keySet().equals(vc.vectorClock.keySet()))
            return false;
        else if (vectorClock != null)
            return compareTo(vc) == 0;
        else
            return vc.vectorClock == null;
    }

    @Override
    public String toString() {
        return "VectorClock{" + vectorClock + '}';
    }

    @Override
    public int compareTo(VectorClock vc) {
        /*
            Newer if all components are greater than the second one.
            Older if at least one component is lesser than second one.
            Undefined if there are both newer and older components.
         */
        Set<Integer> keySet = new HashSet<>(vectorClock.keySet());
        keySet.addAll(vc.vectorClock.keySet());

        boolean isLesser = false;
        boolean isGreater = false;

        for (Integer key : keySet) {
            Integer myValue = vectorClock.getOrDefault(key, 0);
            Integer otherValue = vc.vectorClock.getOrDefault(key, 0);

            if (myValue < otherValue) {
                isLesser = true;
            } else if (myValue > otherValue) {
                isGreater = true;
            }
        }

        if (isLesser && isGreater)
            return VectorClock.COMPARISON_RESULT_UNDEFINED;
        if (isLesser)
            return VectorClock.COMPARISON_RESULT_OLDER;
        if (isGreater)
            return VectorClock.COMPARISON_RESULT_NEWER;

        return VectorClock.COMPARISON_RESULT_EQUAL;
    }
}
