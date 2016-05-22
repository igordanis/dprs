package fiit.dprs.team4.chaos;


import dprs.entity.DatabaseEntry;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.response.dynamo.ReadResponse;
import fiit.dprs.team4.chaos.utils.Loggable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static dprs.entity.VectorClock.fromJSON;

@Component
public class DynamoClient implements Loggable{

    HashMap<String, DatabaseEntry> existingValues = new HashMap<>();

    @Value("${loadbalancer.ip}")
    private String dynamoEndpoint;



    private Entry<String, DatabaseEntry> getRandomExistingEntry(){
        Integer randomIndex = ThreadLocalRandom.current().nextInt(existingValues.size());

        final Entry<String, DatabaseEntry> randomEntry =  existingValues.entrySet().stream()
                .collect(Collectors.toList()).get(randomIndex);

        return randomEntry;
    }

    private void readRandomExisting(){

        logger().info("Reading existing random value");

        final Entry<String, DatabaseEntry> randomEntry =  getRandomExistingEntry();

        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/read")  // adresa loadbalancera
                .queryParam("key", randomEntry.getKey())
                .queryParam("readQuorum", 3)
//                .queryParam("vectorClock", randomEntry.getValue().getVectorClock().toJSON())
                .build()
                .toUri();

        ReadResponse readResponse = new RestTemplate()
                .getForObject(destinationUri, ReadResponse.class);

        //Existing read must return written value
        Assert.isTrue(readResponse.getUniqValues().contains(randomEntry.getValue().getValue()));
        //Existing read must return same vectorClock that it was previously returned by write,
        //or in case of replication returned vector clock must be newer
        Assert.isTrue(
                fromJSON(readResponse.getNextVectorClockToken())
                        .equals(randomEntry.getValue().getVectorClock())
                        || fromJSON(readResponse.getNextVectorClockToken())
                        .isThisNewerThan(randomEntry.getValue().getVectorClock())
        );

    }



    private void writeNewValue(){

        logger().info("Writing new random value");

        final String key = UUID.randomUUID().toString().replaceAll("-", "");
        final String value = UUID.randomUUID().toString().replaceAll("-", "");

        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/write")  // adresa loadbalancera
                .queryParam("key", key)
                .queryParam("value", value)
                .build()
                .toUri();

        DynamoWriteResponse writeResponse = new RestTemplate()
                .getForObject(destinationUri, DynamoWriteResponse.class);

        Assert.isTrue(writeResponse.isSuccessful());

        existingValues.put(key,
                new DatabaseEntry(value, fromJSON(writeResponse.getVectorClock())));
    }




    private void updateExisting(){
        logger().info("Updating existing random value");

        final Entry<String, DatabaseEntry> randomExistingEntry = getRandomExistingEntry();
        final String newValue = UUID.randomUUID().toString().replaceAll("-", "");

        /*
         * Overwrites existing value
         */
        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/write")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("value", newValue)
                .queryParam("vectorClock", randomExistingEntry.getValue().getVectorClock().toJSON())
                .build()
                .toUri();

        DynamoWriteResponse writeResponse = new RestTemplate()
                .getForObject(destinationUri, DynamoWriteResponse.class);

        Assert.isTrue(writeResponse.isSuccessful());

        /*
         * Reads overwritten value
         */

        URI destinationReadUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/read")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("readQuorum", 3)
//                .queryParam("vectorClock", randomEntry.getValue().getVectorClock().toJSON())
                .build()
                .toUri();

        ReadResponse readResponse = new RestTemplate()
                .getForObject(destinationReadUri, ReadResponse.class);


        Assert.isTrue(fromJSON(readResponse.getNextVectorClockToken()).isThisNewerThan(
                        randomExistingEntry.getValue().getVectorClock())
        );

        /*
         * Ocakavam ze novy read uz neobsahuje staru hodnotu
         */
        Assert.isTrue(!readResponse.getUniqValues()
                .contains(randomExistingEntry.getValue().getValue()));

        /*
         * Ocakavam ze novy read obsahuje uz novu hodnotu
         */
        Assert.isTrue(readResponse.getUniqValues().contains(newValue));
        Assert.isTrue(readResponse.getUniqValues().size() == 1);

        randomExistingEntry.setValue(new DatabaseEntry(newValue,
                fromJSON(readResponse.getNextVectorClockToken())));

    }



