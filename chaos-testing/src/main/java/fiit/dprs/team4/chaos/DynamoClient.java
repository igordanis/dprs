package fiit.dprs.team4.chaos;


import dprs.entity.DatabaseEntry;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.response.dynamo.ReadResponse;
import fiit.dprs.team4.chaos.utils.Loggable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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

        Assert.isTrue(writeResponse.isUpdated());

        existingValues.put(key,
                new DatabaseEntry(value, fromJSON(writeResponse.getVectorClock())));
    }

    private void readExisting(){

        logger().info("Reading existing random value");

        Integer randomIndex = ThreadLocalRandom.current().nextInt(existingValues.size());

        final Entry<String, DatabaseEntry> randomEntry =  existingValues.entrySet().stream()
                .collect(Collectors.toList()).get(randomIndex);

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
        //Existing read must return same vectorClock that it was previously returned by write
        Assert.isTrue(fromJSON(readResponse.getNextVectorClockToken())
                .equals(randomEntry.getValue().getVectorClock()));

    }

    private void updateExisting(){

    }



    private void updateExistingConflict(){

    }

    @Scheduled(fixedDelay = 100)
    public void scheduledRequest() {

        if(existingValues.isEmpty()){
            writeNewValue();
        }

        Integer randomAction = ThreadLocalRandom.current().nextInt(4);
        switch (randomAction){
            case 0: writeNewValue(); break;

            case 1:
            case 2:
            case 3: readExisting(); break;
        }
    }

}
