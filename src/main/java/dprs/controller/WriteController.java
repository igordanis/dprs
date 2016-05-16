package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.DynamoWriteResponse;
import dprs.response.WriteResponse;
//import dprs.wthrash.BackupService;
import dprs.service.Chord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class WriteController {
    private static final Logger logger = LoggerFactory.getLogger(WriteController.class);

    public static final String WRITE = "/write";
    public static final String DYNAMO_WRITE = "/dynamoWrite";

    @Autowired
    Chord chord;

//    BackupService backupService;

    @Value("${quorum.write}")
    int writeQuorum;


    @RequestMapping(WriteController.WRITE)
    public WriteResponse write(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "vectorClock", required = false) String vk
    ) {
        String transactionId = randomUUID().toString();
        VectorClock deserializedClock = VectorClock.fromJSON(vk);
        final VectorClock oldVectorClock;

        /*
         * Find part of chord which manages given key
         */

        List<NodeAddress> destinationAddresses = chord.findDestinationAdressesForKey(
                key, writeQuorum);


        if (deserializedClock == null || deserializedClock.getNumberOfComponents() == 0) {
            final VectorClock newVC = new VectorClock();
            destinationAddresses.forEach(destinationAddress -> newVC.setValueForComponent
                    (destinationAddress.getHash(), 0));
            oldVectorClock = newVC;
        } else {
            oldVectorClock = deserializedClock;
        }


        /*
         * Ziska write odpovede od vsetkych dynamo uzlov ktore maju zapisat objekt
         */

        Set<DynamoWriteResponse> allResponses = destinationAddresses.stream()
                .map(destinationAddress -> {

                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress.getIP() + ":" +
                                    destinationAddress.getPort())
                            .path(DYNAMO_WRITE)
                            .queryParam("key", key)
                            .queryParam("value", value)
                            .queryParam("transactionId", transactionId)
                            .queryParam("vectorClock", oldVectorClock.toJSON())
                            .build()
                            .toUri();

                    logger.info(transactionId, "Forwarding write request to: "
                            + destinationUri);

                    return new RestTemplate()
                            .getForObject(destinationUri, DynamoWriteResponse.class);

                })
                .collect(Collectors.toSet());

        VectorClock vl = new VectorClock();
        for(DynamoWriteResponse dynamoWriteResponse : allResponses){
            vl = VectorClock.mergeNewest(vl, VectorClock.fromJSON(dynamoWriteResponse
                    .getVectorClock()));
        }

        return new WriteResponse(vl, key);
    }


    @RequestMapping(WriteController.DYNAMO_WRITE)
    public DynamoWriteResponse dynamoRead(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "transactionId", required = true) String transactionId,
            @RequestParam(value = "vectorClock", required = true) String vc
    ) {
        logger.info(transactionId + ": Received request for concrete write: " + key);

        final VectorClock vectorClock = VectorClock.fromJSON(vc);

        final Integer selfIndexInChord = chord.getSelfIndexInChord();

        InMemoryDatabase.INSTANCE.computeIfPresent(key, (k, oldVal) -> {

            if (vectorClock.isThisEqualOrNewerThan(oldVal.getVectorClock(), selfIndexInChord)) {
                logger.info("New value has been updated");
                vectorClock.incrementValueForComponent(selfIndexInChord);
                return new DatabaseEntry(value, vectorClock);
            } else {
                logger.info("Old value has been kept");
                return oldVal;
            }
        });

        InMemoryDatabase.INSTANCE.computeIfAbsent(key, k  -> {
            final VectorClock vectorClock1 = VectorClock.fromJSON(vc);
            vectorClock1.incrementValueForComponent(selfIndexInChord);
            return new DatabaseEntry(value,vectorClock1);
        });


        return new DynamoWriteResponse(true, vectorClock.toJSON());
    }

    ;

}
