package fiit.dprs.team4;


import dprs.entity.DatabaseEntry;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.response.dynamo.ReadResponse;
import fiit.dprs.team4.utils.Loggable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by igordanis on 19/05/16.
 */
@Service
public class DynamoClient implements Loggable{

    HashMap<String, DatabaseEntry> existingValues = new HashMap<>();

    private String dynamoEndpoint = "http:// ";

    private void writeNewValue(){
        final String key = UUID.randomUUID().toString().replaceAll("-", "");
        final String value = UUID.randomUUID().toString().replaceAll("-", "");

        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://192.168.1.x:xxxx/write")  // adresa loadbalancera
                .queryParam("key", key)
                .queryParam("value", value)
                .build()
                .toUri();

        DynamoWriteResponse writeResponse = new RestTemplate()
                .getForObject(destinationUri, DynamoWriteResponse.class);

        Assert.isTrue(writeResponse.isUpdated());
    }

    private void readExisting(){
        Integer randomIndex = ThreadLocalRandom.current().nextInt(existingValues.size());

        final Map.Entry<String, DatabaseEntry> randomEntry =  existingValues.entrySet().stream()
                .collect(Collectors.toList()).get(randomIndex);

        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://192.168.1.x:xxxx/write")  // adresa loadbalancera
                .queryParam("key", randomEntry.getKey())
                .queryParam("value", randomEntry.getValue().getVectorClock())
                .build()
                .toUri();

        ReadResponse readResponse = new RestTemplate()
                .getForObject(destinationUri, ReadResponse.class);

        //Existing read must return written value
        Assert.isTrue(readResponse.getUniqValues().contains(randomEntry.getValue().getValue()));
        //Existing read must return newer vectorClock that it was previously written
        Assert.isTrue(VectorClock.fromJSON(readResponse.getNextVectorClockToken())
                .isThisNewerThan(randomEntry.getValue().getVectorClock()));

    }

    private void updateExisting(){

    }



    private void updateExistingConflict(){

    }

    @Scheduled(fixedDelay = 300)
    public void scheduledRequest() {

        if(existingValues.isEmpty()){
            writeNewValue();
        }
    }

}
