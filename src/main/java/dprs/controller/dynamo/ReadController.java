package dprs.controller.dynamo;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoReadResponse;
import dprs.response.dynamo.ReadResponse;
import dprs.response.util.ReadAllFromSelfResponse;
import dprs.util.Tuple;
import dprs.service.ChordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ = "/read";
    public static final String DYNAMO_READ = "/dynamoSingleWrite";


    @Autowired
    ChordService chordService;

    @RequestMapping(ReadController.READ)
    public ReadResponse read(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum
    ) {
        String transactionId = randomUUID().toString();

        /*
         * Find part of chord which manages given key
         */
        List<NodeAddress> destinationAddresses = chordService.findDestinationAddressesForKeyInChord(key, readQuorum);

        /*
         * Ziska read odpovede od vsetkych dynamo uzlov ktore maju obsahovat dany objekt
         */
        Set<DynamoReadResponse> allResponses = destinationAddresses.stream()
                .map(destinationAddress -> {

                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress.getIP() + ":" +
                                    destinationAddress.getPort())
                            .path(DYNAMO_READ)
                            .queryParam("key", key)
                            .queryParam("transactionId", transactionId)
                            .build()
                            .toUri();

                    logger.info(transactionId + ": Forwarding read request to: " + destinationUri);

                    try {
                        return new RestTemplate().getForObject(destinationUri, DynamoReadResponse.class);
                    } catch (Exception e) {
                        logger.error(transactionId + ": Failed to retrieve read response from " + destinationUri);
                        return null;
                    }
                })
                .collect(Collectors.toSet());


        /*
         * After all responses has been collected, constructs one final response
         * for client
         */

        Set<VectorClock> allVectorClocks = allResponses.stream()
                .map(dynamoReadResponse -> VectorClock.fromJSON(dynamoReadResponse.getVectorClock()))
                .collect(Collectors.toSet());
        String nextVectorClockToken = VectorClock.mergeToNewer(allVectorClocks).toJSON();


        Set<String> uniqValues = allResponses.stream()
                .map(dynamoReadResponse -> dynamoReadResponse.getValue())
                .collect(Collectors.toSet());

        long countSuccessfulResponses = allResponses.stream().filter(response -> response.isSuccessful()).count();
        boolean successful = countSuccessfulResponses >= readQuorum || countSuccessfulResponses >= chordService.getChordCount();

        return new ReadResponse(nextVectorClockToken, uniqValues, successful);

    }


    @RequestMapping(ReadController.DYNAMO_READ)
    public DynamoReadResponse dynamoRead(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "transactionId", required = true) String transactionId
    ) {
        logger.info(transactionId + ": Received request for concrete write: " + key);

        final DatabaseEntry databaseEntry = InMemoryDatabase.INSTANCE.get(key);
        final DynamoReadResponse dynamoReadResponse = new DynamoReadResponse();

        if(databaseEntry == null){
            logger.info(transactionId + ": Database doesnt contain any value for requested key: " + key);
            dynamoReadResponse.setSuccessful(false);
        } else {
            dynamoReadResponse.setKey(key);
            dynamoReadResponse.setValue(databaseEntry.getValue());
            dynamoReadResponse.setVectorClock(databaseEntry.getVectorClock().toJSON());
            dynamoReadResponse.setSuccessful(true);
        }

        return dynamoReadResponse;
    }

}