    private void updateExistingConflict(){

        logger().info("Updating existing random value with conflict");

        final Entry<String, DatabaseEntry> randomExistingEntry = getRandomExistingEntry();
        final String newValue = UUID.randomUUID().toString().replaceAll("-", "");

        /*
         * Overwrites existing value
         */
        VectorClock conflictingVectorClock = randomExistingEntry.getValue().getVectorClock().clone();
        Integer randomKey = conflictingVectorClock.getVectorClock().keySet().stream()
                .collect(Collectors.toList())
                .get(ThreadLocalRandom.current().nextInt(conflictingVectorClock.getVectorClock()
                        .keySet().size()));
        conflictingVectorClock.decrementValueForComponent(randomKey);
        conflictingVectorClock.decrementValueForComponent(randomKey);

        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/write")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("value", newValue)
                .queryParam("vectorClock", conflictingVectorClock.toJSON())
                .build()
                .toUri();

        DynamoWriteResponse writeResponse = new RestTemplate()
                .getForObject(destinationUri, DynamoWriteResponse.class);

        //bol updatnute aspon 1 node
        Assert.isTrue(writeResponse.isSuccessful() == true);



        /*
         * Reads overwritten value
         */
        URI destinationReadUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/read")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("readQuorum", 3)
                .build()
                .toUri();

        ReadResponse conflictingReadResponse = new RestTemplate()
                .getForObject(destinationReadUri, ReadResponse.class);


        Assert.isTrue(fromJSON(conflictingReadResponse.getNextVectorClockToken()).isThisNewerThan(
                randomExistingEntry.getValue().getVectorClock())
        );


        /*
         * Ocakavam ze novy read obsahuje aj staru hodnotu
         */
        Assert.isTrue(conflictingReadResponse.getUniqValues()
                .contains(randomExistingEntry.getValue().getValue()));
        /*
         * Ocakavam ze novy read obsahuje aj novu hodnotu
         */
        Assert.isTrue(conflictingReadResponse.getUniqValues().contains(newValue));
        Assert.isTrue(conflictingReadResponse.getUniqValues().size() > 1);


        //zmergujem read konflikt

        String mergedValue = newValue;
        URI destinationUri2 = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/write")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("value", mergedValue)
                .queryParam("vectorClock", conflictingReadResponse.getNextVectorClockToken())
                .build()
                .toUri();

        DynamoWriteResponse mergeResponse = new RestTemplate()
                .getForObject(destinationUri2, DynamoWriteResponse.class);

        Assert.isTrue(mergeResponse.isSuccessful());


        URI destinationMergedReadUri = UriComponentsBuilder
                .fromUriString("http://" + dynamoEndpoint + "/read")  // adresa loadbalancera
                .queryParam("key", randomExistingEntry.getKey())
                .queryParam("readQuorum", 3)
                .build()
                .toUri();

        ReadResponse mergedReadResponse = new RestTemplate()
                .getForObject(destinationMergedReadUri, ReadResponse.class);

        Assert.isTrue(mergedReadResponse.getUniqValues().contains(mergedValue));
        Assert.isTrue(mergedReadResponse.getUniqValues().size() == 1);

        randomExistingEntry.setValue(
                new DatabaseEntry(mergedValue, fromJSON(mergedReadResponse
                        .getNextVectorClockToken())));

    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledRequest() {

        if(existingValues.isEmpty()){

//            clearDb();

            for(int i = 0; i<=0; i++) {
                writeNewValue();
            }
        }

        Integer randomAction = ThreadLocalRandom.current().nextInt(7);
        switch (randomAction){
            case 0:
                writeNewValue(); break;

            case 1:
            case 2:
            case 3:
                readRandomExisting(); break;


            case 4:
            case 5:
            case 6:
                updateExisting(); break;

            case 7:
            case 8:
            case 9: updateExistingConflict(); break;
        }
    }

}
