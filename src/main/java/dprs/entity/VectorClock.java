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
public class VectorClock {


    private static final Logger logger = LoggerFactory.getLogger(VectorClock.class);

    private Map<Integer, Integer> vectorClock = new HashMap();

    public VectorClock() {
    }

    public VectorClock(Map<Integer, Integer> vectorClock) {
        this.vectorClock = vectorClock;
    }

    public Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(Map<Integer, Integer> vectorClock) {
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
    private static int FIRST_VC_IS_NEWER = 1;
    private static int SECOND_VC_IS_NEWER = -1;
    private static int VC_ARE_SAME = 0;
    private static int VC_ARE_CONCURENT = 3;
    private static int COMPARISON_UNDEFINED = 4;

    private int compareTwoVectorClocks(VectorClock vc1, VectorClock vc2) {

        if (!vectorKeysHaveCommonComponents(vc1, vc2)) {
            logger.warn("Vector clocks needs to have at least one common component to decide if they are " +
                    "in fact concurent for view of its component");
            return COMPARISON_UNDEFINED;
        }

        final Set<Integer> uniqVectorClockKeys = new HashSet<>();

        uniqVectorClockKeys.addAll(vc1.vectorClock.keySet());
        uniqVectorClockKeys.addAll(vc2.vectorClock.keySet());

        VectorClock newerVectorClock = null;
        boolean areConcurent = false;

        for (final Integer uniqVectorclockKey : uniqVectorClockKeys) {
            Integer value1 = vc1.vectorClock.getOrDefault(uniqVectorclockKey, 0);
            Integer value2 = vc2.vectorClock.getOrDefault(uniqVectorclockKey, 0);

            //hodnota vsetkych komponentov jedneho vector clocku musi byt voci vsetkym
            // komponentom druheho vector clocku neklesajuca - t.j. monotonne stupajuca.
            switch (value1.compareTo(value2)) {
                case -1:
                    // Druhy je novsi. Ak bol pocas iteracie niekedy novsi komponent druheho
                    // vectorclocku vznika konflikt
                    if (newerVectorClock == vc1)
                        areConcurent = true;
                    else {
                        newerVectorClock = vc2;
//                        areConcurent = false;
                    }
                    break;
                case 1:
                    // Prvy je novsi. Ak bol pocas iteracie niekedy novsi komponent druheho
                    // vectorclocku vznika konflikt
                    if (newerVectorClock == vc2)
                        areConcurent = true;
                    else {
                        newerVectorClock = vc1;
//                        areConcurent = false;
                    }
                    break;
                case 0:
                    //hodnoty komponentov su rovnake -> na zaklade tejto informacie nevieme povedat
                    //ktory vc je novsi
                    break;
            }
        }

        if (areConcurent)
            return VC_ARE_CONCURENT;
        else {
            if (newerVectorClock == null)
                return VC_ARE_SAME;
            if (newerVectorClock == vc1)
                return FIRST_VC_IS_NEWER;
            if (newerVectorClock == vc2)
                return SECOND_VC_IS_NEWER;
        }
        //sem sa nikdy nedostaneme
        return -99999;
    }

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

        //ak neexistuje, je to ako keby bola predtym nula a preto ju nastavi na 1
        vectorClock.computeIfAbsent(component, c -> 1);
    }

    public void decrementValueForComponent(final Integer component) {
        // ak existuje hodnota, znizi ju o 1
        vectorClock.computeIfPresent(component, (key, oldVal) -> oldVal - 1);
        // ak neexistuje ignorujeme
    }


    /*
     *
     */
    public boolean isThisNewerThan(final VectorClock other) {
        int comparisionResult = compareTwoVectorClocks(this, other);
        return comparisionResult == FIRST_VC_IS_NEWER;
    }

    public boolean isThisConcurentTo(VectorClock other) {
        final int comparisonResult = compareTwoVectorClocks(this, other);
        return comparisonResult == VC_ARE_CONCURENT;
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


    /*
     * Pre ucely unittestov
     */
//    private boolean equalz(VectorClock that) {
//
//        if (that == null )
//            return false;
//
//
//        final Set<Integer> uniqVectorClockKeys = new HashSet<>();
//
//        uniqVectorClockKeys.addAll(this.vectorClock.keySet());
//        uniqVectorClockKeys.addAll(that.vectorClock.keySet());
//
//        for(Integer uniqKey : uniqVectorClockKeys){
//            Integer thisValue = this.vectorClock.get(uniqKey);
//            Integer thatValue = this.vectorClock.get(uniqKey);
//
//            //staci ze najdeme jeden rozdiel -> nie su rovnake
//            if(thisValue.compareTo(thatValue) != 0)
//                return false;
//        }
//
//        return true;
//    }

    public void addDisjunctiveValuesFrom(VectorClock vc) {
        Set<Integer> keySet = new HashSet<>(vc.vectorClock.keySet());
        keySet.removeAll(vectorClock.keySet());

        keySet.stream().forEach(key -> vectorClock.put(key, vc.vectorClock.get(key)));
    }

    public boolean containsValueForComponent(Integer component) {
        return vectorClock.containsKey(component);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        VectorClock that = (VectorClock) o;

        if (vectorClock != null) {
            final int comparisonResult = compareTwoVectorClocks(this, that);
            return comparisonResult == VC_ARE_SAME;
        } else
            return that.vectorClock == null;
    }

    public VectorClock clone(){
        VectorClock clonedVectorClock = new VectorClock();

        this.vectorClock.entrySet().forEach(integerIntegerEntry -> {
            Integer newKey = new Integer(integerIntegerEntry.getKey().intValue());
            Integer newVal = new Integer(integerIntegerEntry.getValue().intValue());

            clonedVectorClock.vectorClock.put(newKey, newVal);
        });

        return clonedVectorClock;
    }

}
