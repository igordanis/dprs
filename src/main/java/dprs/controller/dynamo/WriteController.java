package dprs.controller.dynamo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.service.DataManagerService;
//import dprs.wthrash.BackupService;
import dprs.service.ChordService;
import dprs.wthrash.TransportDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
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
    public static final String DYNAMO_SINGLE_WRITE = "/dynamoWrite";
    public static final String DYNAMO_BULK_WRITE = "/bulkWrite";

    @Autowired
    ChordService chordService;

    @Autowired
    DataManagerService dataManagerService;

    @Value("${quorum.write}")
    int writeQuorum;

    @Value("${quorum.replication}")
    int replicationQuorum;

    @RequestMapping(WriteController.WRITE)
    public DynamoWriteResponse write(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "vectorClock", required = false) String receivedVectorClock,
            @RequestParam(value = "transactionId", required = false) String transactionId
    ) {
        final String receivedTransactionId = transactionId != null ? transactionId : randomUUID().toString();

        /*
         * Find part of chord which manages given key
         */
        List<NodeAddress> destinationAddresses = chordService.findDestinationAddressesForKeyInChord(
                key, replicationQuorum);

        /*
         * If not coordinator for key, forward to it
         */
        if (!destinationAddresses.get(0).equals(chordService.getSelfAddressInChord())) {
            URI destinationUri = UriComponentsBuilder
                    .fromUriString("http://" + destinationAddresses.get(0).getFullAddress())
                    .path(WRITE)
                    .queryParam("key", key)
                    .queryParam("value", value)
                    .queryParam("transactionId", receivedTransactionId)
                    .queryParam("vectorClock", receivedVectorClock)
                    .build()
                    .toUri();
            logger.info(receivedTransactionId + ": Forwarding write request to " + destinationUri);
            return new RestTemplate().getForObject(destinationUri, DynamoWriteResponse.class);
        }

        // Counts successful writes. (Array because needs to be final)
        final int[] successfulUpdates = {0};

        /*
         * Ziska write odpovede od vsetkych dynamo uzlov ktore maju zapisat objekt
         */
        Set<VectorClock> allVectorClocks = destinationAddresses.stream()
                .map(destinationAddress -> {

                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress.getFullAddress())
                            .path(DYNAMO_SINGLE_WRITE)
                            .queryParam("key", key)
                            .queryParam("value", value)
                            .queryParam("transactionId", receivedTransactionId)
                            .queryParam("vectorClock", receivedVectorClock)
                            .build()
                            .toUri();

                    logger.info(receivedTransactionId +  ": Forwarding single write request to: " + destinationUri);

                    // If an exception is thrown while sending request to other node, it was unsuccessful
                    try {
                        DynamoWriteResponse response = new RestTemplate().getForObject(destinationUri, DynamoWriteResponse.class);
                        if (response.isUpdated()) {
                            successfulUpdates[0]++;
                        }

                        return VectorClock.fromJSON(response.getVectorClock());
                    } catch (Exception e) {
                        logger.error(receivedTransactionId + ": Failed to send single write request to " + destinationUri);
                        return null;
                    }
                })
                .collect(Collectors.toSet());

        /*
         * ked vrati viacej vector clockov, tak ich mergne do jedneho
         */
        VectorClock mergedVectorClock = VectorClock.mergeToNewer(allVectorClocks);
        logger.info(transactionId +  ": Merged vector clock: " + mergedVectorClock.toString() + " / "
                + mergedVectorClock.toJSON() + " from " +
                allVectorClocks);

        return new DynamoWriteResponse(successfulUpdates[0] >= writeQuorum, mergedVectorClock.toJSON());
    }


    @RequestMapping(WriteController.DYNAMO_SINGLE_WRITE)
    public DynamoWriteResponse dynamoSingleWrite(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "transactionId", required = true) String transactionId,
            @RequestParam(value = "vectorClock", required = true) String vc
    ) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        logger.info(transactionId + ": Received request for concrete write: " + key);
        // Checks if update is successful. (Array because it needs to be final)
        final boolean[] updated = {true};
        final VectorClock vectorClock = VectorClock.fromJSON(vc);

        final Integer selfIndexInChord = chordService.getSelfIndexInChord();

        database.computeIfPresent(key, (k, oldVal) -> {

            if (vectorClock.isThisNewerThan(oldVal.getVectorClock())) {
                logger.info("New value has been updated");
                vectorClock.incrementValueForComponent(selfIndexInChord);
                return new DatabaseEntry(value, vectorClock);
            } else {
                logger.info("Old value has been kept");
                updated[0] = false;
                return oldVal;
            }
        });

        database.computeIfAbsent(key, k -> {
            final VectorClock newVectorClock = VectorClock.fromJSON(vc);
            newVectorClock.incrementValueForComponent(selfIndexInChord);
            return new DatabaseEntry(value, newVectorClock);
        });

        return new DynamoWriteResponse(updated[0], database.get(key).getVectorClock().toJSON());
    }

    @RequestMapping(DYNAMO_BULK_WRITE)
    public TransportDataResponse bulkWrite(@RequestParam(value = "data") String data) {

        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        HashMap<String, DatabaseEntry> dataMap = new Gson()
                .fromJson(data, new TypeToken<HashMap<String, DatabaseEntry>>() {
                }.getType());

        logger.info("Received data: " + data);
        database.putAll(dataMap);
        return new TransportDataResponse(true);
    }

}
